package com.duke.demo.service.impl;

import com.duke.demo.config.properties.DocumentPipelineProperties;
import com.duke.demo.document.DocumentParser;
import com.duke.demo.document.DocumentParserFactory;
import com.duke.demo.document.TextChunker;
import com.duke.demo.dto.VectorParamsDTO;
import com.duke.demo.service.IDocumentPipelineService;
import com.duke.demo.service.IQdrantVectorService;
import com.duke.demo.util.QwenEmbeddingUtil;
import com.duke.demo.vo.DocumentProcessResult;
import com.duke.framework.exception.BusinessException;
import com.duke.framework.common.ResultCode;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 文档处理管道服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentPipelineServiceImpl implements IDocumentPipelineService {

    private final DocumentParserFactory parserFactory;
    private final TextChunker textChunker;
    private final QwenEmbeddingUtil qwenEmbeddingUtil;
    private final IQdrantVectorService qdrantVectorService;
    private final DocumentPipelineProperties pipelineProps;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".pdf", ".docx", ".txt", ".md");

    @Override
    public DocumentProcessResult processDocument(MultipartFile file, String collectionName) throws IOException {
        // 1. 校验文件
        validateFile(file);

        String effectiveCollection = collectionName != null && !collectionName.isEmpty()
                ? collectionName
                : pipelineProps.getDefaultCollection();

        String originalName = file.getOriginalFilename();
        String docId = UUID.randomUUID().toString();

        log.info("Processing document: {}, docId: {}, collection: {}", originalName, docId, effectiveCollection);

        try {
            // 2. 确保 Collection 存在（vectorSize=1024）
            ensureCollectionExists(effectiveCollection);

            // 3. 解析文档
            DocumentParser parser = parserFactory.getParser(originalName);
            String rawText = parser.parse(file.getInputStream(), originalName);
            log.info("Document parsed, text length: {}", rawText.length());

            // 4. 分块
            List<String> chunks = textChunker.chunk(rawText);
            int totalChunks = chunks.size();
            log.info("Document chunked into {} parts", totalChunks);

            // 5. 批量向量化 + 入库
            int successChunks = batchVectorizeAndInsert(effectiveCollection, chunks, docId, originalName, totalChunks);

            log.info("Document processing completed: docId={}, totalChunks={}, successChunks={}", docId, totalChunks, successChunks);

            return DocumentProcessResult.builder()
                    .docId(docId)
                    .fileName(originalName)
                    .totalChunks(totalChunks)
                    .successChunks(successChunks)
                    .collectionName(effectiveCollection)
                    .build();

        } catch (Exception e) {
            log.error("Error processing document: {}", originalName, e);
            throw new BusinessException(ResultCode.FAIL.getCode(), "文档处理失败：" + e.getMessage());
        }
    }

    @Override
    public List<IQdrantVectorService.SearchResult> search(String query, String collectionName, int limit, float scoreThreshold) {
        String effectiveCollection = collectionName != null && !collectionName.isEmpty()
                ? collectionName
                : pipelineProps.getDefaultCollection();

        log.info("Searching for: '{}' in collection: {}", query, effectiveCollection);

        try {
            // 查询文本向量化
            List<Float> queryVector = qwenEmbeddingUtil.textToVector(query);

            // 向 Qdrant 搜索
            return qdrantVectorService.search(effectiveCollection, queryVector, limit, scoreThreshold);

        } catch (Exception e) {
            log.error("Search failed for query: {}", query, e);
            throw new BusinessException(ResultCode.FAIL.getCode(), "搜索失败：" + e.getMessage());
        }
    }

    /**
     * 校验上传的文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文件名为空");
        }

        String lowerName = fileName.toLowerCase();
        boolean supported = SUPPORTED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!supported) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不支持的文件类型：" + fileName);
        }
    }

    /**
     * 确保 Collection 存在
     */
    private void ensureCollectionExists(String collectionName) {
        if (!qdrantVectorService.collectionExists(collectionName)) {
            VectorParamsDTO params = new VectorParamsDTO();
            params.setCollectionName(collectionName);
            params.setVectorSize(1024); // 与 qwen3-embedding-8b 维度对齐
            qdrantVectorService.createCollection(params);
            log.info("Collection {} created", collectionName);
        }
    }

    /**
     * 批量向量化并入库（按 batchSize 分批，向量化并行 5 线程）
     */
    private int batchVectorizeAndInsert(String collectionName, List<String> chunks,
                                         String docId, String fileName, int totalChunks) {
        int successCount = 0;
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<List<String>> partitions = Lists.partition(chunks, pipelineProps.getBatchSize());
            int chunkIndex = 0;
            int batchNum = 0;

            for (List<String> batch : partitions) {
                batchNum++;
                try {
                    // 并行向量化
                    List<CompletableFuture<IQdrantVectorService.VectorChunk>> futures = new ArrayList<>();
                    final int baseChunkIndex = chunkIndex;

                    for (int i = 0; i < batch.size(); i++) {
                        final int idx = i;
                        CompletableFuture<IQdrantVectorService.VectorChunk> future =
                                CompletableFuture.supplyAsync(() -> {
                                    String chunkText = batch.get(idx);
                                    List<Float> embedding = qwenEmbeddingUtil.textToVector(chunkText);

                                    Map<String, Object> metadata = new HashMap<>();
                                    metadata.put("docId", docId);
                                    metadata.put("fileName", fileName);
                                    metadata.put("chunkIndex", baseChunkIndex + idx);
                                    metadata.put("totalChunks", totalChunks);

                                    return IQdrantVectorService.VectorChunk.builder()
                                            .text(chunkText)
                                            .embedding(embedding)
                                            .metadata(metadata)
                                            .build();
                                }, executor);
                        futures.add(future);
                    }

                    log.info("[docId={}] Processing batch {}/{} with {} vectors (parallel)", docId, batchNum, partitions.size(), batch.size());

                    // 等待所有向量化完成
                    List<IQdrantVectorService.VectorChunk> vectorChunks = futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());

                    // 批量 upsert
                    qdrantVectorService.batchInsertVectors(collectionName, vectorChunks);
                    successCount += batch.size();
                    chunkIndex += batch.size();

                    log.info("[docId={}] Batch {}/{} completed, total success: {}/{}", docId, batchNum, partitions.size(), successCount, totalChunks);

                } catch (Exception e) {
                    log.error("[docId={}] Error processing batch {}/{}", docId, batchNum, partitions.size(), e);
                }
            }

        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    log.warn("[docId={}] Executor shutdown timeout", docId);
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        return successCount;
    }
}
