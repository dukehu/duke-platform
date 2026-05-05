package com.duke.demo.service;

/**
 * 硅基流动 LLM 调用接口
 */
public interface SiliconFlowService {

    /**
     * 只传 userPrompt，无 systemPrompt
     */
    String chat(String userPrompt);

    /**
     * systemPrompt + userPrompt
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 获取当前使用的模型名称
     */
    String getModel();
}
