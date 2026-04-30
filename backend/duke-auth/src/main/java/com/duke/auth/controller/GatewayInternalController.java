package com.duke.auth.controller;

import com.duke.framework.common.Result;
import com.duke.auth.service.IGatewayPermissionService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@Tag(name = "认证管理")
@RestController
@RequestMapping("/internal/gateway")
@RequiredArgsConstructor
public class GatewayInternalController {

    private final IGatewayPermissionService gatewayPermissionService;

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
    @GetMapping("/check")
    public Result<Boolean> check(
            @RequestParam Long userId,
            @RequestParam String appId,
            @RequestParam String path,
            @RequestParam String httpMethod) {
        boolean allowed = gatewayPermissionService.checkPermission(userId, appId, path, httpMethod);
        return Result.success(allowed);
    }
}
