package com.duke.demo.service;

import com.duke.demo.vo.DocumentProcessResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文档处理管道服务接口
 * 负责完整流程：解析 → 分块 → 向量化 → 存储
 */
public interface IDocumentPipelineService {

    /**
     * 上传并全流程处理文档
     *
     * @param file           上传的文件
     * @param collectionName 目标集合名（null 时使用默认值）
     * @return 处理结果摘要
     * @throws IOException 处理异常
     */
    DocumentProcessResult processDocument(MultipartFile file, String collectionName) throws IOException;

    /**
     * 语义搜索
     *
     * @param query          查询文本
     * @param collectionName 目标集合名
     * @param limit          返回条数
     * @param scoreThreshold 相似度阈值
     * @return 搜索结果列表
     */
    List<IQdrantVectorService.SearchResult> search(String query, String collectionName, int limit, float scoreThreshold);
}
