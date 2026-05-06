package com.duke.auth.controller;

import com.duke.framework.common.Constants;
import com.duke.framework.common.Result;
import com.duke.auth.dto.*;
import com.duke.auth.mapper.SysApiMapper;
import com.duke.auth.mapper.SysRoleMapper;
import com.duke.auth.security.JwtTokenProvider;
import com.duke.auth.security.LoginUser;
import com.duke.auth.service.IAuthService;
import com.duke.auth.service.IGatewayPermissionService;
import com.duke.auth.util.SecurityUtil;
import com.duke.auth.vo.*;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Tag(name = "认证管理")
@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final IGatewayPermissionService gatewayPermissionService;
    private final SysApiMapper apiMapper;
    private final SysRoleMapper roleMapper;

    @Operation(summary = "用户登录（账号密码）")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.success(authService.getCaptcha());
    }

    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms/send")
    public Result<Void> sendSmsCode(@Valid @RequestBody SmsCodeDTO dto) {
        authService.sendSmsCode(dto);
        return Result.success();
    }

    @Operation(summary = "短信验证码登录")
    @PostMapping("/sms/login")
    public Result<LoginVO> smsLogin(@Valid @RequestBody SmsLoginDTO dto) {
        return Result.success(authService.smsLogin(dto));
    }

    @Operation(summary = "获取微信登录二维码URL")
    @GetMapping("/weixin/url")
    public Result<WeixinLoginUrlVO> weixinUrl() {
        return Result.success(authService.getWeixinLoginUrl());
    }

    @Operation(summary = "微信登录回调")
    @PostMapping("/weixin/callback")
    public Result<LoginVO> weixinCallback(@Valid @RequestBody WeixinCallbackDTO dto) {
        return Result.success(authService.weixinLogin(dto));
    }

    @Operation(summary = "获取GitHub登录授权URL")
    @GetMapping("/github/url")
    public Result<GithubLoginUrlVO> githubUrl() {
        return Result.success(authService.getGithubLoginUrl());
    }

    @Operation(summary = "GitHub登录回调")
    @PostMapping("/github/callback")
    public Result<LoginVO> githubCallback(@Valid @RequestBody GithubCallbackDTO dto) {
        return Result.success(authService.githubLogin(dto));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<LoginVO> info() {
        LoginUser loginUser = SecurityUtil.getLoginUser();
        LoginVO vo = new LoginVO();
        vo.setUsername(loginUser.getUsername());
        vo.setRealName(loginUser.getUser().getRealName());
        vo.setAvatar(loginUser.getUser().getAvatar());
        vo.setButtons(loginUser.getButtonCodes());
        vo.setRoles(loginUser.getRoles());
        return Result.success(vo);
    }

    @Operation(summary = "获取当前用户菜单树")
    @GetMapping("/menu")
    public Result<List<MenuTreeVO>> menu() {
        return Result.success(authService.getMenuTree());
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String bearerToken = request.getHeader(Constants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(Constants.TOKEN_PREFIX)) {
            jwtTokenProvider.invalidateToken(bearerToken.substring(Constants.TOKEN_PREFIX.length()));
        }
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto, HttpServletRequest request) {
        authService.changePassword(dto, request);
        return Result.success();
    }

    // ==================== 网关内部接口（仅允许网关调用）====================

    /**
     * 网关权限检查：判断指定用户是否有权限访问指定接口
     * 仅允许网关调用，由 GatewayInternalFilter 校验 X-Gateway-Secret
     *
     * @param userId     用户 ID（来自 JWT）
     * @param appId      目标服务 ID
     * @param path       服务路径（已去掉网关前缀，如 /user/123）
     * @param httpMethod HTTP 方法（GET/POST/PUT/DELETE）
     * @return true=允许，false=拒绝
     */
    @Hidden
    @GetMapping("/internal/gateway/check")
    public Result<Boolean> checkGatewayPermission(
            @RequestParam Long userId,
            @RequestParam String appId,
            @RequestParam String path,
            @RequestParam String httpMethod) {
        boolean allowed = gatewayPermissionService.checkPermission(userId, appId, path, httpMethod);
        return Result.success(allowed);
    }

    /**
     * 获取用户权限列表
     * 仅允许网关调用
     *
     * @param userId 用户 ID
     * @return 权限标识列表
     */
    @Hidden
    @GetMapping("/internal/users/{userId}/permissions")
    public Result<Set<String>> getUserPermissions(@PathVariable Long userId) {
        // 判断是否为超级管理员
        boolean superAdmin = roleMapper.countSuperAdminByUserId(userId, Constants.SUPER_ADMIN_ROLE) > 0;
        
        Set<String> permissions;
        if (superAdmin) {
            // 超级管理员拥有所有 API 权限
            permissions = apiMapper.selectAllApiPermissions();
        } else {
            // 普通用户只拥有角色分配的权限
            permissions = apiMapper.selectApiPermissionsByUserId(userId);
        }
        
        return Result.success(new HashSet<>(permissions));
    }
}
