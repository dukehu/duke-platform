package com.duke.transformer.service;

import com.duke.transformer.config.TransformerConfig;
import com.duke.transformer.core.MatrixUtils;
import com.duke.transformer.core.Tokenizer;
import com.duke.transformer.core.TransformerWeights;
import com.duke.transformer.model.StepType;
import com.duke.transformer.model.TransformerStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Transformer 完整计算流程服务
 * 从输入文本到最终输出，逐步生成每个计算步骤的日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransformerService {

    private final TransformerConfig defaultConfig;
    private final TransformerWeights weights;
    private final Tokenizer tokenizer;

    /**
     * 从已分词的 token 数组执行 Transformer 计算（流式推送版本）
     * 用于 SSE 流式推送场景，前端已经分词
     */
    public void computeTransformerStepsStreaming(int[] tokenIds, TransformerConfig config,
                                                 java.util.function.Consumer<TransformerStep> stepConsumer) {
        if (config == null) {
            config = defaultConfig;
        }
        config.validate();

        try {
            TransformerWeights.WeightMatrices w = weights.getWeights(config);

            // Step 1: Token Embedding
            double[][] X = embedTokens(tokenIds, w.embeddingMatrix);
            stepConsumer.accept(stepTokenEmbedding(tokenIds, X));

            // Step 2: 位置编码
            double[][] PE = computePositionalEncoding(tokenIds.length, config.getEmbeddingDim());
            double[][] Xpos = MatrixUtils.add(X, PE);
            stepConsumer.accept(stepPositionalEncoding(X, PE, Xpos));

            // Step 3: Q/K/V 投影
            double[][] Q = MatrixUtils.multiply(Xpos, w.wQ);
            double[][] K = MatrixUtils.multiply(Xpos, w.wK);
            double[][] V = MatrixUtils.multiply(Xpos, w.wV);
            stepConsumer.accept(stepQKVProjection(Q, K, V));

            // Step 4-8: 多头注意力细节
            List<TransformerStep> attentionSteps = stepMultiHeadAttention(
                    Xpos, Q, K, V, tokenIds, config, w);
            for (TransformerStep step : attentionSteps) {
                stepConsumer.accept(step);
            }
            double[][] attnOutput = (double[][]) attentionSteps.get(attentionSteps.size() - 1)
                    .getMetadata().get("attention_output");

            // Step 9: 残差 + LayerNorm 1
            double[][] residual1 = MatrixUtils.add(Xpos, attnOutput);
            double[][] ln1Output = MatrixUtils.layerNorm(residual1, config.getEpsilon());
            stepConsumer.accept(stepAddNorm1(residual1, ln1Output, buildTokenLabels(tokenIds)));

            // Step 10: FFN 第一层
            double[][] ffn1 = MatrixUtils.multiply(ln1Output, w.w1);
            ffn1 = MatrixUtils.addBias(ffn1, w.b1);
            stepConsumer.accept(stepFFNLinear1(ffn1));

            // Step 11: ReLU 激活
            double[][] ffnRelu = MatrixUtils.relu(ffn1);
            stepConsumer.accept(stepFFNReLU(ffnRelu));

            // Step 12: FFN 第二层
            double[][] ffnOutput = MatrixUtils.multiply(ffnRelu, w.w2);
            ffnOutput = MatrixUtils.addBias(ffnOutput, w.b2);
            stepConsumer.accept(stepFFNLinear2(ffnOutput));

            // Step 13: 残差 + LayerNorm 2
            double[][] residual2 = MatrixUtils.add(ln1Output, ffnOutput);
            double[][] finalOutput = MatrixUtils.layerNorm(residual2, config.getEpsilon());
            stepConsumer.accept(stepAddNorm2(residual2, finalOutput, buildTokenLabels(tokenIds)));

            // Step 14: 输出投影
            double[][] logits = MatrixUtils.multiply(finalOutput, w.wOut);
            stepConsumer.accept(stepOutputProjection(logits, tokenIds));
        } catch (Exception e) {
            log.error("Transformer 计算出错", e);
            throw new RuntimeException("Transformer 计算失败: " + e.getMessage(), e);
        }
    }

    // ==================== 各步骤实现 ====================

    /**
     * Step 1: Token Embedding
     */
    private TransformerStep stepTokenEmbedding(int[] tokenIds, double[][] X) {
        List<String> rowLabels = new ArrayList<>();
        for (int id : tokenIds) {
            rowLabels.add(String.valueOf(tokenizer.getWord(id)));
        }
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < X[0].length; i++) {
            colLabels.add("d" + i);
        }

        double minVal = MatrixUtils.minVal(X);
        double maxVal = MatrixUtils.maxVal(X);

        TransformerStep.MatrixData matrixData = TransformerStep.MatrixData.builder()
                .label("X (Token Embeddings)")
                .values(X)
                .minVal(minVal)
                .maxVal(maxVal)
                .rowLabels(rowLabels)
                .colLabels(colLabels)
                .colorScheme("blue")
                .build();

        return TransformerStep.builder()
                .type(StepType.TOKEN_EMBEDDING)
                .title("Step 1 - Token 嵌入（Embedding）")
                .description("从嵌入矩阵 E[65×16] 中查表，获取每个 token 的密集向量表示")
                .analogy("就像查字典：把每个词换成一排数字代表它的含义。")
                .dataFlow("输入：token ID 序列（整数）\n输出：X —— 每个 token 的向量 [seq × d_model]")
                .matrices(List.of(matrixData))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Step 2: 位置编码
     */
    private TransformerStep stepPositionalEncoding(double[][] X, double[][] PE, double[][] Xpos) {
        List<String> rowLabels = new ArrayList<>();
        for (int i = 0; i < X.length; i++) {
            rowLabels.add("pos" + i);
        }
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < X[0].length; i++) {
            colLabels.add("d" + i);
        }

        double minPE = MatrixUtils.minVal(PE);
        double maxPE = MatrixUtils.maxVal(PE);

        TransformerStep.MatrixData peMat = TransformerStep.MatrixData.builder()
                .label("PE (Positional Encoding)")
                .values(PE)
                .minVal(minPE)
                .maxVal(maxPE)
                .rowLabels(rowLabels)
                .colLabels(colLabels)
                .colorScheme("diverging")
                .build();

        double minXpos = MatrixUtils.minVal(Xpos);
        double maxXpos = MatrixUtils.maxVal(Xpos);

        TransformerStep.MatrixData xposMat = TransformerStep.MatrixData.builder()
                .label("X + PE (带位置信息的嵌入)")
                .values(Xpos)
                .minVal(minXpos)
                .maxVal(maxXpos)
                .rowLabels(rowLabels)
                .colLabels(colLabels)
                .colorScheme("blue")
                .build();

        return TransformerStep.builder()
                .type(StepType.POSITIONAL_ENCODING)
                .title("Step 2 - 位置编码（Positional Encoding）")
                .description("加入位置信息，防止 Transformer 对序列顺序无感知\n\n" +
                        "公式：PE(pos, 2i) = sin(pos/10000^(2i/d_model))\n" +
                        "PE(pos, 2i+1) = cos(pos/10000^(2i/d_model))")
                .analogy("给每个词盖上座位号印章，让模型知道词的先后顺序。")
                .dataFlow("输入：X（词向量）\n输出：X+PE —— 注入位置信息后的嵌入，传入 QKV 投影")
                .matrices(List.of(peMat, xposMat))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Step 3: Q/K/V 线性投影
     */
    private TransformerStep stepQKVProjection(double[][] Q, double[][] K, double[][] V) {
        List<String> rowLabels = new ArrayList<>();
        for (int i = 0; i < Q.length; i++) {
            rowLabels.add("token" + i);
        }
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < Q[0].length; i++) {
            colLabels.add("d" + i);
        }

        List<TransformerStep.MatrixData> matrices = new ArrayList<>();

        // Q 矩阵
        double minQ = MatrixUtils.minVal(Q);
        double maxQ = MatrixUtils.maxVal(Q);
        matrices.add(TransformerStep.MatrixData.builder()
                .label("Q (Query)")
                .values(Q)
                .minVal(minQ)
                .maxVal(maxQ)
                .rowLabels(rowLabels)
                .colLabels(colLabels)
                .colorScheme("diverging")
                .build());

        // K 矩阵
        double minK = MatrixUtils.minVal(K);
        double maxK = MatrixUtils.maxVal(K);
        matrices.add(TransformerStep.MatrixData.builder()
                .label("K (Key)")
                .values(K)
                .minVal(minK)
                .maxVal(maxK)
                .rowLabels(rowLabels)
                .colLabels(colLabels)
                .colorScheme("diverging")
                .build());

        // V 矩阵
        double minV = MatrixUtils.minVal(V);
        double maxV = MatrixUtils.maxVal(V);
        matrices.add(TransformerStep.MatrixData.builder()
                .label("V (Value)")
                .values(V)
                .minVal(minV)
                .maxVal(maxV)
                .rowLabels(rowLabels)
                .colLabels(colLabels)
                .colorScheme("diverging")
                .build());

        return TransformerStep.builder()
                .type(StepType.QKV_PROJECTION)
                .title("Step 3 - Q/K/V 线性投影")
                .description("将位置编码后的表示投影到 Q、K、V 三个子空间\n\n" +
                        "Q = X × W_Q，K = X × W_K，V = X × W_V\n" +
                        "其中 W_Q, W_K, W_V ∈ R^(d_model × d_model)")
                .analogy("像图书馆把你的请求拆成三份：Q=搜索词、K=书的标签、V=书的内容。")
                .dataFlow("输入：X+PE（带位置的嵌入）\n输出：Q、K、V 三个矩阵 —— 分别传入各注意力头")
                .matrices(matrices)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Step 4-8: 多头注意力详细步骤
     */
    private List<TransformerStep> stepMultiHeadAttention(
            double[][] Xpos, double[][] Q, double[][] K, double[][] V,
            int[] tokenIds, TransformerConfig config, TransformerWeights.WeightMatrices w) {

        List<TransformerStep> steps = new ArrayList<>();
        int numHeads = config.getNumHeads();
        int dk = config.getDk();

        List<String> tokenLabels = buildTokenLabels(tokenIds);
        List<String> colLabelsD = new ArrayList<>();
        for (int i = 0; i < dk; i++) {
            colLabelsD.add("d" + i);
        }

        // 计算每个 head 的注意力
        List<double[][]> headOutputs = new ArrayList<>();

        for (int h = 0; h < numHeads; h++) {
            String headName = "head_" + h;

            // 分头
            double[][] Qh = MatrixUtils.sliceCols(Q, h * dk, (h + 1) * dk);
            double[][] Kh = MatrixUtils.sliceCols(K, h * dk, (h + 1) * dk);
            double[][] Vh = MatrixUtils.sliceCols(V, h * dk, (h + 1) * dk);

            // 计算得分
            double[][] Kht = MatrixUtils.transpose(Kh);
            double[][] scores = MatrixUtils.multiply(Qh, Kht);

            // 缩放
            double scale = Math.sqrt(dk);
            double[][] scaledScores = MatrixUtils.scalarMultiply(scores, 1.0 / scale);

            // Softmax
            double[][] attentionWeights = MatrixUtils.softmax(scaledScores);

            // 加权求和
            double[][] headOutput = MatrixUtils.multiply(attentionWeights, Vh);
            headOutputs.add(headOutput);

            // 记录步骤
            double minScore = MatrixUtils.minVal(scores);
            double maxScore = MatrixUtils.maxVal(scores);

            // 4. 注意力得分
            steps.add(TransformerStep.builder()
                    .type(StepType.ATTENTION_SCORES)
                    .title("Step 4." + (h + 1) + " - 注意力得分（" + headName + "）")
                    .description("计算查询与键的相关度：scores = Q × K^T")
                    .analogy("每个词去问其他词：「你和我有多相关？」结果是一张相关度表格。")
                    .dataFlow("输入：Qh、Kh（当前头的切片）\n输出：scores —— 每对 token 之间的相关度，传入缩放")
                    .headIndex(headName)
                    .matrices(List.of(
                            TransformerStep.MatrixData.builder()
                                    .label("scores (Q × K^T)")
                                    .values(scores)
                                    .minVal(minScore)
                                    .maxVal(maxScore)
                                    .rowLabels(tokenLabels)
                                    .colLabels(tokenLabels)
                                    .colorScheme("diverging")
                                    .build()
                    ))
                    .timestamp(System.currentTimeMillis())
                    .build());

            // 5. 缩放
            double minScaled = MatrixUtils.minVal(scaledScores);
            double maxScaled = MatrixUtils.maxVal(scaledScores);
            steps.add(TransformerStep.builder()
                    .type(StepType.ATTENTION_SCALE)
                    .title("Step 5." + (h + 1) + " - 注意力缩放（" + headName + "）")
                    .description("缩放得分：scores / √d_k = scores / √" + dk)
                    .analogy("把分数收窄，防止某个词过于突出，让后续计算更稳定。")
                    .dataFlow("输入：scores\n输出：scaled_scores —— 除以 √dk 后传入 Softmax")
                    .headIndex(headName)
                    .matrices(List.of(
                            TransformerStep.MatrixData.builder()
                                    .label("scaled_scores")
                                    .values(scaledScores)
                                    .minVal(minScaled)
                                    .maxVal(maxScaled)
                                    .rowLabels(tokenLabels)
                                    .colLabels(tokenLabels)
                                    .colorScheme("diverging")
                                    .build()
                    ))
                    .timestamp(System.currentTimeMillis())
                    .build());

            // 6. Softmax（注意力权重）
            double minWeight = MatrixUtils.minVal(attentionWeights);
            double maxWeight = MatrixUtils.maxVal(attentionWeights);
            steps.add(TransformerStep.builder()
                    .type(StepType.ATTENTION_SOFTMAX)
                    .title("Step 6." + (h + 1) + " - 注意力权重（Softmax）（" + headName + "）")
                    .description("使用 Softmax 将缩放得分转换为概率分布\n\n" +
                            "每行的和 = 1.0，代表对每个位置的关注强度")
                    .analogy("把相关度变成注意力分配比例，像把 100% 的精力按比例分给每个词。")
                    .dataFlow("输入：scaled_scores\n输出：attention_weights —— 每行和为 1 的权重，传入加权求和")
                    .headIndex(headName)
                    .matrices(List.of(
                            TransformerStep.MatrixData.builder()
                                    .label("attention_weights")
                                    .values(attentionWeights)
                                    .minVal(minWeight)
                                    .maxVal(maxWeight)
                                    .rowLabels(tokenLabels)
                                    .colLabels(tokenLabels)
                                    .colorScheme("red")
                                    .build()
                    ))
                    .timestamp(System.currentTimeMillis())
                    .build());

            // 7. 加权求和
            double minOutput = MatrixUtils.minVal(headOutput);
            double maxOutput = MatrixUtils.maxVal(headOutput);
            steps.add(TransformerStep.builder()
                    .type(StepType.ATTENTION_WEIGHTED_SUM)
                    .title("Step 7." + (h + 1) + " - 加权求和（" + headName + "）")
                    .description("用注意力权重对 Value 加权求和\n\n" +
                            "output = attention_weights × V")
                    .analogy("按比例混合各词的信息，像调鸡尾酒按比例取原料。")
                    .dataFlow("输入：attention_weights、Vh\n输出：head_output —— 当前头的输出，传入多头拼接")
                    .headIndex(headName)
                    .matrices(List.of(
                            TransformerStep.MatrixData.builder()
                                    .label("head_output")
                                    .values(headOutput)
                                    .minVal(minOutput)
                                    .maxVal(maxOutput)
                                    .rowLabels(tokenLabels)
                                    .colLabels(colLabelsD)
                                    .colorScheme("blue")
                                    .build()
                    ))
                    .timestamp(System.currentTimeMillis())
                    .build());
        }

        // 8. 多头拼接 + W_O
        double[][] concat = headOutputs.get(0);
        for (int h = 1; h < headOutputs.size(); h++) {
            concat = MatrixUtils.horizontalConcat(concat, headOutputs.get(h));
        }

        double[][] multiHeadOutput = MatrixUtils.multiply(concat, w.wO);
        double minMH = MatrixUtils.minVal(multiHeadOutput);
        double maxMH = MatrixUtils.maxVal(multiHeadOutput);

        List<String> colLabelsFull = new ArrayList<>();
        for (int i = 0; i < multiHeadOutput[0].length; i++) {
            colLabelsFull.add("d" + i);
        }

        steps.add(TransformerStep.builder()
                .type(StepType.MULTI_HEAD_CONCAT)
                .title("Step 8 - 多头拼接 + 输出投影")
                .description("将所有 head 的输出拼接，然后通过 W_O 进行最终线性变换\n\n" +
                        "output = concat(head_0, head_1, ...) × W_O")
                .analogy("把多位专家的分析报告汇总，再综合提炼成一份统一结论。")
                .dataFlow("输入：所有注意力头的 head_output（共 numHeads 个矩阵）\n输出：multi_head_output —— 拼接并经 W_O 投影后的矩阵，传入残差连接 1")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("multi_head_output")
                                .values(multiHeadOutput)
                                .minVal(minMH)
                                .maxVal(maxMH)
                                .rowLabels(tokenLabels)
                                .colLabels(colLabelsFull)
                                .colorScheme("blue")
                                .build()
                ))
                .metadata(Map.of("attention_output", multiHeadOutput))
                .timestamp(System.currentTimeMillis())
                .build());

        return steps;
    }

    /**
     * Step 9: 残差 + LayerNorm 1
     */
    private TransformerStep stepAddNorm1(double[][] residual, double[][] output,
                                         List<String> tokenLabels) {
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < output[0].length; i++) {
            colLabels.add("d" + i);
        }

        double minResidual = MatrixUtils.minVal(residual);
        double maxResidual = MatrixUtils.maxVal(residual);
        double minOutput = MatrixUtils.minVal(output);
        double maxOutput = MatrixUtils.maxVal(output);

        List<TransformerStep.MatrixData> matrices = new ArrayList<>();
        matrices.add(TransformerStep.MatrixData.builder()
                .label("residual (X + attention_out)")
                .values(residual)
                .minVal(minResidual)
                .maxVal(maxResidual)
                .rowLabels(tokenLabels)
                .colLabels(colLabels)
                .colorScheme("blue")
                .build());

        matrices.add(TransformerStep.MatrixData.builder()
                .label("LayerNorm output")
                .values(output)
                .minVal(minOutput)
                .maxVal(maxOutput)
                .rowLabels(tokenLabels)
                .colLabels(colLabels)
                .colorScheme("blue")
                .build());

        return TransformerStep.builder()
                .type(StepType.ADD_NORM_1)
                .title("Step 9 - 残差连接 + 层归一化 1")
                .description("Add & Norm 第一个块\n\n" +
                        "1. 残差连接：output = X + attention_output\n" +
                        "2. 层归一化：LN(x) = (x - mean) / sqrt(var + eps)")
                .analogy("先加备忘录（不丢原始信息），再标准化分数（防止数值爆炸）。")
                .dataFlow("输入：X+PE（原始）+ multi_head_output（注意力结果）\n输出：LayerNorm 1 输出，传入 FFN")
                .matrices(matrices)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Step 10: FFN 第一层
     */
    private TransformerStep stepFFNLinear1(double[][] output) {
        List<String> rowLabels = new ArrayList<>();
        for (int i = 0; i < output.length; i++) {
            rowLabels.add("token" + i);
        }
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < output[0].length; i++) {
            colLabels.add("d" + i);
        }

        double minVal = MatrixUtils.minVal(output);
        double maxVal = MatrixUtils.maxVal(output);

        return TransformerStep.builder()
                .type(StepType.FFN_LINEAR1)
                .title("Step 10 - 前馈网络第一层（全连接）")
                .description("线性变换扩展到更高维\n\n" +
                        "x = input × W1 + b1\n" +
                        "维度：[seq × d_model] × [d_model × d_ff] → [seq × d_ff]")
                .analogy("先放大视野：把每个词的表示扩展 4 倍，给模型更大空间发现复杂模式。")
                .dataFlow("输入：LayerNorm 1 输出 [seq × d_model]\n输出：FFN 第一层 [seq × d_ff]，传入 ReLU")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("FFN_layer1 output")
                                .values(output)
                                .minVal(minVal)
                                .maxVal(maxVal)
                                .rowLabels(rowLabels)
                                .colLabels(colLabels)
                                .colorScheme("blue")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Step 11: ReLU 激活
     */
    private TransformerStep stepFFNReLU(double[][] output) {
        List<String> rowLabels = new ArrayList<>();
        for (int i = 0; i < output.length; i++) {
            rowLabels.add("token" + i);
        }
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < output[0].length; i++) {
            colLabels.add("d" + i);
        }

        double minVal = MatrixUtils.minVal(output);
        double maxVal = MatrixUtils.maxVal(output);

        return TransformerStep.builder()
                .type(StepType.FFN_RELU)
                .title("Step 11 - ReLU 激活")
                .description("非线性激活函数\n\n" +
                        "output = max(0, x)\n" +
                        "引入非线性性，增加模型表达能力")
                .analogy("过滤负面信号，只保留有意义的激活值，像大脑忽略无关噪音。")
                .dataFlow("输入：FFN 第一层（含负值）\n输出：ReLU 结果（负值清零），传入 FFN 第二层")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("ReLU output (负值被置为 0)")
                                .values(output)
                                .minVal(minVal)
                                .maxVal(maxVal)
                                .rowLabels(rowLabels)
                                .colLabels(colLabels)
                                .colorScheme("red")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Step 12: FFN 第二层
     */
    private TransformerStep stepFFNLinear2(double[][] output) {
        List<String> rowLabels = new ArrayList<>();
        for (int i = 0; i < output.length; i++) {
            rowLabels.add("token" + i);
        }
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < output[0].length; i++) {
            colLabels.add("d" + i);
        }

        double minVal = MatrixUtils.minVal(output);
        double maxVal = MatrixUtils.maxVal(output);

        return TransformerStep.builder()
                .type(StepType.FFN_LINEAR2)
                .title("Step 12 - 前馈网络第二层（全连接）")
                .description("线性变换投影回原始维度\n\n" +
                        "output = relu_out × W2 + b2\n" +
                        "维度：[seq × d_ff] × [d_ff × d_model] → [seq × d_model]")
                .analogy("再收缩整理：把放大过滤的信息压回原始维度，保持格式统一。")
                .dataFlow("输入：ReLU 结果 [seq × d_ff]\n输出：FFN 第二层 [seq × d_model]，传入残差连接 2")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("FFN_layer2 output")
                                .values(output)
                                .minVal(minVal)
                                .maxVal(maxVal)
                                .rowLabels(rowLabels)
                                .colLabels(colLabels)
                                .colorScheme("blue")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Step 13: 残差 + LayerNorm 2
     */
    private TransformerStep stepAddNorm2(double[][] residual, double[][] output,
                                         List<String> tokenLabels) {
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < output[0].length; i++) {
            colLabels.add("d" + i);
        }

        double minResidual = MatrixUtils.minVal(residual);
        double maxResidual = MatrixUtils.maxVal(residual);
        double minOutput = MatrixUtils.minVal(output);
        double maxOutput = MatrixUtils.maxVal(output);

        List<TransformerStep.MatrixData> matrices = new ArrayList<>();
        matrices.add(TransformerStep.MatrixData.builder()
                .label("residual (norm1_out + ffn_out)")
                .values(residual)
                .minVal(minResidual)
                .maxVal(maxResidual)
                .rowLabels(tokenLabels)
                .colLabels(colLabels)
                .colorScheme("blue")
                .build());

        matrices.add(TransformerStep.MatrixData.builder()
                .label("LayerNorm output")
                .values(output)
                .minVal(minOutput)
                .maxVal(maxOutput)
                .rowLabels(tokenLabels)
                .colLabels(colLabels)
                .colorScheme("blue")
                .build());

        return TransformerStep.builder()
                .type(StepType.ADD_NORM_2)
                .title("Step 13 - 残差连接 + 层归一化 2")
                .description("Add & Norm 第二个块\n\n" +
                        "1. 残差连接：output = norm1_out + ffn_out\n" +
                        "2. 层归一化：LN(x) = (x - mean) / sqrt(var + eps)")
                .analogy("再次加备忘录+标准化，确保 FFN 的结果和原信息融合且数值不跑偏。")
                .dataFlow("输入：LayerNorm 1 输出（残差来源）+ FFN 第二层输出\n输出：最终 Encoder 表示，传入输出投影")
                .matrices(matrices)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Step 14: 输出投影
     */
    private TransformerStep stepOutputProjection(double[][] logits, int[] tokenIds) {
        List<String> rowLabels = buildTokenLabels(tokenIds);
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < logits[0].length; i++) {
            colLabels.add("vocab_" + i);
        }

        return TransformerStep.builder()
                .type(StepType.OUTPUT_PROJECTION)
                .title("Step 14 - 输出投影（预测概率）")
                .description("将最后的表示映射到词表大小\n\n" +
                        "logits = hidden × W_out\n" +
                        "probs = softmax(logits)")
                .analogy("翻译成人话：把内部向量映射到词表，分数最高的词就是模型的预测。")
                .dataFlow("输入：最终 Encoder 表示 [seq × d_model]\n输出：logits —— 每个位置对词表的预测分数")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("logits (top-5 per position)")
                                .values(extractTop5(logits))
                                .minVal(0)
                                .maxVal(1)
                                .rowLabels(rowLabels)
                                .colLabels(List.of("top1", "top2", "top3", "top4", "top5"))
                                .colorScheme("red")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // ==================== 辅助方法 ====================

    /**
     * 查表获取 embeddings
     */
    private double[][] embedTokens(int[] tokenIds, double[][] embeddingMatrix) {
        double[][] embeddings = new double[tokenIds.length][];
        for (int i = 0; i < tokenIds.length; i++) {
            embeddings[i] = embeddingMatrix[tokenIds[i]];
        }
        return embeddings;
    }

    /**
     * 计算位置编码
     */
    private double[][] computePositionalEncoding(int seqLen, int dModel) {
        double[][] pe = new double[seqLen][dModel];
        for (int pos = 0; pos < seqLen; pos++) {
            for (int i = 0; i < dModel; i++) {
                double angle = pos / Math.pow(10000, 2.0 * i / dModel);
                if (i % 2 == 0) {
                    pe[pos][i] = Math.sin(angle);
                } else {
                    pe[pos][i] = Math.cos(angle);
                }
            }
        }
        return pe;
    }

    /**
     * 提取 top-5 概率（用于显示）
     */
    private double[][] extractTop5(double[][] logits) {
        double[][] result = new double[logits.length][Math.min(5, logits[0].length)];
        for (int i = 0; i < logits.length; i++) {
            // 对每一行排序，取前 5 个最大值
            double[] row = logits[i];  // 捕获当前行，避免lambda中的变量捕获问题
            List<Integer> indices = new ArrayList<>();
            for (int j = 0; j < row.length; j++) {
                indices.add(j);
            }
            indices.sort((a, b) -> Double.compare(row[b], row[a]));

            for (int j = 0; j < Math.min(5, indices.size()); j++) {
                result[i][j] = row[indices.get(j)];
            }
        }
        return result;
    }

    /**
     * 构建 token 标签（用于矩阵行标签）
     */
    private List<String> buildTokenLabels(int[] tokenIds) {
        List<String> labels = new ArrayList<>();
        for (int id : tokenIds) {
            labels.add(String.valueOf(tokenizer.getWord(id)));
        }
        return labels;
    }

    // ==================== Encoder 构建方法（共用于两个公开接口）====================

    /**
     * 构建并推送完整 Encoder 步骤
     * 返回 Encoder 最终输出（用于 Decoder 的 Cross-Attention）
     */
    private double[][] buildAndPushEncoderSteps(int[] tokenIds, TransformerConfig config,
                                                TransformerWeights.WeightMatrices w,
                                                java.util.function.Consumer<TransformerStep> stepConsumer) {
        // Step 1: Token Embedding
        double[][] X = embedTokens(tokenIds, w.embeddingMatrix);
        stepConsumer.accept(stepTokenEmbedding(tokenIds, X));

        // Step 2: 位置编码
        double[][] PE = computePositionalEncoding(tokenIds.length, config.getEmbeddingDim());
        double[][] Xpos = MatrixUtils.add(X, PE);
        stepConsumer.accept(stepPositionalEncoding(X, PE, Xpos));

        // Step 3: Q/K/V 投影
        double[][] Q = MatrixUtils.multiply(Xpos, w.wQ);
        double[][] K = MatrixUtils.multiply(Xpos, w.wK);
        double[][] V = MatrixUtils.multiply(Xpos, w.wV);
        stepConsumer.accept(stepQKVProjection(Q, K, V));

        // Step 4-8: 多头注意力细节
        List<TransformerStep> attentionSteps = stepMultiHeadAttention(
                Xpos, Q, K, V, tokenIds, config, w);
        for (TransformerStep step : attentionSteps) {
            stepConsumer.accept(step);
        }
        double[][] attnOutput = (double[][]) attentionSteps.get(attentionSteps.size() - 1)
                .getMetadata().get("attention_output");

        // Step 9: 残差 + LayerNorm 1
        double[][] residual1 = MatrixUtils.add(Xpos, attnOutput);
        double[][] ln1Output = MatrixUtils.layerNorm(residual1, config.getEpsilon());
        stepConsumer.accept(stepAddNorm1(residual1, ln1Output, buildTokenLabels(tokenIds)));

        // Step 10: FFN 第一层
        double[][] ffn1 = MatrixUtils.multiply(ln1Output, w.w1);
        ffn1 = MatrixUtils.addBias(ffn1, w.b1);
        stepConsumer.accept(stepFFNLinear1(ffn1));

        // Step 11: ReLU 激活
        double[][] ffnRelu = MatrixUtils.relu(ffn1);
        stepConsumer.accept(stepFFNReLU(ffnRelu));

        // Step 12: FFN 第二层
        double[][] ffnOutput = MatrixUtils.multiply(ffnRelu, w.w2);
        ffnOutput = MatrixUtils.addBias(ffnOutput, w.b2);
        stepConsumer.accept(stepFFNLinear2(ffnOutput));

        // Step 13: 残差 + LayerNorm 2（不推 OUTPUT_PROJECTION，Encoder-Decoder 模式无需）
        double[][] residual2 = MatrixUtils.add(ln1Output, ffnOutput);
        double[][] encoderOutput = MatrixUtils.layerNorm(residual2, config.getEpsilon());
        stepConsumer.accept(stepAddNorm2(residual2, encoderOutput, buildTokenLabels(tokenIds)));

        return encoderOutput;
    }

    // ==================== Decoder 计算方法 ====================

    /**
     * 完整 Encoder-Decoder 流程，推送完整 Encoder 步骤 + Decoder 步骤
     */
    public void computeEncoderDecoderStreaming(int[] srcTokenIds, int[] tgtTokenIds,
                                               TransformerConfig config,
                                               java.util.function.Consumer<TransformerStep> stepConsumer) {
        if (config == null) {
            config = defaultConfig;
        }
        config.validate();

        try {
            TransformerWeights.WeightMatrices w = weights.getWeights(config);

            // ===== Encoder 部分：推送所有 Encoder 步骤 =====
            double[][] encoderOutput = buildAndPushEncoderSteps(srcTokenIds, config, w, stepConsumer);

            // ===== Decoder 部分 =====
            // Step 1: Decoder 嵌入 + 位置编码
            double[][] decX = embedTokens(tgtTokenIds, w.embeddingMatrix);
            double[][] decPE = computePositionalEncoding(tgtTokenIds.length, config.getEmbeddingDim());
            double[][] decoderInput = MatrixUtils.add(decX, decPE);
            stepConsumer.accept(stepDecoderEmbedding(tgtTokenIds, decoderInput));

            // Step 2-4: Masked Self-Attention
            double[][] decQ = MatrixUtils.multiply(decoderInput, w.wDecQ);
            double[][] decK = MatrixUtils.multiply(decoderInput, w.wDecK);
            double[][] decV = MatrixUtils.multiply(decoderInput, w.wDecV);

            // 应用因果掩码
            double[][] causalMask = MatrixUtils.causalMask(tgtTokenIds.length);
            double[][] decScores = MatrixUtils.multiply(decQ, MatrixUtils.transpose(decK));
            decScores = MatrixUtils.scalarMultiply(decScores, 1.0 / Math.sqrt(config.getEmbeddingDim() / config.getNumHeads()));
            decScores = MatrixUtils.add(decScores, causalMask);

            stepConsumer.accept(stepDecoderMaskedAttentionScores(decScores, tgtTokenIds));

            double[][] decWeights = MatrixUtils.softmax(decScores);
            stepConsumer.accept(stepDecoderMaskedAttentionSoftmax(decWeights, tgtTokenIds));

            double[][] maskedAttnOutput = MatrixUtils.multiply(decWeights, decV);
            stepConsumer.accept(stepDecoderMaskedAttentionWeightedSum(maskedAttnOutput, tgtTokenIds));

            // Step 5: 残差 + LayerNorm 1
            double[][] decResidual1 = MatrixUtils.add(decoderInput, maskedAttnOutput);
            double[][] decLN1 = MatrixUtils.layerNorm(decResidual1, config.getEpsilon());
            stepConsumer.accept(stepDecoderAddNorm1(decResidual1, decLN1, buildTokenLabels(tgtTokenIds)));

            // Step 6-8: Cross-Attention（Q 来自 Decoder，K/V 来自 Encoder）
            double[][] crossQ = MatrixUtils.multiply(decLN1, w.wCrossQ);
            double[][] crossK = MatrixUtils.multiply(encoderOutput, w.wCrossK);
            double[][] crossV = MatrixUtils.multiply(encoderOutput, w.wCrossV);

            double[][] crossScores = MatrixUtils.multiply(crossQ, MatrixUtils.transpose(crossK));
            crossScores = MatrixUtils.scalarMultiply(crossScores, 1.0 / Math.sqrt(config.getEmbeddingDim() / config.getNumHeads()));

            stepConsumer.accept(stepDecoderCrossAttentionScores(crossScores, tgtTokenIds, srcTokenIds));

            double[][] crossWeights = MatrixUtils.softmax(crossScores);
            stepConsumer.accept(stepDecoderCrossAttentionSoftmax(crossWeights, tgtTokenIds, srcTokenIds));

            double[][] crossAttnOutput = MatrixUtils.multiply(crossWeights, crossV);
            stepConsumer.accept(stepDecoderCrossAttentionWeightedSum(crossAttnOutput, tgtTokenIds));

            // Step 9: 残差 + LayerNorm 2
            double[][] decResidual2 = MatrixUtils.add(decLN1, crossAttnOutput);
            double[][] decLN2 = MatrixUtils.layerNorm(decResidual2, config.getEpsilon());
            stepConsumer.accept(stepDecoderAddNorm2(decResidual2, decLN2, buildTokenLabels(tgtTokenIds)));

            // Step 10-11: Decoder FFN
            double[][] decFFN1 = MatrixUtils.multiply(decLN2, w.wDecFFN1);
            decFFN1 = MatrixUtils.addBias(decFFN1, w.bDecFFN1);
            double[][] decFFNRelu = MatrixUtils.relu(decFFN1);
            double[][] decFFNOutput = MatrixUtils.multiply(decFFNRelu, w.wDecFFN2);
            decFFNOutput = MatrixUtils.addBias(decFFNOutput, w.bDecFFN2);
            stepConsumer.accept(stepDecoderFFN(decFFNOutput, buildTokenLabels(tgtTokenIds)));

            // Step 12: 残差 + LayerNorm 3
            double[][] decResidual3 = MatrixUtils.add(decLN2, decFFNOutput);
            double[][] decFinalOutput = MatrixUtils.layerNorm(decResidual3, config.getEpsilon());
            stepConsumer.accept(stepDecoderAddNorm3(decResidual3, decFinalOutput, buildTokenLabels(tgtTokenIds)));

            // Step 13-14: 输出投影和 Softmax
            double[][] decLogits = MatrixUtils.multiply(decFinalOutput, w.wOut);
            stepConsumer.accept(stepDecoderOutputLogits(decLogits, tgtTokenIds));

            double[][] decProbs = MatrixUtils.softmax(decLogits);
            stepConsumer.accept(stepDecoderSoftmaxProbs(decProbs, tgtTokenIds));

        } catch (Exception e) {
            log.error("Encoder-Decoder 计算出错", e);
            throw new RuntimeException("Encoder-Decoder 计算失败: " + e.getMessage(), e);
        }
    }

    /**
     * 仅运行 Encoder，推送所有步骤，并返回 encoderOutput 矩阵
     * 用于前端分阶段调用：先运行 Encoder，后运行 Decoder
     */
    public double[][] computeEncoderOnlyStreaming(int[] tokenIds, TransformerConfig config,
                                                  java.util.function.Consumer<TransformerStep> stepConsumer) {
        if (config == null) {
            config = defaultConfig;
        }
        config.validate();

        try {
            TransformerWeights.WeightMatrices w = weights.getWeights(config);
            double[][] encoderOutput = buildAndPushEncoderSteps(tokenIds, config, w, stepConsumer);
            return encoderOutput;
        } catch (Exception e) {
            log.error("Encoder 计算出错", e);
            throw new RuntimeException("Encoder 计算失败: " + e.getMessage(), e);
        }
    }

    /**
     * 仅运行 Decoder，接收来自 Encoder 的输出矩阵，推送所有 Decoder 步骤
     * 用于前端分阶段调用：先运行 Encoder 获取 encoderOutput，再运行此方法
     */
    public void computeDecoderOnlyStreaming(int[] tgtTokenIds, double[][] encoderOutput,
                                            int[] srcTokenIds, TransformerConfig config,
                                            java.util.function.Consumer<TransformerStep> stepConsumer) {
        if (config == null) {
            config = defaultConfig;
        }
        config.validate();

        try {
            TransformerWeights.WeightMatrices w = weights.getWeights(config);

            // Decoder 嵌入 + 位置编码
            double[][] decX = embedTokens(tgtTokenIds, w.embeddingMatrix);
            double[][] decPE = computePositionalEncoding(tgtTokenIds.length, config.getEmbeddingDim());
            double[][] decoderInput = MatrixUtils.add(decX, decPE);
            stepConsumer.accept(stepDecoderEmbedding(tgtTokenIds, decoderInput));

            // Masked Self-Attention
            double[][] decQ = MatrixUtils.multiply(decoderInput, w.wDecQ);
            double[][] decK = MatrixUtils.multiply(decoderInput, w.wDecK);
            double[][] decV = MatrixUtils.multiply(decoderInput, w.wDecV);

            double[][] causalMask = MatrixUtils.causalMask(tgtTokenIds.length);
            double[][] decScores = MatrixUtils.multiply(decQ, MatrixUtils.transpose(decK));
            decScores = MatrixUtils.scalarMultiply(decScores, 1.0 / Math.sqrt(config.getEmbeddingDim() / config.getNumHeads()));
            decScores = MatrixUtils.add(decScores, causalMask);

            stepConsumer.accept(stepDecoderMaskedAttentionScores(decScores, tgtTokenIds));

            double[][] decWeights = MatrixUtils.softmax(decScores);
            stepConsumer.accept(stepDecoderMaskedAttentionSoftmax(decWeights, tgtTokenIds));

            double[][] maskedAttnOutput = MatrixUtils.multiply(decWeights, decV);
            stepConsumer.accept(stepDecoderMaskedAttentionWeightedSum(maskedAttnOutput, tgtTokenIds));

            // 残差 + LayerNorm 1
            double[][] decResidual1 = MatrixUtils.add(decoderInput, maskedAttnOutput);
            double[][] decLN1 = MatrixUtils.layerNorm(decResidual1, config.getEpsilon());
            stepConsumer.accept(stepDecoderAddNorm1(decResidual1, decLN1, buildTokenLabels(tgtTokenIds)));

            // Cross-Attention（Q 来自 Decoder，K/V 来自 Encoder）
            double[][] crossQ = MatrixUtils.multiply(decLN1, w.wCrossQ);
            double[][] crossK = MatrixUtils.multiply(encoderOutput, w.wCrossK);
            double[][] crossV = MatrixUtils.multiply(encoderOutput, w.wCrossV);

            double[][] crossScores = MatrixUtils.multiply(crossQ, MatrixUtils.transpose(crossK));
            crossScores = MatrixUtils.scalarMultiply(crossScores, 1.0 / Math.sqrt(config.getEmbeddingDim() / config.getNumHeads()));

            stepConsumer.accept(stepDecoderCrossAttentionScores(crossScores, tgtTokenIds, srcTokenIds));

            double[][] crossWeights = MatrixUtils.softmax(crossScores);
            stepConsumer.accept(stepDecoderCrossAttentionSoftmax(crossWeights, tgtTokenIds, srcTokenIds));

            double[][] crossAttnOutput = MatrixUtils.multiply(crossWeights, crossV);
            stepConsumer.accept(stepDecoderCrossAttentionWeightedSum(crossAttnOutput, tgtTokenIds));

            // 残差 + LayerNorm 2
            double[][] decResidual2 = MatrixUtils.add(decLN1, crossAttnOutput);
            double[][] decLN2 = MatrixUtils.layerNorm(decResidual2, config.getEpsilon());
            stepConsumer.accept(stepDecoderAddNorm2(decResidual2, decLN2, buildTokenLabels(tgtTokenIds)));

            // Decoder FFN
            double[][] decFFN1 = MatrixUtils.multiply(decLN2, w.wDecFFN1);
            decFFN1 = MatrixUtils.addBias(decFFN1, w.bDecFFN1);
            double[][] decFFNRelu = MatrixUtils.relu(decFFN1);
            double[][] decFFNOutput = MatrixUtils.multiply(decFFNRelu, w.wDecFFN2);
            decFFNOutput = MatrixUtils.addBias(decFFNOutput, w.bDecFFN2);
            stepConsumer.accept(stepDecoderFFN(decFFNOutput, buildTokenLabels(tgtTokenIds)));

            // 残差 + LayerNorm 3
            double[][] decResidual3 = MatrixUtils.add(decLN2, decFFNOutput);
            double[][] decFinalOutput = MatrixUtils.layerNorm(decResidual3, config.getEpsilon());
            stepConsumer.accept(stepDecoderAddNorm3(decResidual3, decFinalOutput, buildTokenLabels(tgtTokenIds)));

            // 输出投影和 Softmax
            double[][] logits = MatrixUtils.multiply(decFinalOutput, w.wOut);
            stepConsumer.accept(stepDecoderOutputLogits(logits, tgtTokenIds));

            double[][] probs = MatrixUtils.softmax(logits);
            stepConsumer.accept(stepDecoderSoftmaxProbs(probs, tgtTokenIds));

        } catch (Exception e) {
            log.error("Decoder 计算出错", e);
            throw new RuntimeException("Decoder 计算失败: " + e.getMessage(), e);
        }
    }

    private TransformerStep stepDecoderEmbedding(int[] tgtTokenIds, double[][] decoderInput) {
        List<String> rowLabels = buildTokenLabels(tgtTokenIds);
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < decoderInput[0].length; i++) {
            colLabels.add("d" + i);
        }
        double minVal = MatrixUtils.minVal(decoderInput);
        double maxVal = MatrixUtils.maxVal(decoderInput);

        return TransformerStep.builder()
                .type(StepType.DECODER_EMBEDDING)
                .title("Step - Decoder 嵌入 + 位置编码")
                .description("目标序列的 Token 嵌入和位置编码（与 Encoder 结构相同）")
                .analogy("用同样的方法给目标词注入位置信息，准备输入 Decoder。")
                .dataFlow("输入：目标 Token IDs\n输出：目标词的嵌入 + PE [tgt_len × d_model]，传入 Masked Self-Attention")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("Decoder Input (Emb + PE)")
                                .values(decoderInput)
                                .minVal(minVal)
                                .maxVal(maxVal)
                                .rowLabels(rowLabels)
                                .colLabels(colLabels)
                                .colorScheme("blue")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private TransformerStep stepDecoderMaskedAttentionScores(double[][] scores, int[] tgtTokenIds) {
        List<String> labels = buildTokenLabels(tgtTokenIds);
        double minVal = MatrixUtils.minVal(scores);
        double maxVal = MatrixUtils.maxVal(scores);

        return TransformerStep.builder()
                .type(StepType.DECODER_MASKED_SELF_ATTN)
                .title("Step - Decoder Masked Self-Attention 得分")
                .description("计算 Decoder 的自注意力，同时应用因果掩码\n\n" +
                        "scores = (Q × K^T / √d_k) + causal_mask\n" +
                        "上三角（未来位置）被设为 -∞，防止关注未来词")
                .analogy("自己和自己对话，但有'禁令'：不能偷看未来的词，只能看已有的词和自己。")
                .dataFlow("输入：decQ、decK（Decoder 的查询和键）\n输出：masked_scores —— 因果掩码后的注意力得分，传入 Softmax")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("scores (Q × K^T / √dk + causal_mask)")
                                .values(scores)
                                .minVal(minVal)
                                .maxVal(maxVal)
                                .rowLabels(labels)
                                .colLabels(labels)
                                .colorScheme("diverging")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private TransformerStep stepDecoderMaskedAttentionSoftmax(double[][] weights, int[] tgtTokenIds) {
        List<String> labels = buildTokenLabels(tgtTokenIds);
        double minVal = MatrixUtils.minVal(weights);
        double maxVal = MatrixUtils.maxVal(weights);

        return TransformerStep.builder()
                .type(StepType.DECODER_MASKED_SOFTMAX)
                .title("Step - Decoder Masked Attention 权重")
                .description("Softmax 后的因果注意力权重\n\n" +
                        "上三角为 0（-∞ 变成 0），只看对角线及以下，体现自回归特性")
                .analogy("各词对自己和已有词的关注比例，上三角全是 0（看不到）。")
                .dataFlow("输入：masked_scores\n输出：masked_weights —— 每行和为 1，上三角为 0，传入加权求和")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("masked_attention_weights")
                                .values(weights)
                                .minVal(minVal)
                                .maxVal(maxVal)
                                .rowLabels(labels)
                                .colLabels(labels)
                                .colorScheme("red")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private TransformerStep stepDecoderMaskedAttentionWeightedSum(double[][] output, int[] tgtTokenIds) {
        List<String> rowLabels = buildTokenLabels(tgtTokenIds);
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < output[0].length; i++) {
            colLabels.add("d" + i);
        }
        double minVal = MatrixUtils.minVal(output);
        double maxVal = MatrixUtils.maxVal(output);

        return TransformerStep.builder()
                .type(StepType.DECODER_MASKED_WEIGHTED_SUM)
                .title("Step - Decoder Masked Attention 加权求和")
                .description("用因果权重对 Value 加权求和\n\n" +
                        "output = masked_weights × V")
                .analogy("混合自己和已有词的信息，按权重分配，未来的词贡献为 0。")
                .dataFlow("输入：masked_weights、V（Decoder Value）\n输出：masked_attn_output —— 融合后的表示，传入残差连接")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("masked_attn_output")
                                .values(output)
                                .minVal(minVal)
                                .maxVal(maxVal)
                                .rowLabels(rowLabels)
                                .colLabels(colLabels)
                                .colorScheme("blue")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private TransformerStep stepDecoderAddNorm1(double[][] residual, double[][] output, List<String> labels) {
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < output[0].length; i++) {
            colLabels.add("d" + i);
        }
        double minResidual = MatrixUtils.minVal(residual);
        double maxResidual = MatrixUtils.maxVal(residual);
        double minOutput = MatrixUtils.minVal(output);
        double maxOutput = MatrixUtils.maxVal(output);

        return TransformerStep.builder()
                .type(StepType.DECODER_ADD_NORM_1)
                .title("Step - Decoder 残差 + 层归一化 1")
                .description("Add & Norm 第一个块\n\n" +
                        "1. 残差连接：output = decoderInput + masked_attn_output\n" +
                        "2. 层归一化：LN(x) = (x - mean) / sqrt(var + eps)")
                .analogy("备份原始信息，防止梯度消失；再标准化，保证数值稳定。")
                .dataFlow("输入：decoderInput（残差源）+ masked_attn_output（Masked 自注意力结果）\n输出：LayerNorm 1 输出，传入 Cross-Attention")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("residual")
                                .values(residual)
                                .minVal(minResidual)
                                .maxVal(maxResidual)
                                .rowLabels(labels)
                                .colLabels(colLabels)
                                .colorScheme("blue")
                                .build(),
                        TransformerStep.MatrixData.builder()
                                .label("LayerNorm output")
                                .values(output)
                                .minVal(minOutput)
                                .maxVal(maxOutput)
                                .rowLabels(labels)
                                .colLabels(colLabels)
                                .colorScheme("blue")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private TransformerStep stepDecoderCrossAttentionScores(double[][] scores, int[] tgtTokenIds, int[] srcTokenIds) {
        List<String> tgtLabels = buildTokenLabels(tgtTokenIds);
        List<String> srcLabels = buildTokenLabels(srcTokenIds);
        double minVal = MatrixUtils.minVal(scores);
        double maxVal = MatrixUtils.maxVal(scores);

        return TransformerStep.builder()
                .type(StepType.DECODER_CROSS_ATTN_SCORES)
                .title("Step - Decoder Cross-Attention 得分")
                .description("Decoder 和 Encoder 之间的注意力（不加掩码）\n\n" +
                        "scores = (Q_dec × K_enc^T) / √d_k\n" +
                        "Q 来自 Decoder，K/V 来自 Encoder")
                .analogy("Decoder 去'翻阅'Encoder 的输出，问：'这些编码的词和我相关吗？'")
                .dataFlow("输入：crossQ（来自 Decoder）、crossK（来自 Encoder）\n输出：cross_scores —— Decoder 对 Encoder 各位置的相关度，传入 Softmax")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("cross_attention_scores [tgt_len × src_len]")
                                .values(scores)
                                .minVal(minVal)
                                .maxVal(maxVal)
                                .rowLabels(tgtLabels)
                                .colLabels(srcLabels)
                                .colorScheme("diverging")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private TransformerStep stepDecoderCrossAttentionSoftmax(double[][] weights, int[] tgtTokenIds, int[] srcTokenIds) {
        List<String> tgtLabels = buildTokenLabels(tgtTokenIds);
        List<String> srcLabels = buildTokenLabels(srcTokenIds);
        double minVal = MatrixUtils.minVal(weights);
        double maxVal = MatrixUtils.maxVal(weights);

        return TransformerStep.builder()
                .type(StepType.DECODER_CROSS_ATTN_SOFTMAX)
                .title("Step - Decoder Cross-Attention 权重")
                .description("Softmax 后的 Cross-Attention 权重\n\n" +
                        "形状：[tgt_len × src_len]，表示 Decoder 每个位置对 Encoder 各位置的关注强度")
                .analogy("Decoder 最终决定从 Encoder 的各词中吸收多少信息。")
                .dataFlow("输入：cross_scores\n输出：cross_weights —— 每行和为 1，表示注意力分布，传入加权求和")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("cross_attention_weights [tgt_len × src_len]")
                                .values(weights)
                                .minVal(minVal)
                                .maxVal(maxVal)
                                .rowLabels(tgtLabels)
                                .colLabels(srcLabels)
                                .colorScheme("red")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private TransformerStep stepDecoderCrossAttentionWeightedSum(double[][] output, int[] tgtTokenIds) {
        List<String> rowLabels = buildTokenLabels(tgtTokenIds);
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < output[0].length; i++) {
            colLabels.add("d" + i);
        }
        double minVal = MatrixUtils.minVal(output);
        double maxVal = MatrixUtils.maxVal(output);

        return TransformerStep.builder()
                .type(StepType.DECODER_CROSS_ATTN_WEIGHTED)
                .title("Step - Decoder Cross-Attention 加权求和")
                .description("用 Cross-Attention 权重对 Encoder 的 Value 加权求和\n\n" +
                        "output = cross_weights × V_enc")
                .analogy("综合来自 Encoder 的各词信息，按权重混合。")
                .dataFlow("输入：cross_weights、V_enc（来自 Encoder）\n输出：cross_attn_output —— 融合的编码信息，传入残差连接 2")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("cross_attn_output")
                                .values(output)
                                .minVal(minVal)
                                .maxVal(maxVal)
                                .rowLabels(rowLabels)
                                .colLabels(colLabels)
                                .colorScheme("blue")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private TransformerStep stepDecoderAddNorm2(double[][] residual, double[][] output, List<String> labels) {
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < output[0].length; i++) {
            colLabels.add("d" + i);
        }
        double minResidual = MatrixUtils.minVal(residual);
        double maxResidual = MatrixUtils.maxVal(residual);
        double minOutput = MatrixUtils.minVal(output);
        double maxOutput = MatrixUtils.maxVal(output);

        return TransformerStep.builder()
                .type(StepType.DECODER_ADD_NORM_2)
                .title("Step - Decoder 残差 + 层归一化 2")
                .description("Add & Norm 第二个块\n\n" +
                        "1. 残差连接：output = decLN1 + cross_attn_output\n" +
                        "2. 层归一化：LN(x) = (x - mean) / sqrt(var + eps)")
                .analogy("再次保存信息、再次标准化，融合 Decoder 自注意力和编码器信息。")
                .dataFlow("输入：decLN1（残差源）+ cross_attn_output（Cross-Attention 结果）\n输出：LayerNorm 2 输出，传入 FFN")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("residual")
                                .values(residual)
                                .minVal(minResidual)
                                .maxVal(maxResidual)
                                .rowLabels(labels)
                                .colLabels(colLabels)
                                .colorScheme("blue")
                                .build(),
                        TransformerStep.MatrixData.builder()
                                .label("LayerNorm output")
                                .values(output)
                                .minVal(minOutput)
                                .maxVal(maxOutput)
                                .rowLabels(labels)
                                .colLabels(colLabels)
                                .colorScheme("blue")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private TransformerStep stepDecoderFFN(double[][] output, List<String> labels) {
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < output[0].length; i++) {
            colLabels.add("d" + i);
        }
        double minVal = MatrixUtils.minVal(output);
        double maxVal = MatrixUtils.maxVal(output);

        return TransformerStep.builder()
                .type(StepType.DECODER_FFN)
                .title("Step - Decoder 前馈网络")
                .description("逐位置前馈网络\n\n" +
                        "FFN(x) = relu(x × W1 + b1) × W2 + b2")
                .analogy("每个词独立地通过一个小神经网络，发现非线性模式。")
                .dataFlow("输入：LayerNorm 2 输出\n输出：FFN_output —— 经过非线性变换的表示，传入残差连接 3")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("FFN_output")
                                .values(output)
                                .minVal(minVal)
                                .maxVal(maxVal)
                                .rowLabels(labels)
                                .colLabels(colLabels)
                                .colorScheme("blue")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private TransformerStep stepDecoderAddNorm3(double[][] residual, double[][] output, List<String> labels) {
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < output[0].length; i++) {
            colLabels.add("d" + i);
        }
        double minResidual = MatrixUtils.minVal(residual);
        double maxResidual = MatrixUtils.maxVal(residual);
        double minOutput = MatrixUtils.minVal(output);
        double maxOutput = MatrixUtils.maxVal(output);

        return TransformerStep.builder()
                .type(StepType.DECODER_ADD_NORM_3)
                .title("Step - Decoder 残差 + 层归一化 3")
                .description("Add & Norm 第三个块\n\n" +
                        "1. 残差连接：output = decLN2 + ffn_output\n" +
                        "2. 层归一化：最后的 Decoder 输出")
                .analogy("最后一次保存信息和标准化，Decoder 层结束。")
                .dataFlow("输入：decLN2（残差源）+ FFN_output\n输出：Decoder 最终表示，传入输出投影")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("residual")
                                .values(residual)
                                .minVal(minResidual)
                                .maxVal(maxResidual)
                                .rowLabels(labels)
                                .colLabels(colLabels)
                                .colorScheme("blue")
                                .build(),
                        TransformerStep.MatrixData.builder()
                                .label("LayerNorm output")
                                .values(output)
                                .minVal(minOutput)
                                .maxVal(maxOutput)
                                .rowLabels(labels)
                                .colLabels(colLabels)
                                .colorScheme("blue")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private TransformerStep stepDecoderOutputLogits(double[][] logits, int[] tgtTokenIds) {
        List<String> rowLabels = buildTokenLabels(tgtTokenIds);
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < Math.min(5, logits[0].length); i++) {
            colLabels.add("vocab_" + i);
        }

        return TransformerStep.builder()
                .type(StepType.DECODER_OUTPUT_LOGITS)
                .title("Step - Decoder 输出 Logits")
                .description("线性投影到词表大小\n\n" +
                        "logits = final_output × W_out\n" +
                        "输出：[tgt_len × vocab_size] 的原始分数")
                .analogy("把内部表示翻译成词表的'观点'，分数越高越可能是这个词。")
                .dataFlow("输入：Decoder 最终表示 [tgt_len × d_model]\n输出：logits —— 每个位置对词表的预测分数，传入 Softmax")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("logits (top-5 per position)")
                                .values(extractTop5(logits))
                                .minVal(0)
                                .maxVal(1)
                                .rowLabels(rowLabels)
                                .colLabels(List.of("top1", "top2", "top3", "top4", "top5"))
                                .colorScheme("red")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private TransformerStep stepDecoderSoftmaxProbs(double[][] probs, int[] tgtTokenIds) {
        List<String> rowLabels = buildTokenLabels(tgtTokenIds);
        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < Math.min(5, probs[0].length); i++) {
            colLabels.add("vocab_" + i);
        }

        return TransformerStep.builder()
                .type(StepType.DECODER_SOFTMAX_PROBS)
                .title("Step - Decoder Softmax 概率")
                .description("Softmax 后的概率分布\n\n" +
                        "probs = softmax(logits)\n" +
                        "每行和为 1.0，代表下一个词的概率分布")
                .analogy("最终的'投票结果'：概率最高的词就是模型的预测答案。")
                .dataFlow("输入：logits\n输出：probs —— 概率分布，最高概率位置即预测词")
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("softmax_probs (top-5 per position)")
                                .values(extractTop5(probs))
                                .minVal(0)
                                .maxVal(1)
                                .rowLabels(rowLabels)
                                .colLabels(colLabels)
                                .colorScheme("red")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 自回归生成：从 BOS 开始，逐步生成 token 直到 EOS 或达到最大长度
     */
    public void computeAutoRegressiveStreaming(int[] srcTokenIds, double[][] encoderOutput,
                                               TransformerConfig config,
                                               java.util.function.Consumer<TransformerStep> stepConsumer) {
        if (config == null) {
            config = defaultConfig;
        }
        config.validate();

        List<Integer> generated = new ArrayList<>();
        int bosId = tokenizer.getBosTokenId();
        int eosId = tokenizer.getEosTokenId();
        generated.add(bosId);

        int maxLen = config.getMaxSeqLen();
        for (int step = 0; step < maxLen - 1; step++) {
            int[] tgtTokenIds = generated.stream().mapToInt(Integer::intValue).toArray();

            // 运行 Decoder 前向传播（包含所有步骤的emit），获取 logits
            double[][] logits = runDecoderForwardWithSteps(tgtTokenIds, encoderOutput, srcTokenIds, config,
                    weights.getWeights(config), stepConsumer);

            // 获取最后一行的 logits（当前位置的预测）
            double[] lastRowLogits = logits[logits.length - 1];

            // Softmax
            double[] probs = softmax1D(lastRowLogits);

            // Argmax - 选择概率最高的 token
            int nextTokenId = argmax(probs);

            // 构建和发送 TOKEN_GENERATED 步骤
            stepConsumer.accept(buildTokenGeneratedStep(nextTokenId, probs, step + 1, tgtTokenIds));

            // 如果生成了 EOS，停止
            if (nextTokenId == eosId) {
                break;
            }

            // 添加到生成序列
            generated.add(nextTokenId);
        }
    }

    /**
     * 运行 Decoder 前向传播，包含所有步骤的emit
     */
    private double[][] runDecoderForwardWithSteps(int[] tgtTokenIds, double[][] encoderOutput, int[] srcTokenIds,
                                                  TransformerConfig config, TransformerWeights.WeightMatrices w,
                                                  java.util.function.Consumer<TransformerStep> stepConsumer) {
        // Decoder 嵌入 + 位置编码
        double[][] decX = embedTokens(tgtTokenIds, w.embeddingMatrix);
        double[][] decPE = computePositionalEncoding(tgtTokenIds.length, config.getEmbeddingDim());
        double[][] decoderInput = MatrixUtils.add(decX, decPE);
        stepConsumer.accept(stepDecoderEmbedding(tgtTokenIds, decoderInput));

        // Masked Self-Attention
        double[][] decQ = MatrixUtils.multiply(decoderInput, w.wDecQ);
        double[][] decK = MatrixUtils.multiply(decoderInput, w.wDecK);
        double[][] decV = MatrixUtils.multiply(decoderInput, w.wDecV);

        double[][] causalMask = MatrixUtils.causalMask(tgtTokenIds.length);
        double[][] decScores = MatrixUtils.multiply(decQ, MatrixUtils.transpose(decK));
        decScores = MatrixUtils.scalarMultiply(decScores, 1.0 / Math.sqrt(config.getEmbeddingDim() / config.getNumHeads()));
        decScores = MatrixUtils.add(decScores, causalMask);

        stepConsumer.accept(stepDecoderMaskedAttentionScores(decScores, tgtTokenIds));

        double[][] decWeights = MatrixUtils.softmax(decScores);
        stepConsumer.accept(stepDecoderMaskedAttentionSoftmax(decWeights, tgtTokenIds));

        double[][] maskedAttnOutput = MatrixUtils.multiply(decWeights, decV);
        stepConsumer.accept(stepDecoderMaskedAttentionWeightedSum(maskedAttnOutput, tgtTokenIds));

        // 残差 + LayerNorm 1
        double[][] decResidual1 = MatrixUtils.add(decoderInput, maskedAttnOutput);
        double[][] decLN1 = MatrixUtils.layerNorm(decResidual1, config.getEpsilon());
        stepConsumer.accept(stepDecoderAddNorm1(decResidual1, decLN1, buildTokenLabels(tgtTokenIds)));

        // Cross-Attention（Q 来自 Decoder，K/V 来自 Encoder）
        double[][] crossQ = MatrixUtils.multiply(decLN1, w.wCrossQ);
        double[][] crossK = MatrixUtils.multiply(encoderOutput, w.wCrossK);
        double[][] crossV = MatrixUtils.multiply(encoderOutput, w.wCrossV);

        double[][] crossScores = MatrixUtils.multiply(crossQ, MatrixUtils.transpose(crossK));
        crossScores = MatrixUtils.scalarMultiply(crossScores, 1.0 / Math.sqrt(config.getEmbeddingDim() / config.getNumHeads()));

        stepConsumer.accept(stepDecoderCrossAttentionScores(crossScores, tgtTokenIds, srcTokenIds));

        double[][] crossWeights = MatrixUtils.softmax(crossScores);
        stepConsumer.accept(stepDecoderCrossAttentionSoftmax(crossWeights, tgtTokenIds, srcTokenIds));

        double[][] crossAttnOutput = MatrixUtils.multiply(crossWeights, crossV);
        stepConsumer.accept(stepDecoderCrossAttentionWeightedSum(crossAttnOutput, tgtTokenIds));

        // 残差 + LayerNorm 2
        double[][] decResidual2 = MatrixUtils.add(decLN1, crossAttnOutput);
        double[][] decLN2 = MatrixUtils.layerNorm(decResidual2, config.getEpsilon());
        stepConsumer.accept(stepDecoderAddNorm2(decResidual2, decLN2, buildTokenLabels(tgtTokenIds)));

        // Decoder FFN
        double[][] decFFN1 = MatrixUtils.multiply(decLN2, w.wDecFFN1);
        decFFN1 = MatrixUtils.addBias(decFFN1, w.bDecFFN1);
        double[][] decFFNRelu = MatrixUtils.relu(decFFN1);
        double[][] decFFNOutput = MatrixUtils.multiply(decFFNRelu, w.wDecFFN2);
        decFFNOutput = MatrixUtils.addBias(decFFNOutput, w.bDecFFN2);
        stepConsumer.accept(stepDecoderFFN(decFFNOutput, buildTokenLabels(tgtTokenIds)));

        // 残差 + LayerNorm 3
        double[][] decResidual3 = MatrixUtils.add(decLN2, decFFNOutput);
        double[][] decFinalOutput = MatrixUtils.layerNorm(decResidual3, config.getEpsilon());
        stepConsumer.accept(stepDecoderAddNorm3(decResidual3, decFinalOutput, buildTokenLabels(tgtTokenIds)));

        // 输出投影
        return MatrixUtils.multiply(decFinalOutput, w.wOut);
    }

    /**
     * 运行 Decoder 前向传播，返回 logits（不发送任何步骤）
     */
    private double[][] runDecoderForward(int[] tgtTokenIds, double[][] encoderOutput, int[] srcTokenIds,
                                         TransformerConfig config, TransformerWeights.WeightMatrices w) {
        // Decoder 嵌入 + 位置编码
        double[][] decX = embedTokens(tgtTokenIds, w.embeddingMatrix);
        double[][] decPE = computePositionalEncoding(tgtTokenIds.length, config.getEmbeddingDim());
        double[][] decoderInput = MatrixUtils.add(decX, decPE);

        // Masked Self-Attention
        double[][] decQ = MatrixUtils.multiply(decoderInput, w.wDecQ);
        double[][] decK = MatrixUtils.multiply(decoderInput, w.wDecK);
        double[][] decV = MatrixUtils.multiply(decoderInput, w.wDecV);

        double[][] causalMask = MatrixUtils.causalMask(tgtTokenIds.length);
        double[][] decScores = MatrixUtils.multiply(decQ, MatrixUtils.transpose(decK));
        decScores = MatrixUtils.scalarMultiply(decScores, 1.0 / Math.sqrt(config.getEmbeddingDim() / config.getNumHeads()));
        decScores = MatrixUtils.add(decScores, causalMask);

        double[][] decWeights = MatrixUtils.softmax(decScores);
        double[][] maskedAttnOutput = MatrixUtils.multiply(decWeights, decV);

        // 残差 + LayerNorm 1
        double[][] decResidual1 = MatrixUtils.add(decoderInput, maskedAttnOutput);
        double[][] decLN1 = MatrixUtils.layerNorm(decResidual1, config.getEpsilon());

        // Cross-Attention（Q 来自 Decoder，K/V 来自 Encoder）
        double[][] crossQ = MatrixUtils.multiply(decLN1, w.wCrossQ);
        double[][] crossK = MatrixUtils.multiply(encoderOutput, w.wCrossK);
        double[][] crossV = MatrixUtils.multiply(encoderOutput, w.wCrossV);

        double[][] crossScores = MatrixUtils.multiply(crossQ, MatrixUtils.transpose(crossK));
        crossScores = MatrixUtils.scalarMultiply(crossScores, 1.0 / Math.sqrt(config.getEmbeddingDim() / config.getNumHeads()));

        double[][] crossWeights = MatrixUtils.softmax(crossScores);
        double[][] crossAttnOutput = MatrixUtils.multiply(crossWeights, crossV);

        // 残差 + LayerNorm 2
        double[][] decResidual2 = MatrixUtils.add(decLN1, crossAttnOutput);
        double[][] decLN2 = MatrixUtils.layerNorm(decResidual2, config.getEpsilon());

        // Decoder FFN
        double[][] decFFN1 = MatrixUtils.multiply(decLN2, w.wDecFFN1);
        decFFN1 = MatrixUtils.addBias(decFFN1, w.bDecFFN1);
        double[][] decFFNRelu = MatrixUtils.relu(decFFN1);
        double[][] decFFNOutput = MatrixUtils.multiply(decFFNRelu, w.wDecFFN2);
        decFFNOutput = MatrixUtils.addBias(decFFNOutput, w.bDecFFN2);

        // 残差 + LayerNorm 3
        double[][] decResidual3 = MatrixUtils.add(decLN2, decFFNOutput);
        double[][] decFinalOutput = MatrixUtils.layerNorm(decResidual3, config.getEpsilon());

        // 输出投影
        return MatrixUtils.multiply(decFinalOutput, w.wOut);
    }

    /**
     * 1D Softmax
     */
    private double[] softmax1D(double[] logits) {
        double max = java.util.Arrays.stream(logits).max().orElse(0);
        double[] exp = java.util.Arrays.stream(logits).map(v -> Math.exp(v - max)).toArray();
        double sum = java.util.Arrays.stream(exp).sum();
        return java.util.Arrays.stream(exp).map(v -> v / sum).toArray();
    }

    /**
     * Argmax - 返回最大值的索引（仅从有效的token中选择）
     */
    private int argmax(double[] probs) {
        int best = -1;
        double bestProb = -1;
        // 只从有效token（非<UNK_N>占位符）中选择
        for (int i = 0; i < probs.length; i++) {
            String word = tokenizer.getWord(i);
            // 跳过占位符UNK_N
            if (word != null && !word.startsWith("<UNK_")) {
                if (probs[i] > bestProb) {
                    best = i;
                    bestProb = probs[i];
                }
            }
        }
        // 如果没找到有效token，返回概率最高的（即使是UNK）
        if (best == -1) {
            best = 0;
            for (int i = 1; i < probs.length; i++) {
                if (probs[i] > probs[best]) {
                    best = i;
                }
            }
        }
        return best;
    }

    /**
     * 提取 Top-5 token 及其概率（用于 TOKEN_GENERATED 步骤）
     * 返回格式：[[index0, prob0], [index1, prob1], ...]
     */
    private double[][] extractTop5WithIndices(double[] probs) {
        List<java.util.Map.Entry<Integer, Double>> entries = new ArrayList<>();
        for (int i = 0; i < probs.length; i++) {
            entries.add(new java.util.AbstractMap.SimpleEntry<>(i, probs[i]));
        }
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        double[][] result = new double[Math.min(5, entries.size())][2];
        for (int i = 0; i < Math.min(5, entries.size()); i++) {
            result[i][0] = entries.get(i).getKey();
            result[i][1] = entries.get(i).getValue();
        }
        return result;
    }

    /**
     * 构建 TOKEN_GENERATED 步骤
     */
    private TransformerStep buildTokenGeneratedStep(int tokenId, double[] probs, int stepNum, int[] prevTokenIds) {
        String token = tokenizer.getWord(tokenId);

        // 获取 Top-5
        double[][] top5Indices = extractTop5WithIndices(probs);
        List<Map<String, Object>> top5List = new ArrayList<>();
        for (double[] entry : top5Indices) {
            int idx = (int) entry[0];
            double prob = entry[1];
            top5List.add(Map.of(
                    "token", tokenizer.getWord(idx),
                    "id", idx,
                    "prob", prob
            ));
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tokenId", tokenId);
        metadata.put("token", token);
        metadata.put("step", stepNum);
        metadata.put("top5", top5List);

        List<String> rowLabels = new ArrayList<>();
        rowLabels.add(token + " (" + Math.round(probs[tokenId] * 100) + "%)");

        List<String> colLabels = new ArrayList<>();
        for (int i = 0; i < Math.min(5, probs.length); i++) {
            colLabels.add("rank" + (i + 1));
        }

        // 构建矩阵：显示 Top-5 的概率
        double[][] probMatrix = new double[1][Math.min(5, probs.length)];
        for (int i = 0; i < Math.min(5, probs.length); i++) {
            probMatrix[0][i] = probs[(int) top5Indices[i][0]];
        }

        return TransformerStep.builder()
                .type(StepType.TOKEN_GENERATED)
                .title("Step " + stepNum + " - 生成 Token: " + token)
                .description("模型选择了'" + token + "'作为下一个 token。上图展示了 Top-5 候选词的概率分布。")
                .analogy("从所有可能的词汇中，选择概率最高的那个。")
                .dataFlow("输入：前面生成的 token 序列\n输出：下一个 token '" + token + "' (概率 " +
                        Math.round(probs[tokenId] * 1000.0) / 10.0 + "%)")
                .metadata(metadata)
                .matrices(List.of(
                        TransformerStep.MatrixData.builder()
                                .label("Top-5 候选词概率")
                                .values(probMatrix)
                                .minVal(0)
                                .maxVal(1)
                                .rowLabels(rowLabels)
                                .colLabels(colLabels)
                                .colorScheme("red")
                                .build()
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
