package com.duke.demo.document;

import com.duke.demo.config.properties.DocumentPipelineProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块器，按段落优先 + 字符滑窗 + overlap 分块
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TextChunker {

    private final DocumentPipelineProperties props;

    /**
     * 使用配置中的参数分块
     *
     * @param text 原始文本
     * @return 分块列表
     */
    public List<String> chunk(String text) {
        return chunk(text, props.getChunkSize(), props.getOverlap());
    }

    /**
     * 自定义参数分块
     *
     * @param text      原始文本
     * @param chunkSize 每块字符数
     * @param overlap   重叠字符数
     * @return 分块列表
     */
    public List<String> chunk(String text, int chunkSize, int overlap) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        // 步骤1：按空行分割成段落
        List<String> paragraphs = splitIntoParagraphs(text);

        // 步骤2：段落累积成块
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            int paragraphLen = paragraph.length();

            if (currentChunk.length() + paragraphLen <= chunkSize) {
                // 当前段落放得下
                currentChunk.append(paragraph);
            } else {
                // 当前段落放不下
                if (currentChunk.length() > 0) {
                    // 提交当前块
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }

                if (paragraphLen <= chunkSize) {
                    // 单段落不超，作为新块的开头
                    currentChunk.append(paragraph);
                } else {
                    // 单段落超大，做强制切割
                    List<String> splitChunks = splitLongParagraph(paragraph, chunkSize, overlap);
                    chunks.addAll(splitChunks);
                }
            }
        }

        // 提交剩余块
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        // 步骤3：应用 overlap（在相邻块之间拼接）
        return applyOverlap(chunks, overlap);
    }

    /**
     * 按空行分割段落
     */
    private List<String> splitIntoParagraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        String[] parts = text.split("\\n\\n+");

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                paragraphs.add(trimmed + "\n");
            }
        }

        return paragraphs;
    }

    /**
     * 对超长段落做字符级滑动窗口（优先在句子边界切分）
     */
    private List<String> splitLongParagraph(String paragraph, int chunkSize, int overlap) {
        List<String> result = new ArrayList<>();
        int start = 0;

        while (start < paragraph.length()) {
            int end = Math.min(start + chunkSize, paragraph.length());

            // 在 [max(start+1, end-50), end] 区间内查找最右的句子边界
            int adjustedEnd = findSentenceBoundary(paragraph, end, Math.max(start + 1, end - 50));

            String chunk = paragraph.substring(start, adjustedEnd).trim();
            if (!chunk.isEmpty()) {
                result.add(chunk);
            }

            // 移动 start：保证 overlap 最少为 1
            start = Math.max(start + 1, adjustedEnd - overlap);
        }

        return result;
    }

    /**
     * 在 [searchStart, end] 范围内查找最右的句子边界（中文或英文）
     */
    private int findSentenceBoundary(String text, int end, int searchStart) {
        // 优先在句子边界切分：。！？.!?
        for (int i = end - 1; i >= searchStart; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '.' || c == '!' || c == '?') {
                return i + 1;
            }
        }
        // 未找到句子边界，返回原计划的 end
        return end;
    }

    /**
     * 对块列表应用 overlap：在相邻块之间拼接上一块的末尾 overlap 个字符
     */
    private List<String> applyOverlap(List<String> chunks, int overlap) {
        if (chunks.isEmpty() || overlap <= 0) {
            return chunks;
        }

        List<String> result = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);

            if (i > 0) {
                String prevChunk = chunks.get(i - 1);
                int overlapLen = Math.min(overlap, prevChunk.length());
                String overlapPart = prevChunk.substring(prevChunk.length() - overlapLen);

                // 若当前块开头已包含 overlap 内容，则跳过
                if (!chunk.startsWith(overlapPart)) {
                    chunk = overlapPart + chunk;
                }
            }

            result.add(chunk);
        }

        return result;
    }
}
