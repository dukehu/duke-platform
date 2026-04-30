package com.duke.transformer.core;

import lombok.experimental.UtilityClass;

/**
 * 纯 Java 矩阵工具类，实现 Transformer 所有矩阵运算
 * 所有矩阵表示为 double[][]，行优先存储
 */
@UtilityClass
public class MatrixUtils {

    private static final double EPSILON = 1e-6;

    /**
     * 创建零矩阵
     */
    public static double[][] zeros(int rows, int cols) {
        return new double[rows][cols];
    }

    /**
     * 创建全 1 矩阵
     */
    public static double[][] ones(int rows, int cols) {
        double[][] matrix = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = 1.0;
            }
        }
        return matrix;
    }

    /**
     * Xavier Uniform 初始化：limit = sqrt(6 / (fan_in + fan_out))
     * 使用指定的 seed 保证可复现性
     */
    public static double[][] xavierUniform(int rows, int cols, long seed) {
        java.util.Random random = new java.util.Random(seed);
        double limit = Math.sqrt(6.0 / (rows + cols));
        double[][] matrix = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // 均匀分布 [-limit, limit]
                matrix[i][j] = (random.nextDouble() * 2 - 1) * limit;
            }
        }
        return matrix;
    }

    /**
     * 高斯分布初始化：标准差 scale
     */
    public static double[][] gaussianRandom(int rows, int cols, long seed, double scale) {
        java.util.Random random = new java.util.Random(seed);
        double[][] matrix = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = random.nextGaussian() * scale;
            }
        }
        return matrix;
    }

    /**
     * 矩阵乘法：A[m×k] × B[k×n] = C[m×n]
     */
    public static double[][] multiply(double[][] A, double[][] B) {
        int m = A.length;
        int k = A[0].length;
        int n = B[0].length;

        if (B.length != k) {
            throw new IllegalArgumentException("矩阵维度不匹配：A 列数 " + k + " != B 行数 " + B.length);
        }

        double[][] C = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                double sum = 0.0;
                for (int p = 0; p < k; p++) {
                    sum += A[i][p] * B[p][j];
                }
                C[i][j] = sum;
            }
        }
        return C;
    }

    /**
     * 矩阵转置：A[m×n] -> A^T[n×m]
     */
    public static double[][] transpose(double[][] A) {
        int m = A.length;
        int n = A[0].length;
        double[][] AT = new double[n][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                AT[j][i] = A[i][j];
            }
        }
        return AT;
    }

    /**
     * 矩阵加法：A + B（要求同形状）
     */
    public static double[][] add(double[][] A, double[][] B) {
        if (A.length != B.length || A[0].length != B[0].length) {
            throw new IllegalArgumentException("矩阵形状不匹配");
        }
        int m = A.length;
        int n = A[0].length;
        double[][] C = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] + B[i][j];
            }
        }
        return C;
    }

    /**
     * 矩阵减法：A - B
     */
    public static double[][] subtract(double[][] A, double[][] B) {
        if (A.length != B.length || A[0].length != B[0].length) {
            throw new IllegalArgumentException("矩阵形状不匹配");
        }
        int m = A.length;
        int n = A[0].length;
        double[][] C = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] - B[i][j];
            }
        }
        return C;
    }

    /**
     * 给矩阵每行加上偏置向量
     */
    public static double[][] addBias(double[][] A, double[] bias) {
        int m = A.length;
        int n = A[0].length;
        if (bias.length != n) {
            throw new IllegalArgumentException("偏置维度 " + bias.length + " != 矩阵列数 " + n);
        }
        double[][] B = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                B[i][j] = A[i][j] + bias[j];
            }
        }
        return B;
    }

    /**
     * 标量乘法：matrix × scalar
     */
    public static double[][] scalarMultiply(double[][] A, double scalar) {
        int m = A.length;
        int n = A[0].length;
        double[][] B = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                B[i][j] = A[i][j] * scalar;
            }
        }
        return B;
    }

    /**
     * ReLU 激活：max(0, x)
     */
    public static double[][] relu(double[][] A) {
        int m = A.length;
        int n = A[0].length;
        double[][] B = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                B[i][j] = Math.max(0.0, A[i][j]);
            }
        }
        return B;
    }

    /**
     * 沿行做 Softmax（每行和为 1.0）
     * 使用数值稳定技巧：先减去每行的最大值
     */
    public static double[][] softmax(double[][] A) {
        int m = A.length;
        int n = A[0].length;
        double[][] result = new double[m][n];

        for (int i = 0; i < m; i++) {
            // 找出第 i 行的最大值
            double maxVal = A[i][0];
            for (int j = 1; j < n; j++) {
                if (A[i][j] > maxVal) {
                    maxVal = A[i][j];
                }
            }

            // 计算 exp(x - max) 并求和
            double sumExp = 0.0;
            for (int j = 0; j < n; j++) {
                result[i][j] = Math.exp(A[i][j] - maxVal);
                sumExp += result[i][j];
            }

            // 正规化为概率分布
            for (int j = 0; j < n; j++) {
                result[i][j] /= sumExp;
            }
        }
        return result;
    }

    /**
     * 层归一化：(x - mean) / sqrt(var + eps)，沿最后一维（每行）
     * gamma=1, beta=0（演示中不训练参数）
     */
    public static double[][] layerNorm(double[][] X, double epsilon) {
        int m = X.length;
        int n = X[0].length;
        double[][] result = new double[m][n];

        for (int i = 0; i < m; i++) {
            // 计算第 i 行的均值
            double mean = 0.0;
            for (int j = 0; j < n; j++) {
                mean += X[i][j];
            }
            mean /= n;

            // 计算方差
            double variance = 0.0;
            for (int j = 0; j < n; j++) {
                double diff = X[i][j] - mean;
                variance += diff * diff;
            }
            variance /= n;

            // 归一化
            double stdDev = Math.sqrt(variance + epsilon);
            for (int j = 0; j < n; j++) {
                result[i][j] = (X[i][j] - mean) / stdDev;
            }
        }
        return result;
    }

    /**
     * 矩阵列切片：取列 [fromCol, toCol) 的子矩阵
     */
    public static double[][] sliceCols(double[][] A, int fromCol, int toCol) {
        int m = A.length;
        int n = toCol - fromCol;
        if (n <= 0 || toCol > A[0].length) {
            throw new IllegalArgumentException("列范围无效");
        }
        double[][] B = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                B[i][j] = A[i][fromCol + j];
            }
        }
        return B;
    }

    /**
     * 水平拼接：A[m×a] || B[m×b] = C[m×(a+b)]
     */
    public static double[][] horizontalConcat(double[][] A, double[][] B) {
        if (A.length != B.length) {
            throw new IllegalArgumentException("矩阵行数不匹配");
        }
        int m = A.length;
        int a = A[0].length;
        int b = B[0].length;
        double[][] C = new double[m][a + b];
        for (int i = 0; i < m; i++) {
            System.arraycopy(A[i], 0, C[i], 0, a);
            System.arraycopy(B[i], 0, C[i], a, b);
        }
        return C;
    }

    /**
     * 生成因果掩码矩阵（Causal Mask）
     * 上三角（i < j）设为 -1e9，对角线及以下为 0
     * 用于 Decoder 的 Masked Self-Attention，防止看到未来词
     */
    public static double[][] causalMask(int seqLen) {
        double[][] mask = new double[seqLen][seqLen];
        for (int i = 0; i < seqLen; i++) {
            for (int j = 0; j < seqLen; j++) {
                if (j > i) {
                    mask[i][j] = -1e9;
                } else {
                    mask[i][j] = 0.0;
                }
            }
        }
        return mask;
    }

    /**
     * 缩放点积注意力：Attention(Q,K,V) = softmax(QK^T / sqrt(d_k)) * V
     * 返回 AttentionResult（包含注意力权重矩阵和输出矩阵）
     */
    public static AttentionResult scaledDotProductAttention(double[][] Q, double[][] K, double[][] V, int dk) {
        // 1. 计算得分：scores = Q * K^T
        double[][] KT = transpose(K);
        double[][] scores = multiply(Q, KT);

        // 2. 缩放：scores / sqrt(dk)
        double scale = Math.sqrt(dk);
        double[][] scaledScores = scalarMultiply(scores, 1.0 / scale);

        // 3. Softmax：得到注意力权重
        double[][] attentionWeights = softmax(scaledScores);

        // 4. 加权求和：output = attentionWeights * V
        double[][] output = multiply(attentionWeights, V);

        return new AttentionResult(attentionWeights, output);
    }

    /**
     * 注意力计算结果 Record
     */
    public record AttentionResult(double[][] weights, double[][] output) {
    }

    /**
     * 求矩阵最小值
     */
    public static double minVal(double[][] A) {
        double min = Double.MAX_VALUE;
        for (double[] row : A) {
            for (double val : row) {
                if (val < min) {
                    min = val;
                }
            }
        }
        return min;
    }

    /**
     * 求矩阵最大值
     */
    public static double maxVal(double[][] A) {
        double max = -Double.MAX_VALUE;
        for (double[] row : A) {
            for (double val : row) {
                if (val > max) {
                    max = val;
                }
            }
        }
        return max;
    }

    /**
     * 格式化矩阵为字符串（用于日志输出）
     * 保留 4 位小数
     */
    public static String formatMatrix(double[][] A, String label) {
        StringBuilder sb = new StringBuilder();
        if (label != null && !label.isEmpty()) {
            sb.append(label).append(" [").append(A.length).append("×").append(A[0].length).append("]:\n");
        }

        // 打印列标签
        sb.append("    ");
        for (int j = 0; j < Math.min(A[0].length, 10); j++) {
            sb.append(String.format("%8d", j));
        }
        if (A[0].length > 10) {
            sb.append("  ...");
        }
        sb.append("\n");

        // 打印矩阵内容
        for (int i = 0; i < Math.min(A.length, 8); i++) {
            sb.append(String.format("%3d [", i));
            for (int j = 0; j < Math.min(A[0].length, 10); j++) {
                sb.append(String.format("%8.4f", A[i][j]));
            }
            if (A[0].length > 10) {
                sb.append("  ...");
            }
            sb.append("]\n");
        }
        if (A.length > 8) {
            sb.append("...\n");
        }
        return sb.toString();
    }

    /**
     * 计算向量的欧几里得范数
     */
    public static double norm(double[] vector) {
        double sum = 0.0;
        for (double v : vector) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }
}
