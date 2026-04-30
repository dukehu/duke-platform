package com.duke.auth.service.impl;

import cn.hutool.captcha.GifCaptcha;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duke.auth.config.properties.SmsProperties;
import com.duke.auth.dto.*;
import com.duke.auth.entity.SysButton;
import com.duke.auth.entity.SysMenu;
import com.duke.auth.entity.SysUser;
import com.duke.auth.mapper.SysButtonMapper;
import com.duke.auth.mapper.SysMenuMapper;
import com.duke.auth.mapper.SysUserMapper;
import com.duke.auth.security.JwtTokenProvider;
import com.duke.auth.security.LoginUser;
import com.duke.auth.service.GithubService;
import com.duke.auth.service.IAuthService;
import com.duke.auth.service.SmsService;
import com.duke.auth.service.WeixinService;
import com.duke.auth.util.SecurityUtil;
import com.duke.auth.vo.*;
import com.duke.framework.common.Constants;
import com.duke.framework.common.ResultCode;
import com.duke.framework.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 认证服务实现，负责所有登录方式和认证相关操作。
 *
 * <p>Redis Key 约定：
 * <ul>
 *   <li>{@code captcha:{uuid}}       — 图形验证码答案，TTL 5min</li>
 *   <li>{@code sms:code:{phone}}     — 短信验证码，TTL 由 sms.expire-seconds 控制</li>
 *   <li>{@code sms:limit:{phone}}    — 短信发送限流标记，TTL 由 sms.rate-limit-seconds 控制</li>
 *   <li>{@code weixin:state:{uuid}}  — 微信 OAuth CSRF 防护 state，TTL 5min</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final String SMS_LIMIT_PREFIX = "sms:limit:";
    private static final String WEIXIN_STATE_PREFIX = "weixin:state:";
    private static final String GITHUB_STATE_PREFIX = "github:state:";

    private final SmsProperties smsProperties;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysMenuMapper menuMapper;
    private final SysButtonMapper buttonMapper;
    private final SysUserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final SmsService smsService;
    private final WeixinService weixinService;
    private final GithubService githubService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginDTO dto) {
        // captchaId 为空表示调用方未传验证码（如内部调用或测试），跳过验证
        if (StringUtils.hasText(dto.getCaptchaId())) {
            String answer = redisTemplate.opsForValue().get(CAPTCHA_PREFIX + dto.getCaptchaId());
            // 无论正确与否都删除，防止暴力枚举复用同一验证码
            redisTemplate.delete(CAPTCHA_PREFIX + dto.getCaptchaId());
            if (answer == null || !answer.equalsIgnoreCase(dto.getCaptchaCode())) {
                throw new BusinessException("验证码错误或已过期");
            }
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        if (!loginUser.isEnabled()) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        String token = jwtTokenProvider.generateToken(loginUser.getUsername(), loginUser.getUserId());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUsername(loginUser.getUsername());
        vo.setRealName(loginUser.getUser().getRealName());
        vo.setAvatar(loginUser.getUser().getAvatar());
        vo.setButtons(loginUser.getButtonCodes());
        vo.setRoles(loginUser.getRoles());
        return vo;
    }

    @Override
    public CaptchaVO getCaptcha() {
        GifCaptcha captcha = new GifCaptcha(150, 50, 4);
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(CAPTCHA_PREFIX + captchaId, captcha.getCode(), 5, TimeUnit.MINUTES);
        return new CaptchaVO(captchaId, captcha.getImageBase64Data());
    }

    @Override
    public void sendSmsCode(SmsCodeDTO dto) {
        String limitKey = SMS_LIMIT_PREFIX + dto.getPhone();
        // 限流 key 存在说明 60s 内已发送过，拒绝本次请求
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            throw new BusinessException("发送频繁，请 " + smsProperties.getRateLimitSeconds() + " 秒后重试");
        }
        String code = RandomUtil.randomNumbers(6);
        redisTemplate.opsForValue().set(SMS_CODE_PREFIX + dto.getPhone(), code, smsProperties.getExpireSeconds(), TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(limitKey, "1", smsProperties.getRateLimitSeconds(), TimeUnit.SECONDS);
        smsService.sendCode(dto.getPhone(), code);
    }

    @Override
    public LoginVO smsLogin(SmsLoginDTO dto) {
        String codeKey = SMS_CODE_PREFIX + dto.getPhone();
        String storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null || !storedCode.equals(dto.getCode())) {
            throw new BusinessException("验证码错误或已过期");
        }
        // 验证码一次性使用，验证后立即删除，防止重放
        redisTemplate.delete(codeKey);

        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, dto.getPhone()));
        if (user == null) {
            // 首次登录自动注册，密码设为随机 UUID（不可用于密码登录）
            user = new SysUser();
            user.setUsername(dto.getPhone());
            user.setPhone(dto.getPhone());
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setStatus(1);
            userMapper.insert(user);
        }
        if (user.getStatus() != 1) throw new BusinessException(ResultCode.USER_DISABLED);

        return buildLoginVO(user);
    }

    @Override
    public WeixinLoginUrlVO getWeixinLoginUrl() {
        // state 写入 Redis，回调时校验，防止 CSRF 攻击
        String state = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(WEIXIN_STATE_PREFIX + state, "1", 5, TimeUnit.MINUTES);
        return new WeixinLoginUrlVO(weixinService.buildLoginUrl(state));
    }

    @Override
    public LoginVO weixinLogin(WeixinCallbackDTO dto) {
        String stateKey = WEIXIN_STATE_PREFIX + dto.getState();
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(stateKey))) {
            throw new BusinessException("登录状态已过期，请重新扫码");
        }
        // state 一次性消费，防止重放
        redisTemplate.delete(stateKey);

        WeixinService.WeixinUserInfo wxUser = weixinService.getUserInfo(dto.getCode());

        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getWeixinOpenid, wxUser.getOpenid()));
        if (user == null) {
            // 首次微信登录自动注册，用户名取 openid 前 8 位避免太长
            String username = "wx_" + wxUser.getOpenid().substring(0, Math.min(8, wxUser.getOpenid().length()));
            user = new SysUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setNickname(wxUser.getNickname());
            user.setAvatar(wxUser.getHeadimgurl());
            user.setWeixinOpenid(wxUser.getOpenid());
            user.setWeixinUnionid(wxUser.getUnionid());
            user.setStatus(1);
            userMapper.insert(user);
        }
        if (user.getStatus() != 1) throw new BusinessException(ResultCode.USER_DISABLED);

        return buildLoginVO(user);
    }

    @Override
    public GithubLoginUrlVO getGithubLoginUrl() {
        // state 写入 Redis，回调时校验，防止 CSRF 攻击
        String state = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(GITHUB_STATE_PREFIX + state, "1", 5, TimeUnit.MINUTES);
        return new GithubLoginUrlVO(githubService.buildLoginUrl(state));
    }

    @Override
    public LoginVO githubLogin(GithubCallbackDTO dto) {
        String stateKey = GITHUB_STATE_PREFIX + dto.getState();
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(stateKey))) {
            throw new BusinessException("登录状态已过期，请重新授权");
        }
        // state 一次性消费，防止重放
        redisTemplate.delete(stateKey);

        GithubService.GithubUserInfo ghUser = githubService.getUserInfo(dto.getCode());

        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getGithubId, ghUser.getId()));
        if (user == null) {
            // 首次 GitHub 登录自动注册
            String username = "gh_" + ghUser.getLogin();
            // 用户名唯一性保证：若 gh_{login} 已被占用则追加 ID 后四位
            if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0) {
                username = "gh_" + ghUser.getId();
            }
            user = new SysUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setNickname(ghUser.getName() != null ? ghUser.getName() : ghUser.getLogin());
            user.setEmail(ghUser.getEmail());
            user.setAvatar(ghUser.getAvatarUrl());
            user.setGithubId(ghUser.getId());
            user.setGithubLogin(ghUser.getLogin());
            user.setStatus(1);
            userMapper.insert(user);
        }
        if (user.getStatus() != 1) throw new BusinessException(ResultCode.USER_DISABLED);

        return buildLoginVO(user);
    }

    @Override
    public void changePassword(ChangePasswordDTO dto, HttpServletRequest request) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次密码输入不一致");
        }
        LoginUser loginUser = SecurityUtil.getLoginUser();
        if (loginUser == null) throw new BusinessException(ResultCode.UNAUTHORIZED);

        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        validatePasswordStrength(dto.getNewPassword());

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);

        // 密码修改后将当前 token 加入黑名单，强制重新登录，防止旧 token 被滥用
        String bearerToken = request.getHeader(Constants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(Constants.TOKEN_PREFIX)) {
            jwtTokenProvider.invalidateToken(bearerToken.substring(Constants.TOKEN_PREFIX.length()));
        }
    }

    private void validatePasswordStrength(String password) {
        if (!StringUtils.hasText(password) || !password.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$")) {
            throw new BusinessException("密码至少 8 位，且需包含字母和数字");
        }
    }

    /** 为通过手机号/微信登录的用户构建 LoginVO，无角色信息（角色需后台分配） */
    private LoginVO buildLoginVO(SysUser user) {
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setAvatar(user.getAvatar());
        vo.setButtons(Set.of());
        vo.setRoles(List.of());
        return vo;
    }

    @Override
    public List<MenuTreeVO> getMenuTree() {
        LoginUser loginUser = SecurityUtil.getLoginUser();
        if (loginUser == null) return List.of();

        List<SysMenu> menus;
        if (loginUser.getRoles().contains(Constants.SUPER_ADMIN_ROLE)) {
            menus = menuMapper.selectList(
                    new LambdaQueryWrapper<SysMenu>()
                            .in(SysMenu::getMenuType, 1, 2)
                            .eq(SysMenu::getStatus, 1)
                            .orderByAsc(SysMenu::getSortOrder));
        } else {
            menus = menuMapper.selectMenusByUserId(loginUser.getUserId());
        }

        if (menus.isEmpty()) return List.of();

        // 批量预加载所有菜单对应的按钮，消除 N+1 查询
        List<Long> menuIds = menus.stream().map(SysMenu::getId).collect(Collectors.toList());
        Map<Long, List<ButtonVO>> buttonsByMenuId = buttonMapper.selectList(
                        new LambdaQueryWrapper<SysButton>().in(SysButton::getMenuId, menuIds))
                .stream().collect(Collectors.groupingBy(SysButton::getMenuId,
                        Collectors.mapping(b -> {
                            ButtonVO bvo = new ButtonVO();
                            bvo.setId(b.getId());
                            bvo.setButtonName(b.getButtonName());
                            bvo.setButtonCode(b.getButtonCode());
                            bvo.setButtonType(b.getButtonType());
                            bvo.setSortOrder(b.getSortOrder());
                            return bvo;
                        }, Collectors.toList())));

        // 用 groupingBy 预分组，将 O(n²) 的树形构建降为 O(n)
        Map<Long, List<SysMenu>> childrenMap = menus.stream()
                .collect(Collectors.groupingBy(SysMenu::getParentId));
        return buildTree(childrenMap, 0L, buttonsByMenuId);
    }

    private List<MenuTreeVO> buildTree(Map<Long, List<SysMenu>> childrenMap, Long parentId,
                                       Map<Long, List<ButtonVO>> buttonsByMenuId) {
        return childrenMap.getOrDefault(parentId, List.of()).stream()
                .map(m -> {
                    MenuTreeVO vo = new MenuTreeVO();
                    vo.setId(m.getId());
                    vo.setParentId(m.getParentId());
                    vo.setMenuName(m.getMenuName());
                    vo.setMenuType(m.getMenuType());
                    vo.setPath(m.getPath());
                    vo.setComponent(m.getComponent());
                    vo.setIcon(m.getIcon());
                    vo.setSortOrder(m.getSortOrder());
                    vo.setVisible(m.getVisible());
                    // 只有叶子菜单（menuType=2）才有按钮权限
                    if (m.getMenuType() == 2) {
                        vo.setButtons(buttonsByMenuId.getOrDefault(m.getId(), List.of()));
                    }
                    vo.setChildren(buildTree(childrenMap, m.getId(), buttonsByMenuId));
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
