package com.duke.demo.util;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.duke.demo.config.properties.EmbeddingProperties;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * qwen3-embedding-8b 向量生成工具
 */
@Component
@AllArgsConstructor
public class QwenEmbeddingUtil {

    // Embedding 配置类
    private final EmbeddingProperties embeddingProperties;

    /**
     * 文本生成向量（qwen3-embedding-8b 默认输出 1024 维）
     */
    public List<Float> textToVector(String text) {
        // 构造请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", embeddingProperties.getModel());
        requestBody.put("input", new String[]{text});
//        requestBody.put("dimensions", 1024);

        // 发送请求
        String response = HttpRequest.post(embeddingProperties.getEndpoint())
                .header("Authorization", "Bearer " + embeddingProperties.getApiKey())
                .header("Content-Type", "application/json")
                .body(requestBody.toJSONString())
                .execute()
                .body();

        // 解析向量
        JSONObject respJson = JSON.parseObject(response);
        JSONArray embeddings = respJson.getJSONArray("data");
        return embeddings.getJSONObject(0)
                .getJSONArray("embedding")
                .toList(Float.class);
    }
}
