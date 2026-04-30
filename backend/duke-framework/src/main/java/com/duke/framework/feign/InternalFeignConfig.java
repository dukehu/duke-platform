package com.duke.framework.feign;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalFeignConfig {

    @Value("${gateway.internal-secret}")
    private String internalSecret;

    @Bean
    public RequestInterceptor internalSecretInterceptor() {
        return template -> template.header("X-Gateway-Secret", internalSecret);
    }
}
