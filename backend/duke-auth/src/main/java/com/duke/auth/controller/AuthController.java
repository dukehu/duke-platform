package com.duke.auth.controller;

import com.duke.framework.common.Constants;
import com.duke.framework.common.Result;
import com.duke.auth.dto.*;
import com.duke.auth.security.JwtTokenProvider;
import com.duke.auth.security.LoginUser;
import com.duke.auth.service.IAuthService;
import com.duke.auth.util.SecurityUtil;
import com.duke.auth.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "认证管理")
@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

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
}
