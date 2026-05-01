package com.duke.knowledgeqa.service;

import com.duke.knowledgeqa.dto.VectorParamsDTO;
import com.duke.knowledgeqa.vo.CollectionInfoVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Qdrant 向量数据库服务接口 spring-ai-qdrant代替
 * 提供向量的增删改查操作
 */
public interface IQdrantVectorService {

    /**
     * 查询集合（表）
     *
     * @param collectionName 集合名称
     */
    CollectionInfoVO listCollection(String collectionName);

    /**
     * 创建集合（表）
     *
     * @param params 向量参数
     */
    CollectionInfoVO createCollection(VectorParamsDTO params);

    /**
     * 删除集合
     *
     * @param collectionName 集合名称
     */
    void deleteCollection(String collectionName);

    /**
     * 检查集合是否存在
     *
     * @param collectionName 集合名称
     * @return true 存在，false 不存在
     */
    boolean collectionExists(String collectionName);

    /**
     * 插入单条向量
     *
     * @param collectionName 集合名
     * @param text           原始文本
     * @param embedding      向量数组
     * @param metadata       自定义元数据
     */
    void insertVector(String collectionName, String text, List<Float> embedding, Map<String, Object> metadata);


    /**
     * 删除向量
     *
     * @param collectionName 集合名称
     * @param id             向量 ID
     */
    void deleteVector(String collectionName, long id);

    /**
     * 向量相似度搜索
     *
     * @param collectionName 集合名称
     * @param queryVector    查询向量
     * @param limit          返回结果数量
     * @param scoreThreshold 相似度阈值（0-1）
     * @return 搜索结果列表
     */
    List<SearchResult> search(String collectionName, List<Float> queryVector, int limit, float scoreThreshold);

    /**
     * 搜索结果 DTO
     */
    @Builder
    @Data
    class SearchResult {
        public long id;
        public float score;
        public Map<String, Object> payload;
    }
}
