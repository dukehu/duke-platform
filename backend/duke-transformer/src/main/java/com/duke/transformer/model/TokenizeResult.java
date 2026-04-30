package com.duke.transformer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分词结果 DTO：包含词列表、token IDs、未知词
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenizeResult {
    private List<String> tokens;        // 分词结果词列表
    private List<Integer> tokenIds;     // 对应的 token ID 列表
    private List<String> unknownTokens; // 不在词表中的词列表
}
