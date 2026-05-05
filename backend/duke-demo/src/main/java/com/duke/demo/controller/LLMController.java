package com.duke.demo.controller;

import com.duke.demo.config.properties.SiliconFlowProperties;
import com.duke.demo.entity.ChatHistoryStore;
import com.duke.framework.common.Result;
import com.duke.framework.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.alibaba.nacos.common.http.param.MediaType.APPLICATION_JSON;

@Slf4j
@Tag(name = "大模型测试")
@RestController
@RequestMapping("/llm")
@RequiredArgsConstructor
public class LLMController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final SiliconFlowProperties siliconFlowProperties;

    private final StreamingChatLanguageModel streamingChatModel;

    private final ChatHistoryStore historyStore;

    @PostMapping("/chat")
    public Result<JsonNode> chat(@RequestBody ChatRequest req) {
        Map<String, Object> reqParams = new HashMap<>();
        reqParams.put("model", siliconFlowProperties.getDefaultModel());
        reqParams.put("max_tokens", req.getMaxTokens() > 0 ? req.getMaxTokens() : 512);
        reqParams.put("temperature", req.getTemperature());
        reqParams.put("messages", req.getMessages());


        try {
            // 发送请求
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(siliconFlowProperties.getApiUrl4j())
                    .addHeader("Authorization", "Bearer " + siliconFlowProperties.getApiKey())
                    .addHeader("Content-Type", APPLICATION_JSON)
                    .post(okhttp3.RequestBody.create(objectMapper.writeValueAsString(reqParams), okhttp3.MediaType.get("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                assert response.body() != null;
                return Result.success(objectMapper.readTree(response.body().string()));
            }
        } catch (IOException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    // LLMChatStream01
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest req) {
        SseEmitter emitter = new SseEmitter(60_000L);

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                // 1. 构建请求参数
                Map<String, Object> reqParams = new HashMap<>();
                reqParams.put("model", siliconFlowProperties.getDefaultModel());
                reqParams.put("max_tokens", req.getMaxTokens() > 0 ? req.getMaxTokens() : 512);
                reqParams.put("temperature", req.getTemperature());
                reqParams.put("stream", true);
                reqParams.put("messages", req.getMessages());

                // 2. 创建客户端和请求
                OkHttpClient client = new OkHttpClient.Builder()
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build();

                Request request = new Request.Builder()
                        .url(siliconFlowProperties.getApiUrl4j())
                        .addHeader("Authorization", "Bearer " + siliconFlowProperties.getApiKey())
                        .addHeader("Content-Type", "application/json")
                        .post(okhttp3.RequestBody.create(
                                objectMapper.writeValueAsString(reqParams),
                                okhttp3.MediaType.get("application/json")
                        ))
                        .build();

                // 3. 执行请求并处理流式响应
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        emitter.completeWithError(new RuntimeException("请求失败，状态码：" + response.code()));
                        return;
                    }

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8)
                    );

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 只处理以 "data:" 开头的行，其他全部跳过
                        if (!line.startsWith("data:")) {
                            continue;
                        }

                        // 去掉 "data:" 前缀并去除空格
                        String data = line.substring(5).trim();

                        // 遇到结束标记，退出循环
                        if ("[DONE]".equals(data)) {
                            break;
                        }

                        try {
                            // 解析 JSON
                            JsonNode root = objectMapper.readTree(data);
                            JsonNode choices = root.path("choices");
                            if (choices.isArray() && !choices.isEmpty()) {
                                JsonNode delta = choices.get(0).path("delta");
                                // 只提取 content 字段
                                String content = delta.path("content").asText("");
                                if (!content.isEmpty()) {
                                    emitter.send(SseEmitter.event().data(content));
                                }
                            }
                        } catch (Exception e) {
                            // JSON 解析失败的行直接跳过，不影响后续流程
                            continue;
                        }
                    }

                    emitter.complete();
                }

            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    // LLMChatStream02
    @GetMapping(value = "/chat/stream/langchain4j", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String sessionId) {

        SseEmitter emitter = new SseEmitter(60_000L);

        // 1. 把用户消息加入历史
        historyStore.addUserMessage(sessionId, message);
        historyStore.trim(sessionId);

        // 2. 用一个 StringBuilder 收集完整回复，用于存历史
        StringBuilder fullReply = new StringBuilder();

        // 3. 调用流式模型
        streamingChatModel.generate(
                historyStore.getHistory(sessionId),
                new StreamingResponseHandler<AiMessage>() {

                    @Override
                    public void onNext(String token) {
                        // 每来一个 token 就推给前端
                        try {
                            fullReply.append(token);
                            emitter.send(SseEmitter.event().data(token));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onComplete(dev.langchain4j.model.output.Response<AiMessage> response) {
                        // 生成完毕，把完整回复存入历史
                        historyStore.addAiMessage(sessionId, fullReply.toString());
                        try {
                            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                        } catch (IOException e) {
                            // ignore
                        }
                        emitter.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        emitter.completeWithError(error);
                    }
                }
        );

        return emitter;
    }

    // 清空某个会话的历史
    @DeleteMapping("/history/{sessionId}")
    public Result<String> clearHistory(@PathVariable String sessionId) {
        historyStore.clear(sessionId);
        return Result.success("已清空");
    }

    // Message.java —— 对应 { role, content }
    @Data
    @AllArgsConstructor
    static class Message {
        public String role;    // "user" | "assistant" | "system"
        public String content;
    }

    // {
    //     "temperature": 0.7,
    //     "maxTokens": 512,
    //     "messages": [
    //         {
    //             "role": "system",
    //             "content": "你是一个诗人，只用古诗体回答"
    //         },
    //         {
    //             "role": "user",
    //             "content": "介绍一下春天"
    //         }
    //     ]
    // }
    @Data
    @Builder
    static class ChatRequest {
        public String model;
        public int maxTokens;
        public double temperature;
        public List<Message> messages;
    }

}
