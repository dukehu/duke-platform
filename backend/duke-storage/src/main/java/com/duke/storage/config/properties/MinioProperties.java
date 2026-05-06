package com.duke.storage.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    
    /**
     * 是否启用MinIO
     */
    private Boolean enable = false;
    
    /**
     * MinIO端点
     */
    private String endpoint = "http://127.0.0.1:9000";
    
    /**
     * 访问密钥
     */
    private String accessKey = "minioadmin";
    
    /**
     * 秘密密钥
     */
    private String secretKey = "minioadmin";
    
    /**
     * Bucket名称
     */
    private String bucketName = "duke-file-bucket";
}
