package com.duke.knowledgeqa.service;

import com.duke.knowledgeqa.dto.VectorParamsDTO;
import com.duke.knowledgeqa.vo.CollectionInfoVO;

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

//    /**
//     * 删除集合
//     *
//     * @param collectionName 集合名称
//     */
//    void deleteCollection(String collectionName);
//
//    /**
//     * 检查集合是否存在
//     *
//     * @param collectionName 集合名称
//     * @return true 存在，false 不存在
//     */
//    boolean collectionExists(String collectionName);
//

    /**
     * 插入向量
     *
     * @param collectionName 集合名称
     * @param text           文本
     */
    void insertVector(String collectionName, String text);
//
//    /**
//     * 批量插入向量
//     *
//     * @param collectionName 集合名称
//     * @param vectors        向量列表，每个元素包含 id、vector、payload
//     */
//    void batchInsertVectors(String collectionName, List<VectorData> vectors);
//
//    /**
//     * 删除向量
//     *
//     * @param collectionName 集合名称
//     * @param id             向量 ID
//     */
//    void deleteVector(String collectionName, long id);
//
//    /**
//     * 批量删除向量
//     *
//     * @param collectionName 集合名称
//     * @param ids            向量 ID 列表
//     */
//    void batchDeleteVectors(String collectionName, List<Long> ids);
//
//    /**
//     * 更新向量
//     *
//     * @param collectionName 集合名称
//     * @param id             向量 ID
//     * @param vector         新的向量数据
//     * @param payload        新的元数据
//     */
//    void updateVector(String collectionName, long id, List<Float> vector, java.util.Map<String, Object> payload);
//
//    /**
//     * 向量相似度搜索
//     *
//     * @param collectionName 集合名称
//     * @param queryVector    查询向量
//     * @param limit          返回结果数量
//     * @param scoreThreshold 相似度阈值（0-1）
//     * @return 搜索结果列表
//     */
//    List<SearchResult> search(String collectionName, List<Float> queryVector, int limit, float scoreThreshold);
//
//    /**
//     * 向量数据 DTO
//     */
//    class VectorData {
//        public long id;
//        public List<Float> vector;
//        public java.util.Map<String, Object> payload;
//
//        public VectorData(long id, List<Float> vector, java.util.Map<String, Object> payload) {
//            this.id = id;
//            this.vector = vector;
//            this.payload = payload;
//        }
//    }
//
//    /**
//     * 搜索结果 DTO
//     */
//    class SearchResult {
//        public long id;
//        public float score;
//        public java.util.Map<String, Object> payload;
//
//        public SearchResult(long id, float score, java.util.Map<String, Object> payload) {
//            this.id = id;
//            this.score = score;
//            this.payload = payload;
//        }
//    }
}
