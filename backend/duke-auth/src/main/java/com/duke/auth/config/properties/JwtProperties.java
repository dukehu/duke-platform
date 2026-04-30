package com.duke.auth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置项，对应 application.yml 中的 jwt.* 前缀。
 * 生产环境必须通过 JWT_SECRET 环境变量覆盖默认密钥。
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /** HMAC-SHA256 签名密钥，长度至少 32 字符 */
    private String secret;
    /** Token 有效期（秒），默认 86400 = 24h */
    private long expiration = 86400;
}
