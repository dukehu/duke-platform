package com.duke.storage.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件服务配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileProperties {
    
    /**
     * 本地存储基础路径
     */
    private String localBasePath = "D:/duke-storage/upload/";
    
    /**
     * 分片临时存储路径
     */
    private String chunkTempPath = "D:/duke-storage/temp/";
    
    /**
     * 允许的文件后缀
     */
    private String allowSuffix = "pdf,doc,docx,txt,xlsx,png,jpg,jpeg";
    
    /**
     * 最大文件大小（MB）
     */
    private Long maxFileSize = 50L;
    
    /**
     * 分片大小（MB）
     */
    private Integer chunkSize = 5;
    
    /**
     * 分片过期天数
     */
    private Integer chunkExpireDay = 7;
    
    /**
     * 逻辑删除文件保留天数
     */
    private Integer deleteExpireDay = 30;
}
