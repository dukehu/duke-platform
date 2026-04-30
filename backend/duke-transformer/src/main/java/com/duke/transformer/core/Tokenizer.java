package com.duke.transformer.core;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 词级分词器（Word-level）
 * 将输入文本通过 Jieba 分词，转换为 token ID，支持反向解码
 * 词表：常见中文词汇 + 英文字符 + 数字 + 特殊符号
 */
@Component
@Getter
public class Tokenizer {

    private final Map<String, Integer> wordToId;
    private final Map<Integer, String> idToWord;
    private final int vocabSize;
    private final int maxSeqLen;

    private static final int VOCAB_SIZE = 500;  // 词汇表大小（词级）
    private static final int MAX_SEQ_LEN = 8;
    private static final String PAD_TOKEN = "<PAD>";
    private static final String UNK_TOKEN = "<UNK>";
    private static final String BOS_TOKEN = "<BOS>";
    private static final String EOS_TOKEN = "<EOS>";

    public Tokenizer() {
        this(VOCAB_SIZE, MAX_SEQ_LEN);
    }

    public Tokenizer(int vocabSize, int maxSeqLen) {
        this.vocabSize = vocabSize;
        this.maxSeqLen = maxSeqLen;
        this.wordToId = new LinkedHashMap<>();
        this.idToWord = new LinkedHashMap<>();
        buildVocab();
    }

    /**
     * 核心分词逻辑：正向最大匹配
     */
    public List<String> segment(String sentence) {
        List<String> result = new ArrayList<>();
        int idx = 0;
        int len = sentence.length();

        while (idx < len) {
            // 默认先只取1个字兜底
            int maxLen = 1;
            // 从当前位置往后试探最长匹配
            for (int i = 1; i <= len - idx; i++) {
                String sub = sentence.substring(idx, idx + i);
                if (wordToId.keySet().contains(sub)) {
                    maxLen = i;
                }
            }
            // 切出当前最优词语
            String word = sentence.substring(idx, idx + maxLen);
            result.add(word);
            // 指针前进
            idx += maxLen;
        }
        return result;
    }

    /**
     * 构建词汇表
     */
    private void buildVocab() {
        int id = 0;

        // 特殊符号
        wordToId.put(PAD_TOKEN, id);
        idToWord.put(id, PAD_TOKEN);
        id++;

        wordToId.put(UNK_TOKEN, id);
        idToWord.put(id, UNK_TOKEN);
        id++;

        wordToId.put(BOS_TOKEN, id);
        idToWord.put(id, BOS_TOKEN);
        id++;

        wordToId.put(EOS_TOKEN, id);
        idToWord.put(id, EOS_TOKEN);
        id++;

        // 常见中文词汇（包括单个字符和多字词）
        String[] chineseVocab = {
                "用", "电", "鳗", "鱼", "鳗鱼", "会", "不会", "会不会", "死", "被电死",
                "的", "我", "你", "他", "她", "这", "那", "是", "在", "和", "或", "有", "没有", "人", "大", "小",
                "爱",  // 新增：中文"爱"字
                "多少", "什么", "哪里", "怎么", "为什么", "从", "到", "与", "对于", "给", "让", "被", "了", "吗", "呢",
                "好", "很", "太", "吧", "行", "上", "下", "左", "右", "前", "后", "中间", "内", "外",
                "来", "去", "过", "完成", "成功", "失败", "错", "开始", "结束", "时间", "明天", "后天", "今天", "昨天",
                "周", "一", "二", "三", "四", "五", "六", "日", "月", "年", "分", "秒", "钟", "小时", "分钟",
                "天数", "个", "十", "百", "千", "万", "条件", "判断", "循环", "数组", "列表", "集合", "字典", "对象",
                "类型", "函数", "方法", "变量", "赋值", "比较", "运算符", "空格", "用电", "死", "电死"
        };

        for (String word : chineseVocab) {
            if (!wordToId.containsKey(word) && id < vocabSize) {
                wordToId.put(word, id);
                idToWord.put(id, word);
                id++;
            }
        }

        // 英文字母和数字
        for (char c = 'a'; c <= 'z'; c++) {
            if (id < vocabSize) {
                String s = String.valueOf(c);
                if (!wordToId.containsKey(s)) {
                    wordToId.put(s, id);
                    idToWord.put(id, s);
                    id++;
                }
            }
        }

        for (char c = 'A'; c <= 'Z'; c++) {
            if (id < vocabSize) {
                String s = String.valueOf(c);
                if (!wordToId.containsKey(s)) {
                    wordToId.put(s, id);
                    idToWord.put(id, s);
                    id++;
                }
            }
        }

        for (char c = '0'; c <= '9'; c++) {
            if (id < vocabSize) {
                String s = String.valueOf(c);
                if (!wordToId.containsKey(s)) {
                    wordToId.put(s, id);
                    idToWord.put(id, s);
                    id++;
                }
            }
        }

        // 英文单词（用于翻译演示）
        String[] englishWords = {
                "love", "need", "want", "like", "miss",
                "you", "me", "him", "her", "us",
                "i", "he", "she", "we", "they"
        };

        for (String word : englishWords) {
            if (id < vocabSize && !wordToId.containsKey(word)) {
                wordToId.put(word, id);
                idToWord.put(id, word);
                id++;
            }
        }

        // 填充剩余位置
        while (id < vocabSize) {
            String placeholder = "<UNK_" + (id - 2) + ">";
            wordToId.put(placeholder, id);
            idToWord.put(id, placeholder);
            id++;
        }
    }

    /**
     * 编码：文本 → token ID 数组
     * 先通过 Jieba 分词，再转换为 token ID
     */
    public int[] encode(String text) {
        if (text == null || text.isEmpty()) {
            int[] tokens = new int[maxSeqLen];
            for (int i = 0; i < maxSeqLen; i++) {
                tokens[i] = 0;  // PAD
            }
            return tokens;
        }

        // 分词
        List<String> words = segment(text);

        // 转换为 token ID
        List<Integer> tokenList = new ArrayList<>();
        for (String word : words) {
            if (tokenList.size() >= maxSeqLen) {
                break;
            }
            Integer tokenId = wordToId.getOrDefault(word, 1);  // 1 是 UNK
            tokenList.add(tokenId);
        }

        // 补齐到 maxSeqLen
        int[] tokens = new int[maxSeqLen];
        for (int i = 0; i < maxSeqLen; i++) {
            if (i < tokenList.size()) {
                tokens[i] = tokenList.get(i);
            } else {
                tokens[i] = 0;  // PAD
            }
        }
        return tokens;
    }

    /**
     * 解码：token ID 数组 → 文本
     */
    public String decode(int[] tokenIds) {
        StringBuilder sb = new StringBuilder();
        for (int id : tokenIds) {
            if (id == 0) {
                continue;
            }
            String word = idToWord.get(id);
            if (word != null && !word.equals(PAD_TOKEN)) {
                sb.append(word);
            }
        }
        return sb.toString();
    }

    /**
     * 获取词汇表（作为 List<String>）
     * 用于前端展示支持的词汇
     */
    public List<String> getVocabList() {
        List<String> vocabList = new ArrayList<>();
        for (int i = 0; i < vocabSize; i++) {
            String word = idToWord.get(i);
            if (word != null && !word.startsWith("<UNK_")) {
                vocabList.add(word);
            }
        }
        return vocabList;
    }

    /**
     * 获取词汇表映射表（用于日志展示）
     */
    public Map<Integer, String> getVocabMap() {
        Map<Integer, String> vocabMap = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : idToWord.entrySet()) {
            int id = entry.getKey();
            String word = entry.getValue();
            vocabMap.put(id, id + " -> '" + word + "'");
        }
        return vocabMap;
    }

    /**
     * 分词并展示
     * 用于前端展示和调试
     */
    public List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        return segment(text);
    }

    /**
     * 获取词对应的 token ID
     */
    public int getTokenId(String word) {
        return wordToId.getOrDefault(word, 1);  // 默认返回 UNK
    }

    /**
     * 获取 token ID 对应的词
     */
    public String getWord(int tokenId) {
        return idToWord.getOrDefault(tokenId, UNK_TOKEN);
    }

    /**
     * 获取 BOS token ID
     */
    public int getBosTokenId() {
        return wordToId.getOrDefault(BOS_TOKEN, 2);
    }

    /**
     * 获取 EOS token ID
     */
    public int getEosTokenId() {
        return wordToId.getOrDefault(EOS_TOKEN, 3);
    }
}
