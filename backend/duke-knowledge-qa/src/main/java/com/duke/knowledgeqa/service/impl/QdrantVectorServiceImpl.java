package com.duke.knowledgeqa.service.impl;

import com.duke.knowledgeqa.dto.VectorParamsDTO;
import com.duke.knowledgeqa.service.IQdrantVectorService;
import com.duke.knowledgeqa.vo.CollectionInfoVO;
import com.google.common.util.concurrent.ListenableFuture;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Common;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@AllArgsConstructor
public class QdrantVectorServiceImpl implements IQdrantVectorService {

    private final QdrantClient qdrantClient;

    @Override
    public CollectionInfoVO listCollection(String collectionName) {
        log.info("Listing collection {}", collectionName);
        try {
            // 集合是否存在
            ListenableFuture<Boolean> existsFuture = qdrantClient.collectionExistsAsync(collectionName);
            if (existsFuture.get()) {
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
            } else {
                log.info("Collection {} does not exist", collectionName);
                return CollectionInfoVO.builder().build();
            }
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

    @SneakyThrows
    @Override
    public void deleteCollection(String collectionName) {
        log.info("Deleting collection {}", collectionName);
        ListenableFuture<Collections.CollectionOperationResponse> response
                = qdrantClient.deleteCollectionAsync(collectionName);
        Collections.CollectionOperationResponse collectionOperationResponse = response.get();
        log.info("Collection {} deleted successfully: {}", collectionName, collectionOperationResponse.getResult());
    }

    @Override
    public boolean collectionExists(String collectionName) {
        return false;
    }

    @Override
    public void insertVector(String collectionName, String text, List<Float> embedding, Map<String, Object> metadata) {
        log.info("Inserting vector {}", text);
        List<Points.PointStruct> points = new ArrayList<>();
        Points.PointStruct point = Points.PointStruct.newBuilder()
                // 唯一ID
                .setId(PointIdFactory.id(UUID.randomUUID()))
                // 向量
                .setVectors(VectorsFactory.vectors(embedding))
                // 存原文
                .putPayload("text", ValueFactory.value(text))
                .putAllPayload(toValueMap(metadata))
                .build();
        points.add(point);
        qdrantClient.upsertAsync(collectionName, points);
    }

    @Override
    public void deleteVector(String collectionName, long id) {
        log.info("Deleting vector {}", id);
        List<Common.PointId> pointIds = new ArrayList<>();
        pointIds.add(PointIdFactory.id(id));
        qdrantClient.deleteAsync(collectionName, pointIds);
    }

    @SneakyThrows
    @Override
    public List<SearchResult> search(String collectionName, List<Float> queryVector, int limit, float scoreThreshold) {
        log.info("Searching collection {} with query vector {}", collectionName, queryVector);
        Points.SearchPoints searchPoints = Points.SearchPoints.newBuilder()
                .setCollectionName(collectionName) // 你漏了这个关键参数！
                .addAllVector(queryVector)
                .setLimit(limit)
                .setScoreThreshold(scoreThreshold) // 加上你定义的阈值
                .setWithVectors(Points.WithVectorsSelector.newBuilder().setEnable(true).build())
                .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                .build();
        ListenableFuture<List<Points.ScoredPoint>> responseFuture = qdrantClient.searchAsync(searchPoints);
        List<Points.ScoredPoint> scoredPoints = responseFuture.get();
        List<SearchResult> searchResults = new ArrayList<>();
        for (Points.ScoredPoint scoredPoint : scoredPoints) {
            Map<String, Object> payload = new HashMap<>();
            Map<String, JsonWithInt.Value> payloadMap = scoredPoint.getPayloadMap();
            for (Map.Entry<String, JsonWithInt.Value> entry : payloadMap.entrySet()) {
                String key = entry.getKey();
                JsonWithInt.Value value = entry.getValue();
                // 把 Value 转成普通类型：String / int / boolean / double
                Object realVal = switch (value.getKindCase()) {
                    case STRING_VALUE -> value.getStringValue();
                    case INTEGER_VALUE -> value.getIntegerValue();
                    case DOUBLE_VALUE -> value.getDoubleValue();
                    case BOOL_VALUE -> value.getBoolValue();
                    default -> null;
                };
                payload.put(key, realVal);
            }
            SearchResult searchResult = SearchResult.builder()
                    .id(scoredPoint.getId().getNum())
                    .score(scoredPoint.getScore())
                    .payload(payload)
                    .build();
            searchResults.add(searchResult);
        }
        return searchResults;
    }

    /**
     * 关键工具方法：把 Map<String, Object> 安全转成 Map<String, Value>
     *
     * @param metadata 元数据
     * @return 转换后的 Map
     */
    private Map<String, JsonWithInt.Value> toValueMap(Map<String, Object> metadata) {
        Map<String, JsonWithInt.Value> valueMap = new HashMap<>();
        if (metadata == null || metadata.isEmpty()) {
            return valueMap;
        }

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                continue; // 跳过 null 值
            }

            // 根据类型调用正确的 ValueFactory 方法，避免 Object 报错
            if (value instanceof String str) {
                valueMap.put(key, ValueFactory.value(str));
            } else if (value instanceof Integer i) {
                valueMap.put(key, ValueFactory.value(i));
            } else if (value instanceof Long l) {
                valueMap.put(key, ValueFactory.value(l));
            } else if (value instanceof Double d) {
                valueMap.put(key, ValueFactory.value(d));
            } else if (value instanceof Boolean b) {
                valueMap.put(key, ValueFactory.value(b));
            } else {
                // 其他类型统一转成字符串
                valueMap.put(key, ValueFactory.value(String.valueOf(value)));
            }
        }
        return valueMap;
    }
}
