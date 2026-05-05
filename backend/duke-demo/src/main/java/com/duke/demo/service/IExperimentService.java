package com.duke.demo.service;

import java.util.List;

/**
 * 项目一：Prompt 模式对比实验接口
 */
public interface IExperimentService {

    /**
     * 运行指定模式
     *
     * @param mode zero-shot | role-shot | few-shot | cot | structured
     * @param code 待分析的 Java 代码片段
     */
    ExperimentResult runMode(String mode, String code);

    /**
     * 对同一段代码跑全部 5 种模式
     */
    List<ExperimentResult> runAllModes(String code);

    /**
     * 单次实验结果
     *
     * @param mode      模式名称
     * @param code      输入代码
     * @param response  模型原始输出
     * @param latencyMs 耗时（毫秒）
     * @param formatOk  输出是否包含期望的 JSON 字段
     */
    record ExperimentResult(
            String mode,
            String code,
            String response,
            long latencyMs,
            boolean formatOk
    ) {}
}
