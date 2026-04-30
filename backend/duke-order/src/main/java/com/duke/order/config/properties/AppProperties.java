package com.duke.order.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Order 鏈嶅姟鑷畾涔夐厤缃? * 鍦?application.yml 鎴?Nacos 涓互 order: 鍓嶇紑閰嶇疆
 */
@Data
@ConfigurationProperties(prefix = "order")
public class AppProperties {
    // TODO
}



