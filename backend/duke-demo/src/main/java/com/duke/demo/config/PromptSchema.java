package com.duke.demo.config;

import java.util.Map;
import java.util.Set;

// 定义每种 Prompt 的期望字段和枚举值
public enum PromptSchema {

    CODE_REVIEW(
            Set.of("issue", "severity", "suggestion"),
            Map.of("severity", Set.of("HIGH", "MEDIUM", "LOW"))
    ),
    RAG_QA(
            Set.of("answer", "confidence"),
            Map.of("confidence", Set.of("HIGH", "MEDIUM", "LOW"))
    ),
    SQL_REVIEW(
            Set.of("issue", "severity", "category", "suggestion"),
            Map.of("severity", Set.of("HIGH", "MEDIUM", "LOW"),
                    "category", Set.of("安全", "性能", "规范"))
    ),
    UNKNOWN(Set.of(), Map.of());

    private final Set<String> requiredFields;
    private final Map<String, Set<String>> enumFields;

    PromptSchema(Set<String> requiredFields, Map<String, Set<String>> enumFields) {
        this.requiredFields = requiredFields;
        this.enumFields = enumFields;
    }

    public static PromptSchema of(String promptName) {
        if (promptName == null) return UNKNOWN;
        return switch (promptName.toLowerCase()) {
            case "code_review" -> CODE_REVIEW;
            case "rag_qa"      -> RAG_QA;
            case "sql_review"  -> SQL_REVIEW;
            default            -> UNKNOWN;
        };
    }

    public Set<String> requiredFields() {
        return requiredFields;
    }

    public Map<String, Set<String>> enumFields() {
        return enumFields;
    }
}
