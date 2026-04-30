package com.duke.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID = "X-Trace-Id";
    private static final String START_TIME = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();

        // 将 traceId 存入 exchange，供后续过滤器使用
        exchange.getAttributes().put(TRACE_ID, traceId);
        exchange.getAttributes().put(START_TIME, startTime);

        // 记录请求信息
        logRequest(request, traceId);

        return chain.filter(exchange).doFinally(signalType -> {
            ServerHttpResponse response = exchange.getResponse();
            long duration = System.currentTimeMillis() - startTime;
            logResponse(request, response, traceId, duration);
        });
    }

    private void logRequest(ServerHttpRequest request, String traceId) {
        String method = request.getMethod() != null ? request.getMethod().toString() : "UNKNOWN";
        String path = request.getURI().getPath();
        String query = request.getURI().getQuery();
        String fullPath = query != null ? path + "?" + query : path;

        // 记录请求头中的授权信息（脱敏）
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String authInfo = authorization != null ? "Bearer [***]" : "None";

        log.info("[REQUEST] TraceId={} {} {} | Auth: {} | RemoteAddr: {}",
                traceId, method, fullPath, authInfo, getRemoteAddr(request));
    }

    private void logResponse(ServerHttpRequest request, ServerHttpResponse response, String traceId, long duration) {
        String method = request.getMethod() != null ? request.getMethod().toString() : "UNKNOWN";
        String path = request.getURI().getPath();
        int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 0;

        log.info("[RESPONSE] TraceId={} {} {} | Status: {} | Duration: {}ms",
                traceId, method, path, statusCode, duration);
    }

    private String getRemoteAddr(ServerHttpRequest request) {
        // 优先使用 X-Forwarded-For（代理环境）
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "Unknown";
    }

    @Override
    public int getOrder() {
        return -200;  // 最高优先级，确保在其他过滤器之前执行
    }
}
