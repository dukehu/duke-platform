package com.duke.auth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关内部通信配置，对应 application.yml 中的 gateway.* 前缀。
 * internalSecret 用于鉴别来自 gateway 的内部调用，
 * 必须与网关侧 GatewayInternalFilter 发送的 X-Gateway-Secret 头保持一致。
 */
@Data
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {
    /** 内部接口共享密钥，生产环境通过 GATEWAY_INTERNAL_SECRET 环境变量注入 */
    private String internalSecret;
}
