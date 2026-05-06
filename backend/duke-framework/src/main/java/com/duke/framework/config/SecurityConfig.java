package com.duke.framework.config;

import com.duke.framework.common.Result;
import com.duke.framework.common.ResultCode;
import com.duke.framework.security.GatewayAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

/**
 * 微服务统一安全配置（第二层权限控制）
 * 
 * 职责：
 * 1. 从网关传递的 Header 中提取用户信息和权限列表
 * 2. 填充 SecurityContext，支持 @PreAuthorize 注解
 * 3. 提供方法级的细粒度权限控制能力
 * 
 * 架构说明：
 * - 第一层（网关）：API 级别的粗粒度权限检查
 * - 第二层（微服务）：方法级的细粒度业务权限控制
 * 
 * 使用方式：
 * 1. 微服务引入 duke-framework 依赖
 * 2. 启动类添加 scanBasePackages = {"com.duke.xxx", "com.duke.framework"}
 * 3. 即可使用 @PreAuthorize 进行方法级权限控制
 * 
 * 示例：
 * ```java
 * @PreAuthorize("hasAuthority('storage:file:delete')")
 * public void delete(@PathVariable Long fileId) {
 *     // 可以在此处添加业务级权限检查
 *     // 例如：只能删除自己上传的文件
 * }
 * ```
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 启用 @PreAuthorize
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayAuthFilter gatewayAuthFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（API 服务不需要）
            .csrf(AbstractHttpConfigurer::disable)
            
            // 禁用 Session（无状态认证）
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 授权配置
            .authorizeHttpRequests(auth -> auth
                // Swagger 文档放行
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                
                // 其他请求需要认证（由 GatewayAuthFilter 填充 SecurityContext）
                .anyRequest().authenticated()
            )
            
            // 异常处理：返回 JSON 格式
            .exceptionHandling(ex -> ex
                // 未认证 → 401
                .authenticationEntryPoint((request, response, e) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getOutputStream().write(
                            objectMapper.writeValueAsString(Result.fail(ResultCode.UNAUTHORIZED))
                                    .getBytes(StandardCharsets.UTF_8));
                })
                // 无权限 → 403
                .accessDeniedHandler((request, response, e) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getOutputStream().write(
                            objectMapper.writeValueAsString(Result.fail(ResultCode.FORBIDDEN))
                                    .getBytes(StandardCharsets.UTF_8));
                })
            )
            
            // 添加网关认证过滤器
            .addFilterBefore(gatewayAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
