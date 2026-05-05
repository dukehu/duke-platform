package com.duke.knowledgeqa.config;

import com.duke.knowledgeqa.config.properties.SiliconFlowProperties;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class LangChainConfig {

    private final SiliconFlowProperties siliconFlowProperties;
    @Bean
    public StreamingChatLanguageModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(siliconFlowProperties.getApiKey())
                .baseUrl(siliconFlowProperties.getApiUrl())
                .modelName(siliconFlowProperties.getDefaultModel())
                .temperature(1.0)
                .build();
    }
}
