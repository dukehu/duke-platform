package com.duke.transformer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Transformer 计算步骤的日志数据结构
 * 包含该步骤的数学说明、所有中间矩阵、元数据信息
 * 通过 SSE 推送给前端逐步渲染
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformerStep {

    /**
     * 步骤类型枚举
     */
    private StepType type;

    /**
     * 步骤标题，例如 "Step 3.2 - 注意力得分（Head 0）"
     */
    private String title;

    /**
     * 数学公式和中文说明，支持 Markdown 格式
     */
    private String description;

    /**
     * 通俗解释（生活类比），纯中文白话，给初学者读
     */
    private String analogy;

    /**
     * 数据流向说明，两行文字：输入来自哪里 / 输出传给哪里
     */
    private String dataFlow;

    /**
     * 所属的注意力头，"head_0" / "head_1" / null（表示非分头操作）
     */
    private String headIndex;

    /**
     * 该步骤产生的所有矩阵数据（可能有多个，如"变换前"和"变换后"）
     */
    private List<MatrixData> matrices;

    /**
     * 非矩阵的辅助信息，如 tokenIds、tokenChars、均值/方差等
     */
    private Map<String, Object> metadata;

    /**
     * 时间戳（毫秒），用于前端日志排序
     */
    private long timestamp;

    /**
     * 矩阵数据包装类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatrixData {

        /**
         * 矩阵名称标签，例如 "Q_full", "scores_head0", "attention_weights_head1"
         */
        private String label;

        /**
         * 矩阵的实际数值，二维数组
         */
        private double[][] values;

        /**
         * 矩阵中的最小值（用于前端热力图颜色映射的归一化）
         */
        private double minVal;

        /**
         * 矩阵中的最大值
         */
        private double maxVal;

        /**
         * 矩阵的行标签，例如 token 字符 ["h", "e", "l", "l", "o"]
         */
        private List<String> rowLabels;

        /**
         * 矩阵的列标签，例如维度索引 ["d0", "d1", ...] 或 token 名（注意力矩阵）
         */
        private List<String> colLabels;

        /**
         * 热力图配色方案
         * "blue"：白→蓝，用于 embedding 和隐层
         * "red"：白→深红，用于注意力权重（值域 [0,1]）
         * "diverging"：蓝→白→红，用于含正负值的矩阵（权重矩阵）
         */
        private String colorScheme;
    }
}
