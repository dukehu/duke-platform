package com.duke.knowledgeqa.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingProperties {
    private String model;
    private String endpoint;
    private String apiKey;
}
