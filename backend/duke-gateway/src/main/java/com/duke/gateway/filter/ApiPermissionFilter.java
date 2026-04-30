package com.duke.gateway.filter;

import com.duke.gateway.client.AuthCenterClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiPermissionFilter implements GlobalFilter, Ordered {

    private static final int ORDER = -90;
    private final AuthCenterClient authCenterClient;

    @Value("#{${gateway.route-prefix-map}}")
    private Map<String, String> routePrefixMap;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<String> WHITE_LIST = List.of(
            // 业务接口
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/weixin/url",
            "/api/auth/weixin/callback",
            "/api/auth/sms/send",
            "/api/auth/sms/login",
            "/api/auth/github/url",
            "/api/auth/github/callback",
            "/api/auth/captcha",
            "/api/transformer/**",
            "/api/knowledge-qa/**",
            // 网关 Swagger UI 和 API 文档
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            // 服务 API 文档（通过业务路由聚合）
            "/api/auth/v3/api-docs/**",
            "/api/transformer/v3/api-docs/**",
            "/api/knowledge-qa/v3/api-docs/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 修正路径获取方式
        String gatewayPath = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        // 2. 白名单放行
        if (WHITE_LIST.stream().anyMatch(p -> PATH_MATCHER.match(p, gatewayPath))) {
            return chain.filter(exchange);
        }

        // 3. 获取 userId（由前面的 JwtAuthFilter 写入）
        Long userId = exchange.getAttribute("userId");
        if (userId == null) {
            log.warn("未获取到userId，拒绝访问: path={}", gatewayPath);
            return writeResponse(exchange.getResponse(), HttpStatus.FORBIDDEN, 403, "用户未认证");
        }

        // 4. 获取路由信息
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return chain.filter(exchange);
        }

        String routeId = route.getId();
        String prefix = routePrefixMap.get(routeId);
        if (prefix == null) {
            return chain.filter(exchange);
        }

        // 5. 截取服务路径
        String servicePath = gatewayPath.startsWith(prefix)
                ? gatewayPath.substring(prefix.length())
                : gatewayPath;
        if (!servicePath.startsWith("/")) {
            servicePath = "/" + servicePath;
        }

        // 6. 调用权限校验接口
        String finalServicePath = servicePath;
        return authCenterClient.checkPermission(userId, routeId, servicePath, method)
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorReturn(TimeoutException.class, false)
                .onErrorReturn(false)
                .flatMap(allowed -> {
                    if (Boolean.TRUE.equals(allowed)) {
                        return chain.filter(exchange);
                    }
                    log.warn("权限不足 userId={}, route={}, path={}", userId, routeId, finalServicePath);
                    return writeResponse(exchange.getResponse(), HttpStatus.FORBIDDEN, 403, "无权限访问");
                });
    }

    private Mono<Void> writeResponse(ServerHttpResponse response, HttpStatus status, int code, String msg) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"code\":%d,\"message\":\"%s\",\"data\":null}",
                code, msg
        );
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer))
                .doOnError(e -> log.error("响应写入异常: {}", e.getMessage()));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}