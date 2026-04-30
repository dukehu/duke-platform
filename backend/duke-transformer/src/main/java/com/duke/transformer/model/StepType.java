package com.duke.transformer.model;

/**
 * Transformer 计算步骤类型枚举
 * 从分词到输出投影的所有关键计算步骤
 */
public enum StepType {
    // 基础阶段
    TOKENIZATION("分词"),
    TOKEN_EMBEDDING("Token 嵌入"),
    POSITIONAL_ENCODING("位置编码"),

    // 注意力阶段
    QKV_PROJECTION("Q/K/V 线性投影"),
    ATTENTION_SCORES("注意力得分（点积）"),
    ATTENTION_SCALE("注意力缩放（÷√dk）"),
    ATTENTION_SOFTMAX("注意力权重（Softmax）"),
    ATTENTION_WEIGHTED_SUM("加权求和（权重×V）"),
    MULTI_HEAD_CONCAT("多头拼接 + 输出投影"),

    // 层处理阶段
    ADD_NORM_1("残差连接 + 层归一化 1"),
    FFN_LINEAR1("前馈网络第一层（全连接）"),
    FFN_RELU("ReLU 激活"),
    FFN_LINEAR2("前馈网络第二层（全连接）"),
    ADD_NORM_2("残差连接 + 层归一化 2"),

    // 输出阶段
    OUTPUT_PROJECTION("输出投影（预测概率）"),

    // Decoder 阶段
    DECODER_EMBEDDING("Decoder 嵌入 + 位置编码"),
    DECODER_MASKED_SELF_ATTN("Decoder Masked Self-Attention 得分"),
    DECODER_MASKED_SOFTMAX("Decoder Masked Attention 权重"),
    DECODER_MASKED_WEIGHTED_SUM("Decoder Masked Attention 加权求和"),
    DECODER_ADD_NORM_1("Decoder 残差 + 层归一化 1"),
    DECODER_CROSS_ATTN_SCORES("Decoder Cross-Attention 得分"),
    DECODER_CROSS_ATTN_SOFTMAX("Decoder Cross-Attention 权重"),
    DECODER_CROSS_ATTN_WEIGHTED("Decoder Cross-Attention 加权求和"),
    DECODER_ADD_NORM_2("Decoder 残差 + 层归一化 2"),
    DECODER_FFN("Decoder 前馈网络"),
    DECODER_ADD_NORM_3("Decoder 残差 + 层归一化 3"),
    DECODER_OUTPUT_LOGITS("Decoder 输出 Logits"),
    DECODER_SOFTMAX_PROBS("Decoder Softmax 概率"),

    // 自回归生成阶段
    TOKEN_GENERATED("生成 Token"),

    // 完成标志
    COMPLETE("计算完成");

    private final String description;

    StepType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
