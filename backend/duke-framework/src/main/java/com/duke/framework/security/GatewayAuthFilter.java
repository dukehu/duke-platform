package com.duke.framework.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 网关认证信息传递过滤器
 * 
 * 职责：
 * 1. 从网关传递的 Header 中提取用户信息（userId, username）
 * 2. 解析权限列表（X-User-Permissions）
 * 3. 填充 SecurityContext，使 @PreAuthorize 能够工作
 * 
 * 工作流程：
 * 网关验证 JWT → 获取用户权限 → 放入 Header → 微服务提取 → 填充 SecurityContext
 */
@Slf4j
@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-Username";
    private static final String HEADER_PERMISSIONS = "X-User-Permissions";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String userId = request.getHeader(HEADER_USER_ID);
        String username = request.getHeader(HEADER_USERNAME);
        String permissions = request.getHeader(HEADER_PERMISSIONS);
        
        log.debug("网关过滤器接收到 - userId: {}, username: {}, permissions: {}", userId, username, permissions);

        // 如果网关传递了用户信息，则填充 SecurityContext
        if (StringUtils.hasText(userId) && StringUtils.hasText(username)) {
            try {
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                
                // 解析权限列表
                if (StringUtils.hasText(permissions)) {
                    String[] permissionArray = permissions.split(",");
                    for (String permission : permissionArray) {
                        String trimmed = permission.trim();
                        if (StringUtils.hasText(trimmed)) {
                            authorities.add(new SimpleGrantedAuthority(trimmed));
                        }
                    }
                }

                // 创建认证对象
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                authorities
                        );
                
                // 填充到 SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.info("网关用户信息已填充: userId={}, username={}, permissionsCount={}", 
                         userId, username, authorities.size());
                
            } catch (Exception e) {
                log.error("填充网关用户信息失败: {}", e.getMessage(), e);
            }
        } else {
            log.warn("网关未传递用户信息 - userId: {}, username: {}", userId, username);
        }

        filterChain.doFilter(request, response);
    }
}
