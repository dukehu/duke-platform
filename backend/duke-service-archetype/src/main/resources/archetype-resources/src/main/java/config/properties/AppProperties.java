package ${package}.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ${classPrefix} 鏈嶅姟鑷畾涔夐厤缃? * 鍦?application.yml 鎴?Nacos 涓互 ${classPrefixLower}: 鍓嶇紑閰嶇疆
 */
@Data
@ConfigurationProperties(prefix = "${classPrefixLower}")
public class AppProperties {
    // TODO
}



