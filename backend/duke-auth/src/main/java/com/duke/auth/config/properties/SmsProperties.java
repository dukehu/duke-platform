package com.duke.auth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 短信验证码配置，对应 application.yml 中的 sms.* 前缀。
 */
@Data
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {
    /** 验证码有效期（秒），默认 5 分钟 */
    private long expireSeconds = 300;
    /** 同一手机号发送间隔限制（秒），防刷，默认 60 秒 */
    private long rateLimitSeconds = 60;
}
