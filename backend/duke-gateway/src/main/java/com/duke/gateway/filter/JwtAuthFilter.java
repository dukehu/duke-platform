package com.duke.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey signingKey;

    @PostConstruct
    private void init() {
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    // 白名单：无需 JWT 验证的路径（网关视角）
    private static final List<String> WHITE_LIST = List.of(
            // 业务接口
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/weixin/url",
            "/api/auth/sms/send",
            "/api/auth/sms/login",
            "/api/auth/github/url",
            "/api/auth/github/callback",
            "/api/auth/captcha",
            "/api/transformer/**",
            "/api/knowledge-qa/**",
            // API 文档（网关统一管理）
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            // API 文档聚合路由（转发下游服务的文档）
            "/swagger-auth/**",
            "/swagger-transformer/**",
            "/swagger-knowledge-qa/**"
    );

    // 需要从外部请求中剥离的敏感请求头，防止客户端伪造用户身份
    private static final List<String> SENSITIVE_HEADERS = List.of("X-Username", "X-User-Id");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单放行，但仍需剥离敏感头防止伪造
        if (isWhitelisted(path)) {
            ServerWebExchange cleaned = stripSensitiveHeaders(exchange);
            return chain.filter(cleaned);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange.getResponse(), "未提供认证token");
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            Object userId = claims.get("userId");

            // 先剥离外部传入的同名头，再附加经过验证的用户信息
            ServerHttpRequest mutatedRequest = stripSensitiveHeaders(exchange).getRequest()
                    .mutate()
                    .header("X-Username", username)
                    .header("X-User-Id", userId != null ? userId.toString() : "")
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
            // 将 userId 存入 exchange attributes，供 ApiPermissionFilter 使用
            if (userId != null) {
                mutatedExchange.getAttributes().put("userId", Long.parseLong(userId.toString()));
            }
            return chain.filter(mutatedExchange);

        } catch (Exception e) {
            log.warn("JWT 验证失败: {}", e.getMessage());
            return unauthorized(exchange.getResponse(), "token无效或已过期");
        }
    }

    private boolean isWhitelisted(String path) {
        for (String pattern : WHITE_LIST) {
            if (PATH_MATCHER.match(pattern, path)) return true;
        }
        return false;
    }

    private ServerWebExchange stripSensitiveHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> SENSITIVE_HEADERS.forEach(headers::remove))
                .build();
        return exchange.mutate().request(request).build();
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String msg) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // 与 duke-auth Result 格式保持一致：code/message/data
        String body = String.format("{\"code\":401,\"message\":\"%s\",\"data\":null}", msg);
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
