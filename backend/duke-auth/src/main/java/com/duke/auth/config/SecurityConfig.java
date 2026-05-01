package com.duke.auth.config;

import com.duke.framework.common.Result;
import com.duke.framework.common.ResultCode;
import com.duke.auth.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security 全局安全配置。
 * 采用无状态（STATELESS）会话策略，认证完全依赖 JWT，不使用 Session/Cookie。
 * 白名单路径无需 token 即可访问，其余接口须通过 JwtAuthenticationFilter 校验。
 * 401/403 均返回 JSON 格式，便于前端统一处理。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 开启 @PreAuthorize 方法级权限控制
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    /** 不需要认证的公开路径（去掉 context-path: /auth 后的路径） */
    private static final String[] WHITE_LIST = {
            "/login",
            "/captcha",
            "/sms/send",
            "/sms/login",
            "/weixin/url",
            "/weixin/callback",
            "/github/url",
            "/github/callback",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/internal/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)   // 无状态 JWT 不需要 CSRF 防护
                .logout(AbstractHttpConfigurer::disable) // 禁用 form-based logout（使用 REST API 处理）
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // 将白名单转换为 RequestMatcher 数组
                    List<RequestMatcher> matchers = new ArrayList<>();
                    for (String pattern : WHITE_LIST) {
                        matchers.add(new AntPathRequestMatcher(pattern));
                    }
                    auth.requestMatchers(matchers.toArray(new RequestMatcher[0])).permitAll();
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        // 未携带/无效 token → 401
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getOutputStream().write(
                                    objectMapper.writeValueAsString(Result.fail(ResultCode.UNAUTHORIZED))
                                            .getBytes(StandardCharsets.UTF_8));
                        })
                        // token 有效但无权限 → 403
                        .accessDeniedHandler((request, response, e) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getOutputStream().write(
                                    objectMapper.writeValueAsString(Result.fail(ResultCode.FORBIDDEN))
                                            .getBytes(StandardCharsets.UTF_8));
                        })
                )
                // JWT 过滤器插在 UsernamePasswordAuthenticationFilter 之前，
                // 确保请求到达表单认证过滤器时 SecurityContext 已被填充
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
