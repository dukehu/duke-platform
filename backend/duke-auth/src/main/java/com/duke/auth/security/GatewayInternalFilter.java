package com.duke.auth.security;

import com.duke.auth.config.properties.GatewayProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 网关内部接口保护过滤器。
 * /rbac/internal/gateway/** 接口只允许网关通过共享密钥调用，
 * 不经过 Spring Security 认证（绕过 JWT 校验），因此单独用此过滤器前置拦截。
 * Order=1 确保在 Spring Security 过滤链之前执行。
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class GatewayInternalFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PATH_PREFIX = "/rbac/internal/gateway/";
    private static final String SECRET_HEADER = "X-Gateway-Secret";

    private final GatewayProperties gatewayProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String secret = request.getHeader(SECRET_HEADER);
        if (!gatewayProperties.getInternalSecret().equals(secret)) {
            log.warn("非法访问内部接口: uri={}, remoteAddr={}", request.getRequestURI(), request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getOutputStream().write(
                    "{\"code\":403,\"message\":\"非法访问\",\"data\":null}".getBytes(StandardCharsets.UTF_8));
            return;
        }

        chain.doFilter(request, response);
    }
}
