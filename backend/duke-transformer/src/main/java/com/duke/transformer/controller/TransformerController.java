package com.duke.transformer.controller;

import com.duke.framework.common.Result;
import com.duke.transformer.config.TransformerConfig;
import com.duke.transformer.core.Tokenizer;
import com.duke.transformer.model.TokenizeResult;
import com.duke.transformer.service.TransformerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Transformer 演示 REST 控制器
 * 提供 SSE 流式接口、学习文档、词表、配置等接口
 */
@Slf4j
@Tag(name = "Transformer 学习演示")
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class TransformerController {

    private final TransformerService transformerService;
    private final TransformerConfig defaultConfig;
    private final Tokenizer tokenizer;

    /**
     * SSE 流式接口：运行 Transformer 并逐步推送计算步骤
     * 前端通过 EventSource 连接此端点，实时接收每个计算步骤的数据
     * 接收分词后的 token 数组，而不是原始文本
     */
    @Operation(summary = "运行 Transformer（SSE 流式）")
    @GetMapping(value = "/run", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runTransformer(
            @RequestParam(required = false) String tokens,
            @RequestParam(required = false) String text,
            @RequestParam(defaultValue = "16") int embeddingDim,
            @RequestParam(defaultValue = "2") int numHeads,
            @RequestParam(defaultValue = "1") int numLayers
    ) {
        SseEmitter emitter = new SseEmitter(120_000L);

        Executors.newVirtualThreadPerTaskExecutor().execute(() -> {
            try {
                int[] tokenIds;

                if (tokens != null) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String[] tokenArray = mapper.readValue(tokens, String[].class);
                    tokenIds = new int[tokenArray.length];
                    for (int i = 0; i < tokenArray.length; i++) {
                        tokenIds[i] = tokenizer.getTokenId(tokenArray[i]);
                    }
                } else if (text != null) {
                    List<String> tokenList = tokenizer.tokenize(text);
                    tokenIds = new int[tokenList.size()];
                    for (int i = 0; i < tokenList.size(); i++) {
                        tokenIds[i] = tokenizer.getTokenId(tokenList.get(i));
                    }
                } else {
                    throw new IllegalArgumentException("Either 'tokens' or 'text' parameter must be provided");
                }

                TransformerConfig config = defaultConfig.withOverrides(embeddingDim, numHeads, numLayers);
                config.validate();

                log.info("开始运行 Transformer：tokens={}, embedding_dim={}, num_heads={}, num_layers={}",
                        java.util.Arrays.toString(tokenIds), embeddingDim, numHeads, numLayers);

                ObjectMapper objectMapper = new ObjectMapper();
                transformerService.computeTransformerStepsStreaming(tokenIds, config, step -> {
                    try {
                        String json = objectMapper.writeValueAsString(step);
                        emitter.send(SseEmitter.event()
                                .id(System.currentTimeMillis() + "")
                                .name("step")
                                .data(json));
                        Thread.sleep(500);
                    } catch (IOException | InterruptedException e) {
                        log.error("SSE 推送步骤失败", e);
                        throw new RuntimeException(e);
                    }
                });

                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data("{}"));

                log.info("Transformer 计算完成");
                emitter.complete();

            } catch (Exception e) {
                log.error("Transformer 执行出错", e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    log.error("错误处理失败", ex);
                }
            }
        });

        return emitter;
    }

    /**
     * 获取 Transformer 学习文档（Markdown 格式）
     */
    @Operation(summary = "获取学习文档（Markdown）")
    @GetMapping("/guide")
    public Result<String> getGuide() {
        try {
            String content = new String(
                    getClass().getResourceAsStream("/docs/transformer-guide.md").readAllBytes(),
                    StandardCharsets.UTF_8
            );
            return Result.success(content);
        } catch (Exception e) {
            log.error("读取学习文档失败", e);
            return Result.success("");
        }
    }

    /**
     * 获取词表列表（所有支持的词汇）
     */
    @Operation(summary = "获取词表列表")
    @GetMapping("/vocab-list")
    public Result<List<String>> getVocabList() {
        return Result.success(tokenizer.getVocabList());
    }

    /**
     * 分词演示：展示输入文本如何被分词
     */
    @Operation(summary = "分词展示")
    @GetMapping("/tokenize")
    public Result<TokenizeResult> tokenize(@RequestParam String text) {
        List<String> tokens = tokenizer.tokenize(text);
        List<Integer> tokenIds = new ArrayList<>();
        List<String> unknownTokens = new ArrayList<>();

        for (String token : tokens) {
            int tokenId = tokenizer.getTokenId(token);
            tokenIds.add(tokenId);
            if (tokenId == 1) {  // 1 is UNK
                unknownTokens.add(token);
            }
        }

        TokenizeResult result = new TokenizeResult(tokens, tokenIds, unknownTokens);
        return Result.success(result);
    }

    /**
     * 获取词表映射
     */
    @Operation(summary = "获取词表")
    @GetMapping("/vocab")
    public Result<Map<String, Object>> getVocab() {
        Map<String, Object> vocabInfo = new LinkedHashMap<>();
        vocabInfo.put("vocab_size", tokenizer.getVocabSize());
        vocabInfo.put("max_seq_len", tokenizer.getMaxSeqLen());
        vocabInfo.put("vocab_map", tokenizer.getVocabMap());
        return Result.success(vocabInfo);
    }

    /**
     * 获取默认配置
     */
    @Operation(summary = "获取 Transformer 默认配置")
    @GetMapping("/config")
    public Result<TransformerConfig> getConfig() {
        return Result.success(defaultConfig);
    }

    /**
     * SSE 流式接口：仅运行 Encoder，推送完整 Encoder 步骤
     * 最后推送 encoder_complete 事件，包含 encoderOutput 矩阵和 srcTokenIds
     */
    @Operation(summary = "运行 Encoder（SSE 流式）")
    @GetMapping(value = "/run-encoder", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runEncoder(
            @RequestParam String srcText,
            @RequestParam(defaultValue = "16") int embeddingDim,
            @RequestParam(defaultValue = "2") int numHeads,
            @RequestParam(defaultValue = "1") int numLayers
    ) {
        SseEmitter emitter = new SseEmitter(120_000L);

        Executors.newVirtualThreadPerTaskExecutor().execute(() -> {
            try {
                int[] srcTokenIds = tokenizer.encode(srcText);
                TransformerConfig config = defaultConfig.withOverrides(embeddingDim, numHeads, numLayers);
                config.validate();

                log.info("开始运行 Encoder：srcText={}, embedding_dim={}, num_heads={}, num_layers={}",
                        srcText, embeddingDim, numHeads, numLayers);

                ObjectMapper objectMapper = new ObjectMapper();
                double[][] encoderOutput = transformerService.computeEncoderOnlyStreaming(srcTokenIds, config, step -> {
                    try {
                        String json = objectMapper.writeValueAsString(step);
                        emitter.send(SseEmitter.event()
                                .id(System.currentTimeMillis() + "")
                                .name("step")
                                .data(json));
                        Thread.sleep(500);
                    } catch (IOException | InterruptedException e) {
                        log.error("SSE 推送步骤失败", e);
                        throw new RuntimeException(e);
                    }
                });

                // 推送 encoder_complete 事件，包含 encoderOutput 矩阵
                Map<String, Object> completeData = new LinkedHashMap<>();
                completeData.put("encoderOutput", encoderOutput);
                completeData.put("srcTokenIds", srcTokenIds);
                String completeJson = objectMapper.writeValueAsString(completeData);

                emitter.send(SseEmitter.event()
                        .name("encoder_complete")
                        .data(completeJson));

                log.info("Encoder 计算完成");
                emitter.complete();

            } catch (Exception e) {
                log.error("Encoder 执行出错", e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    log.error("错误处理失败", ex);
                }
            }
        });

        return emitter;
    }

    /**
     * SSE 流式接口：仅运行 Decoder，推送完整 Decoder 步骤
     * 需要从 Encoder 调用获得的 encoderOutput 和 srcTokenIds
     */
    @Operation(summary = "运行 Decoder（SSE 流式）")
    @GetMapping(value = "/run-decoder", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runDecoder(
            @RequestParam String tgtText,
            @RequestParam String encoderOutputJson,
            @RequestParam String srcTokenIdsJson,
            @RequestParam(defaultValue = "16") int embeddingDim,
            @RequestParam(defaultValue = "2") int numHeads,
            @RequestParam(defaultValue = "1") int numLayers
    ) {
        SseEmitter emitter = new SseEmitter(120_000L);

        Executors.newVirtualThreadPerTaskExecutor().execute(() -> {
            try {
                ObjectMapper mapper = new ObjectMapper();

                // 解析 encoderOutput 矩阵
                double[][] encoderOutput = mapper.readValue(encoderOutputJson, double[][].class);
                // 解析 srcTokenIds 数组
                int[] srcTokenIds = mapper.readValue(srcTokenIdsJson, int[].class);
                int[] tgtTokenIds = tokenizer.encode(tgtText);

                TransformerConfig config = defaultConfig.withOverrides(embeddingDim, numHeads, numLayers);
                config.validate();

                log.info("开始运行 Decoder：tgtText={}, embedding_dim={}, num_heads={}, num_layers={}",
                        tgtText, embeddingDim, numHeads, numLayers);

                transformerService.computeDecoderOnlyStreaming(tgtTokenIds, encoderOutput, srcTokenIds, config, step -> {
                    try {
                        String json = mapper.writeValueAsString(step);
                        emitter.send(SseEmitter.event()
                                .id(System.currentTimeMillis() + "")
                                .name("step")
                                .data(json));
                        Thread.sleep(500);
                    } catch (IOException | InterruptedException e) {
                        log.error("SSE 推送步骤失败", e);
                        throw new RuntimeException(e);
                    }
                });

                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data("{}"));

                log.info("Decoder 计算完成");
                emitter.complete();

            } catch (Exception e) {
                log.error("Decoder 执行出错", e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    log.error("错误处理失败", ex);
                }
            }
        });

        return emitter;
    }

    /**
     * SSE 流式接口：自回归 Decoder 生成
     * 从 <BOS> 开始，逐步生成 token 直到 <EOS>
     */
    @Operation(summary = "自回归生成 Token（SSE 流式）")
    @GetMapping(value = "/run-autoregressive", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runAutoRegressive(
            @RequestParam String encoderOutputJson,
            @RequestParam String srcTokenIdsJson,
            @RequestParam(defaultValue = "16") int embeddingDim,
            @RequestParam(defaultValue = "2") int numHeads,
            @RequestParam(defaultValue = "1") int numLayers
    ) {
        SseEmitter emitter = new SseEmitter(120_000L);

        Executors.newVirtualThreadPerTaskExecutor().execute(() -> {
            try {
                ObjectMapper mapper = new ObjectMapper();

                // 解析 encoderOutput 矩阵
                double[][] encoderOutput = mapper.readValue(encoderOutputJson, double[][].class);
                // 解析 srcTokenIds 数组
                int[] srcTokenIds = mapper.readValue(srcTokenIdsJson, int[].class);

                TransformerConfig config = defaultConfig.withOverrides(embeddingDim, numHeads, numLayers);
                config.validate();

                log.info("开始自回归生成：embedding_dim={}, num_heads={}, num_layers={}",
                        embeddingDim, numHeads, numLayers);

                ObjectMapper objectMapper = new ObjectMapper();
                transformerService.computeAutoRegressiveStreaming(srcTokenIds, encoderOutput, config, step -> {
                    try {
                        String json = objectMapper.writeValueAsString(step);
                        emitter.send(SseEmitter.event()
                                .id(System.currentTimeMillis() + "")
                                .name("step")
                                .data(json));
                        Thread.sleep(500);
                    } catch (IOException | InterruptedException e) {
                        log.error("SSE 推送步骤失败", e);
                        throw new RuntimeException(e);
                    }
                });

                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data("{}"));

                log.info("自回归生成完成");
                emitter.complete();

            } catch (Exception e) {
                log.error("自回归生成执行出错", e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    log.error("错误处理失败", ex);
                }
            }
        });

        return emitter;
    }

    /**
     * SSE 流式接口：运行完整 Encoder-Decoder Transformer
     * 接收源序列和目标序列的 token 数组
     */
    @Operation(summary = "运行 Encoder-Decoder Transformer（SSE 流式）")
    @GetMapping(value = "/run-seq2seq", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runSeq2Seq(
            @RequestParam(required = false) String srcTokensJson,
            @RequestParam(required = false) String tgtTokensJson,
            @RequestParam(required = false, defaultValue = "false") boolean useTextEncoding,
            @RequestParam(defaultValue = "16") int embeddingDim,
            @RequestParam(defaultValue = "2") int numHeads,
            @RequestParam(defaultValue = "1") int numLayers
    ) {
        SseEmitter emitter = new SseEmitter(120_000L);

        Executors.newVirtualThreadPerTaskExecutor().execute(() -> {
            try {
                int[] srcTokenIds;
                int[] tgtTokenIds;

                ObjectMapper mapper = new ObjectMapper();
                String[] srcTokenArray = mapper.readValue(srcTokensJson, String[].class);
                String[] tgtTokenArray = mapper.readValue(tgtTokensJson, String[].class);

                if (useTextEncoding) {
                    // 新方式：数组中的元素是文本，后端调用 encode() 方法分词
                    // srcTokenArray[0] 和 tgtTokenArray[0] 是完整的文本
                    srcTokenIds = tokenizer.encode(srcTokenArray[0]);
                    tgtTokenIds = tokenizer.encode(tgtTokenArray[0]);
                } else {
                    // 旧方式：数组中的元素是词，后端逐个 getTokenId()
                    srcTokenIds = new int[srcTokenArray.length];
                    for (int i = 0; i < srcTokenArray.length; i++) {
                        srcTokenIds[i] = tokenizer.getTokenId(srcTokenArray[i]);
                    }

                    tgtTokenIds = new int[tgtTokenArray.length];
                    for (int i = 0; i < tgtTokenArray.length; i++) {
                        tgtTokenIds[i] = tokenizer.getTokenId(tgtTokenArray[i]);
                    }
                }

                TransformerConfig config = defaultConfig.withOverrides(embeddingDim, numHeads, numLayers);
                config.validate();

                log.info("开始运行 Encoder-Decoder：src={}, tgt={}, embedding_dim={}, num_heads={}, num_layers={}",
                        java.util.Arrays.toString(srcTokenIds), java.util.Arrays.toString(tgtTokenIds),
                        embeddingDim, numHeads, numLayers);

                ObjectMapper objectMapper = new ObjectMapper();
                transformerService.computeEncoderDecoderStreaming(srcTokenIds, tgtTokenIds, config, step -> {
                    try {
                        String json = objectMapper.writeValueAsString(step);
                        emitter.send(SseEmitter.event()
                                .id(System.currentTimeMillis() + "")
                                .name("step")
                                .data(json));
                        Thread.sleep(500);
                    } catch (IOException | InterruptedException e) {
                        log.error("SSE 推送步骤失败", e);
                        throw new RuntimeException(e);
                    }
                });

                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data("{}"));

                log.info("Encoder-Decoder 计算完成");
                emitter.complete();

            } catch (Exception e) {
                log.error("Encoder-Decoder 执行出错", e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    log.error("错误处理失败", ex);
                }
            }
        });

        return emitter;
    }
}
