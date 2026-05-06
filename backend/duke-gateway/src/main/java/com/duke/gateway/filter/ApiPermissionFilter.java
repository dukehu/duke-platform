package com.duke.gateway.filter;

import com.duke.gateway.client.AuthCenterClient;
import com.duke.gateway.config.GatewayWhiteListConfig;
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
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * API 权限过滤器 - 粗粒度权限控制
 * 
 * 职责：
 * 1. 检查用户是否有访问该 API 的权限（基于 sys_api 表配置）
 * 2. 获取用户权限列表并传递给微服务（供 @PreAuthorize 使用）
 * 3. 快速拦截未授权请求，保护微服务
 * 
 * 注意：这是第一层权限检查（API 级别），微服务可以做更细粒度的业务级权限控制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiPermissionFilter implements GlobalFilter, Ordered {

    private static final int ORDER = -90;
    private final AuthCenterClient authCenterClient;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 修正路径获取方式
        String gatewayPath = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        // 2. 白名单放行
        if (GatewayWhiteListConfig.WHITE_LIST.stream().anyMatch(p -> PATH_MATCHER.match(p, gatewayPath))) {
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
        
        // 5. 从网关路径中提取服务路径（约定：/api/{service-name}/** → /{context-path}/**）
        // 例如：/api/storage/files/list → routeId=duke-storage → servicePath=/files/list
        String servicePath = extractServicePath(gatewayPath, routeId);

        // 6. 调用权限校验接口
        String finalServicePath = servicePath;
        return authCenterClient.checkPermission(userId, routeId, servicePath, method)
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorReturn(TimeoutException.class, false)
                .onErrorReturn(false)
                .flatMap(allowed -> {
                    if (Boolean.TRUE.equals(allowed)) {
                        // 获取用户权限列表并放入 Header（供微服务 @PreAuthorize 使用）
                        return authCenterClient.getUserPermissions(userId)
                                .subscribeOn(Schedulers.boundedElastic())
                                .onErrorReturn(new ArrayList<>())  // 异常时返回空列表
                                .map((java.util.List<String> permissions) -> {
                                    ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
                                    
                                    // 无论权限列表是否为空，都添加 Header（空字符串表示无权限）
                                    if (permissions != null && !permissions.isEmpty()) {
                                        String permissionsStr = String.join(",", permissions);
                                        requestBuilder.header("X-User-Permissions", permissionsStr);
                                        log.debug("用户权限已传递: userId={}, permissions={}", userId, permissionsStr);
                                    } else {
                                        requestBuilder.header("X-User-Permissions", "");
                                        log.debug("用户无权限: userId={}", userId);
                                    }
                                    
                                    ServerHttpRequest mutatedRequest = requestBuilder.build();
                                    return exchange.mutate().request(mutatedRequest).build();
                                })
                                .flatMap(chain::filter);
                    }
                    log.warn("权限不足 userId={}, route={}, path={}", userId, routeId, finalServicePath);
                    return writeResponse(exchange.getResponse(), HttpStatus.FORBIDDEN, 403, "无权限访问");
                });
    }

    /**
     * 从网关路径提取服务路径
     * 约定：路由 ID 格式为 duke-{service-name}，网关路径格式为 /api/{service-name}/**
     * 例如：routeId=duke-storage, gatewayPath=/api/storage/files/list → /files/list
     */
    private String extractServicePath(String gatewayPath, String routeId) {
        // 从路由 ID 提取服务名称（去掉 "duke-" 前缀）
        String serviceName = routeId.startsWith("duke-") ? routeId.substring(5) : routeId;
        
        // 构造期望的前缀：/api/{service-name}
        String expectedPrefix = "/api/" + serviceName;
        
        // 如果路径以期望的前缀开头，去掉前缀
        if (gatewayPath.startsWith(expectedPrefix)) {
            String servicePath = gatewayPath.substring(expectedPrefix.length());
            return servicePath.isEmpty() ? "/" : servicePath;
        }
        
        // 否则返回原路径
        return gatewayPath;
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
