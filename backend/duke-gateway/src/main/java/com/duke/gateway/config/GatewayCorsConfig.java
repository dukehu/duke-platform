package com.duke.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Gateway CORS 跨域配置
 * 使用 CorsWebFilter 拦截所有请求，在网关层统一处理跨域
 * 注意：必须在所有其他过滤器之前执行
 */
@Configuration
public class GatewayCorsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE) // 关键：强制最高优先级
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        corsConfig.setAllowedOriginPatterns(Arrays.asList("*"));


        // 允许所有请求头
        corsConfig.addAllowedHeader("*");

        // 允许的请求方法
        corsConfig.addAllowedMethod("GET");
        corsConfig.addAllowedMethod("POST");
        corsConfig.addAllowedMethod("PUT");
        corsConfig.addAllowedMethod("DELETE");
        corsConfig.addAllowedMethod("OPTIONS");
        corsConfig.addAllowedMethod("PATCH");

        // 允许携带认证信息
        corsConfig.setAllowCredentials(true);

        // 预检请求缓存时间
        corsConfig.setMaxAge(3600L);

        // 暴露的响应头
        corsConfig.addExposedHeader("Authorization");
        corsConfig.addExposedHeader("Content-Type");
        corsConfig.addExposedHeader("X-Total-Count");
        corsConfig.addExposedHeader("X-Page-Number");
        corsConfig.addExposedHeader("X-Page-Size");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.setCorsConfigurations(java.util.Collections.singletonMap("/**", corsConfig));

        return new CorsWebFilter(source);
    }
}
