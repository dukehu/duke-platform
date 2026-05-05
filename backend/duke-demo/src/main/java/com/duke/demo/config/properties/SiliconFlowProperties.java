package com.duke.demo.config.properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "silicon-flow")
public class SiliconFlowProperties {

    // api文档：https://docs.siliconflow.cn/cn/api-reference/chat-completions/chat-completions

    // java 代码
     private String apiUrl4j = "https://api.siliconflow.cn/v1/chat/completions";
    // LangChain4j
    private String apiUrl4LangChain = "https://api.siliconflow.cn/v1";
    private String apiKey = "sk-xzxfednykotdhevrxuqpyrzjsfksdmjtdtwkojqopvoycguw";
    private String defaultModel = "Qwen/Qwen2.5-7B-Instruct";
}
