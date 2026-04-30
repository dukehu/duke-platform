package com.duke.transformer.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * Transformer 超参数配置
 * 默认值设定为演示规模（而非生产规模）
 */
@Configuration
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransformerConfig {

    /**
     * 词表大小（词级）
     */
    private int vocabSize = 500;

    /**
     * 嵌入维度（d_model）
     */
    private int embeddingDim = 16;

    /**
     * 注意力头数
     */
    private int numHeads = 2;

    /**
     * Encoder 层数
     */
    private int numLayers = 1;

    /**
     * 前馈网络隐层维度
     */
    private int dFf = 32;

    /**
     * 最大序列长度
     */
    private int maxSeqLen = 8;

    /**
     * 随机数种子（权重初始化）
     */
    private int randomSeed = 42;

    /**
     * LayerNorm epsilon（数值稳定性）
     */
    private double epsilon = 1e-6;

    /**
     * 获取注意力头的维度（d_k = d_model / num_heads）
     */
    public int getDk() {
        return embeddingDim / numHeads;
    }

    /**
     * 验证配置有效性
     */
    public void validate() {
        if (embeddingDim % numHeads != 0) {
            throw new IllegalArgumentException(
                    "嵌入维度 " + embeddingDim + " 必须能被注意力头数 " + numHeads + " 整除"
            );
        }
        if (dFf <= 0 || embeddingDim <= 0 || numHeads <= 0 || numLayers <= 0) {
            throw new IllegalArgumentException("所有维度参数必须大于 0");
        }
        // 验证支持的参数组合
        if (!isValidEmbeddingDim(embeddingDim)) {
            throw new IllegalArgumentException(
                    "不支持的嵌入维度 " + embeddingDim + "，支持的值：8, 16, 32"
            );
        }
        if (!isValidNumHeads(numHeads)) {
            throw new IllegalArgumentException(
                    "不支持的注意力头数 " + numHeads + "，支持的值：1, 2, 4"
            );
        }
        if (!isValidNumLayers(numLayers)) {
            throw new IllegalArgumentException(
                    "不支持的 Encoder 层数 " + numLayers + "，支持的值：1, 2"
            );
        }
    }

    private boolean isValidEmbeddingDim(int dim) {
        return dim == 4 || dim == 8 || dim == 16 || dim == 32;
    }

    private boolean isValidNumHeads(int heads) {
        return heads == 1 || heads == 2 || heads == 4;
    }

    private boolean isValidNumLayers(int layers) {
        return layers == 1 || layers == 2;
    }

    /**
     * 创建一个带有覆盖参数的副本
     */
    public TransformerConfig withOverrides(
            Integer newEmbeddingDim, Integer newNumHeads, Integer newNumLayers
    ) {
        TransformerConfig copy = new TransformerConfig(
                this.vocabSize,
                newEmbeddingDim != null ? newEmbeddingDim : this.embeddingDim,
                newNumHeads != null ? newNumHeads : this.numHeads,
                newNumLayers != null ? newNumLayers : this.numLayers,
                (newEmbeddingDim != null ? newEmbeddingDim : this.embeddingDim) * 2,  // dFf = 2 * embedding_dim
                this.maxSeqLen,
                this.randomSeed,
                this.epsilon
        );
        copy.validate();
        return copy;
    }

    @Override
    public String toString() {
        return "TransformerConfig{" +
                "vocabSize=" + vocabSize +
                ", embeddingDim=" + embeddingDim +
                ", numHeads=" + numHeads +
                ", dk=" + getDk() +
                ", numLayers=" + numLayers +
                ", dFf=" + dFf +
                ", maxSeqLen=" + maxSeqLen +
                ", randomSeed=" + randomSeed +
                '}';
    }
}
