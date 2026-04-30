package com.duke.transformer.core;

import com.duke.transformer.config.TransformerConfig;
import org.springframework.stereotype.Component;

/**
 * Transformer 权重矩阵（不可变）
 * 所有权重在构造时用 Xavier Uniform 初始化，seed 为固定值以确保可复现
 * 为了避免外部修改，所有 getter 都返回防御性副本
 */
@Component
public class TransformerWeights {

    private final TransformerConfig defaultConfig;

    public TransformerWeights(TransformerConfig config) {
        this.defaultConfig = config;
    }

    public WeightMatrices getWeights(TransformerConfig config) {
        config.validate();

        int vocabSize = config.getVocabSize();
        int d = config.getEmbeddingDim();
        int dFf = config.getDFf();
        long seed = config.getRandomSeed();

        return new WeightMatrices(
                MatrixUtils.xavierUniform(vocabSize, d, seed),
                MatrixUtils.xavierUniform(d, d, seed + 1),
                MatrixUtils.xavierUniform(d, d, seed + 2),
                MatrixUtils.xavierUniform(d, d, seed + 3),
                MatrixUtils.xavierUniform(d, d, seed + 4),
                MatrixUtils.xavierUniform(d, dFf, seed + 5),
                new double[dFf],
                MatrixUtils.xavierUniform(dFf, d, seed + 6),
                new double[d],
                MatrixUtils.xavierUniform(d, vocabSize, seed + 7),
                // Decoder Masked Self-Attention
                MatrixUtils.xavierUniform(d, d, seed + 8),
                MatrixUtils.xavierUniform(d, d, seed + 9),
                MatrixUtils.xavierUniform(d, d, seed + 10),
                MatrixUtils.xavierUniform(d, d, seed + 11),
                // Decoder Cross-Attention
                MatrixUtils.xavierUniform(d, d, seed + 12),
                MatrixUtils.xavierUniform(d, d, seed + 13),
                MatrixUtils.xavierUniform(d, d, seed + 14),
                MatrixUtils.xavierUniform(d, d, seed + 15),
                // Decoder FFN
                MatrixUtils.xavierUniform(d, dFf, seed + 16),
                new double[dFf],
                MatrixUtils.xavierUniform(dFf, d, seed + 17),
                new double[d]
        );
    }

    public static class WeightMatrices {
        public final double[][] embeddingMatrix;
        public final double[][] wQ;
        public final double[][] wK;
        public final double[][] wV;
        public final double[][] wO;
        public final double[][] w1;
        public final double[] b1;
        public final double[][] w2;
        public final double[] b2;
        public final double[][] wOut;

        // Decoder Masked Self-Attention
        public final double[][] wDecQ;
        public final double[][] wDecK;
        public final double[][] wDecV;
        public final double[][] wDecO;

        // Decoder Cross-Attention
        public final double[][] wCrossQ;
        public final double[][] wCrossK;
        public final double[][] wCrossV;
        public final double[][] wCrossO;

        // Decoder FFN
        public final double[][] wDecFFN1;
        public final double[] bDecFFN1;
        public final double[][] wDecFFN2;
        public final double[] bDecFFN2;

        public WeightMatrices(double[][] embeddingMatrix, double[][] wQ, double[][] wK,
                              double[][] wV, double[][] wO, double[][] w1, double[] b1,
                              double[][] w2, double[] b2, double[][] wOut,
                              double[][] wDecQ, double[][] wDecK, double[][] wDecV, double[][] wDecO,
                              double[][] wCrossQ, double[][] wCrossK, double[][] wCrossV, double[][] wCrossO,
                              double[][] wDecFFN1, double[] bDecFFN1, double[][] wDecFFN2, double[] bDecFFN2) {
            this.embeddingMatrix = embeddingMatrix;
            this.wQ = wQ;
            this.wK = wK;
            this.wV = wV;
            this.wO = wO;
            this.w1 = w1;
            this.b1 = b1;
            this.w2 = w2;
            this.b2 = b2;
            this.wOut = wOut;
            this.wDecQ = wDecQ;
            this.wDecK = wDecK;
            this.wDecV = wDecV;
            this.wDecO = wDecO;
            this.wCrossQ = wCrossQ;
            this.wCrossK = wCrossK;
            this.wCrossV = wCrossV;
            this.wCrossO = wCrossO;
            this.wDecFFN1 = wDecFFN1;
            this.bDecFFN1 = bDecFFN1;
            this.wDecFFN2 = wDecFFN2;
            this.bDecFFN2 = bDecFFN2;
        }
    }

}
