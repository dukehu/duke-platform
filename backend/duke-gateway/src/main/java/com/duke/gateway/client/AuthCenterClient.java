package com.duke.gateway.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AuthCenterClient {

    private static final String CHECK_URI = "/auth/internal/gateway/check";
    private static final String PERMISSIONS_URI = "/auth/internal/users/{userId}/permissions";
    private static final String HEADER_GATEWAY_SECRET = "X-Gateway-Secret";

    private final WebClient webClient;

    public AuthCenterClient(
            WebClient.Builder builder,
            @Value("${duke-auth.internal-base-url}") String baseUrl,
            @Value("${gateway.internal-secret}") String secret) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HEADER_GATEWAY_SECRET, secret)
                .build();
        log.info("[鉴权客户端初始化] baseUrl:{}", baseUrl);
    }

    public Mono<Boolean> checkPermission(Long userId, String appId, String path, String httpMethod) {
        LocalDateTime start = LocalDateTime.now();

        log.info(
                "[权限校验-请求] userId={},appId={},path={},method={},uri={}",
                userId, appId, path, httpMethod, CHECK_URI
        );

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(CHECK_URI)
                        .queryParam("userId", userId)
                        .queryParam("appId", appId)
                        .queryParam("path", path)
                        .queryParam("httpMethod", httpMethod)
                        .build())
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), resp ->
                        Mono.error(new RuntimeException("鉴权服务异常状态码：" + resp.statusCode().value()))
                )
                .bodyToMono(JsonNode.class)
                .doOnSuccess(node -> {
                    long cost = Duration.between(start, LocalDateTime.now()).toMillis();
                    log.info(
                            "[权限校验-成功] 耗时{}ms，完整响应：{}",
                            cost, node.toString()
                    );
                })
                .map(node -> node.has("data") ? node.get("data").asBoolean(false) : false)
                .onErrorResume(e -> {
                    long cost = Duration.between(start, LocalDateTime.now()).toMillis();
                    if (e instanceof WebClientResponseException ex) {
                        log.error(
                                "[权限校验-HTTP异常] 耗时{}ms，状态码：{}，响应：{}",
                                cost, ex.getRawStatusCode(), ex.getResponseBodyAsString(), e
                        );
                    } else {
                        log.error(
                                "[权限校验-失败] 耗时{}ms，原因：{}",
                                cost, e.getMessage(), e
                        );
                    }
                    // 服务挂了直接拦截，安全兜底
                    return Mono.just(false);
                });
    }

    /**
     * 获取用户权限列表
     *
     * @param userId 用户 ID
     * @return 权限标识列表
     */
    public Mono<List<String>> getUserPermissions(Long userId) {
        LocalDateTime start = LocalDateTime.now();
        
        log.debug("[获取权限-请求] userId={}", userId);
        
        return webClient.get()
                .uri(PERMISSIONS_URI, userId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .<List<String>>map(node -> {
                    if (node.has("data") && node.get("data").isArray()) {
                        List<String> permissions = new ArrayList<>();
                        node.get("data").forEach(p -> permissions.add(p.asText()));
                        log.debug("[获取权限-成功] userId={}, count={}", userId, permissions.size());
                        return permissions;
                    }
                    log.warn("[获取权限-响应格式异常] userId={}", userId);
                    return new ArrayList<>();
                })
                .onErrorResume(e -> {
                    long cost = Duration.between(start, LocalDateTime.now()).toMillis();
                    log.error("[获取权限-失败] userId={}, 耗时{}ms, 原因: {}", userId, cost, e.getMessage());
                    return Mono.just(new ArrayList<String>());
                });
    }
}