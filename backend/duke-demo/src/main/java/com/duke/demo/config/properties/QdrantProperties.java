package com.duke.knowledgeqa.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "qdrant")
public class QdrantProperties {
    private String host;
    private Integer port;
    private String apiKey;
}
