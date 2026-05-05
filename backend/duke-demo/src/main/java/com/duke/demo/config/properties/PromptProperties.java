package com.duke.demo.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "prompt")
public class PromptProperties {
    private String storageDir = "prompts";
}
