package com.duke.knowledgeqa.entity;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatHistoryStore {

    // 用 sessionId 隔离不同用户的对话历史
    private final Map<String, List<ChatMessage>> store = new ConcurrentHashMap<>();

    public List<ChatMessage> getHistory(String sessionId) {
        return store.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }

    public void addUserMessage(String sessionId, String content) {
        getHistory(sessionId).add(new UserMessage(content));
    }

    public void addAiMessage(String sessionId, String content) {
        getHistory(sessionId).add(new AiMessage(content));
    }

    // 只保留最近 10 轮，防止 token 爆炸
    public void trim(String sessionId) {
        List<ChatMessage> history = getHistory(sessionId);
        if (history.size() > 20) {
            history.subList(0, history.size() - 20).clear();
        }
    }

    public void clear(String sessionId) {
        store.remove(sessionId);
    }
}