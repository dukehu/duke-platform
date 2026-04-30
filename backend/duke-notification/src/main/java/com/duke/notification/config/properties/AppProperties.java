package com.duke.notification.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Notification 鏈嶅姟鑷畾涔夐厤缃? * 鍦?application.yml 鎴?Nacos 涓互 notification: 鍓嶇紑閰嶇疆
 */
@Data
@ConfigurationProperties(prefix = "notification")
public class AppProperties {
    // TODO
}



