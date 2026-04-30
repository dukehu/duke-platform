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

@Slf4j
@Component
public class AuthCenterClient {

    // 鉴权中心的 context-path 是 /auth，所以完整路径是 /auth/internal/gateway/check
    private static final String CHECK_URI = "/auth/internal/gateway/check";
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
}