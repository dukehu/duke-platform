package com.duke.knowledgeqa.service.impl;

import com.duke.knowledgeqa.dto.VectorParamsDTO;
import com.duke.knowledgeqa.service.IQdrantVectorService;
import com.duke.knowledgeqa.util.QwenEmbeddingUtil;
import com.duke.knowledgeqa.vo.CollectionInfoVO;
import com.google.common.util.concurrent.ListenableFuture;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@AllArgsConstructor
public class QdrantVectorServiceImpl implements IQdrantVectorService {

    private final QdrantClient qdrantClient;
    private final QwenEmbeddingUtil qwenEmbeddingUtil;

    @Override
    public CollectionInfoVO listCollection(String collectionName) {
        log.info("Listing collection {}", collectionName);
        try {
            ListenableFuture<Collections.CollectionInfo> collectionFuture = qdrantClient.getCollectionInfoAsync(collectionName);
            Collections.CollectionInfo info = collectionFuture.get();
            CollectionInfoVO collectionInfoVO = CollectionInfoVO.builder()
                    .collectionName(collectionName)
                    .status(info.getStatus().name())
                    .vectorSize(info.getConfig().getParams().getVectorsConfig().getParams().getSize())
                    .distance(info.getConfig().getParams().getVectorsConfig().getParams().getDistance().name())
                    .build();
            log.info("Collection {} info: {}", collectionName, collectionInfoVO);
            return collectionInfoVO;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CollectionInfoVO.builder().build();
        } catch (ExecutionException e) {
            return CollectionInfoVO.builder().build();
        }
    }

    @SneakyThrows
    @Override
    public CollectionInfoVO createCollection(VectorParamsDTO params) {
        String collectionName = params.getCollectionName();
        Integer vectorSize = params.getVectorSize();
        log.info("Creating collection {} with vector size {}", collectionName, vectorSize);

        // 是否存在，存在返回
        try {
            CollectionInfoVO existingCollection = listCollection(collectionName);
            if (existingCollection != null && !ObjectUtils.isEmpty(existingCollection.getCollectionName())) {
                log.info("Collection {} already exists", collectionName);
                return existingCollection;
            }
        } catch (RuntimeException e) {
            log.info("Collection {} does not exist, will create it", collectionName);
        }

        // 向量参数
        Collections.VectorParams vectorParams = Collections.VectorParams.newBuilder()
                .setSize(vectorSize)        // 维度
                .setDistance(Collections.Distance.Cosine) // 相似度算法
                .build();

        // 创建集合
        ListenableFuture<Collections.CollectionOperationResponse> responseFuture = qdrantClient.createCollectionAsync(collectionName, vectorParams);
        Collections.CollectionOperationResponse collectionOperationResponse = responseFuture.get();
        log.info("Collection {} created successfully: {}", collectionName, collectionOperationResponse.getResult());

        // 创建后获取集合信息并返回
        return listCollection(collectionName);
    }

    @Override
    public void deleteCollection(String collectionName) {
        
    }

    @Override
    public void insertVector(String collectionName, String text) {
        log.info("Inserting vector {}", text);
        List<Points.PointStruct> points = new ArrayList<>();
        Points.PointStruct point = Points.PointStruct.newBuilder()
                .setId(PointIdFactory.id(UUID.randomUUID())) // 唯一ID
                .setVectors(VectorsFactory.vectors(qwenEmbeddingUtil.textToVector(text)))            // 你的Qwen3向量
                .putPayload("text", ValueFactory.value(text))          // 存原文
                .build();
        points.add(point);
        qdrantClient.upsertAsync(collectionName, points);
    }
}
