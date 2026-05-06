package com.duke.demo.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文档处理管道配置属性
 * 前缀：document.pipeline
 */
@Data
@Component
@ConfigurationProperties(prefix = "document.pipeline")
public class DocumentPipelineProperties {

    // 每块字符数
    private int chunkSize = 500;

    // 相邻块重叠字符数
    private int overlap = 50;

    // 每批向量化+入库的块数
    private int batchSize = 10;

    // 默认集合名
    private String defaultCollection = "documents";
}
