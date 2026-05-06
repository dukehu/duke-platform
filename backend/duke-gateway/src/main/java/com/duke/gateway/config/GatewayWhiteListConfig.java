package com.duke.gateway.config;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 网关白名单配置
 * 
 * 说明：
 * - 这些路径不需要 JWT 验证和 API 权限检查
 * - 通常包括：登录接口、公开接口、API 文档等
 * - 两个过滤器（JwtAuthFilter 和 ApiPermissionFilter）共用此配置
 */
@Component
public class GatewayWhiteListConfig {

    /**
     * 无需认证和权限检查的白名单路径
     */
    public static final List<String> WHITE_LIST = List.of(
            // ========== 认证相关接口 ==========
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/captcha",
            
            // ========== 第三方登录 ==========
            "/api/auth/weixin/url",
            "/api/auth/weixin/callback",
            "/api/auth/github/url",
            "/api/auth/github/callback",
            
            // ========== SMS 登录 ==========
            "/api/auth/sms/send",
            "/api/auth/sms/login",
            
            // ========== 文件预览接口（iframe 无法携带 token） ==========
            "/api/storage/files/preview/**",
            
            // ========== API 文档 ==========
            // 网关级别的 Swagger
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            
            // 各服务的 API 文档
            "/api/auth/v3/api-docs/**",
            "/api/transformer/v3/api-docs/**",
            "/api/knowledge-qa/v3/api-docs/**",
            "/api/demo/v3/api-docs/**",
            "/api/storage/v3/api-docs/**"
    );

    /**
     * 敏感请求头列表（需要剥离，防止客户端伪造）
     */
    public static final List<String> SENSITIVE_HEADERS = List.of(
            "X-Username",
            "X-User-Id",
            "X-User-Permissions"
    );
}
