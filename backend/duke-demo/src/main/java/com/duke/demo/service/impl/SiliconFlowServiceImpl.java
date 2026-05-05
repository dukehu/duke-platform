package com.demo.service.impl;

import com.demo.config.SiliconFlowProperties;
import com.demo.service.SiliconFlowService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class SiliconFlowServiceImpl implements SiliconFlowService {

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final SiliconFlowProperties props;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SiliconFlowServiceImpl(SiliconFlowProperties props) {
        this.props = props;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String chat(String userPrompt) {
        return chat(null, userPrompt);
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", props.getModel());
            body.put("temperature", 0.7);
            body.put("max_tokens", 1024);

            ArrayNode messages = body.putArray("messages");

            if (systemPrompt != null && !systemPrompt.isBlank()) {
                ObjectNode sys = messages.addObject();
                sys.put("role", "system");
                sys.put("content", systemPrompt);
            }

            ObjectNode user = messages.addObject();
            user.put("role", "user");
            user.put("content", userPrompt);

            Request request = new Request.Builder()
                    .url(props.getBaseUrl())
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(objectMapper.writeValueAsString(body), JSON_TYPE))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "no body";
                    throw new RuntimeException("API 请求失败 HTTP " + response.code() + ": " + errorBody);
                }
                JsonNode result = objectMapper.readTree(response.body().string());
                return result.path("choices").get(0).path("message").path("content").asText();
            }
        } catch (IOException e) {
            throw new RuntimeException("调用硅基流动 API 异常: " + e.getMessage(), e);
        }
    }

    @Override
    public String getModel() {
        return props.getModel();
    }
}
