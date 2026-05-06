package com.duke.demo.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 文档处理结果 VO
 */
@Data
@Builder
public class DocumentProcessResult {

    // 文档 ID
    private String docId;

    // 原始文件名
    private String fileName;

    // 总分块数
    private int totalChunks;

    // 成功入库的分块数
    private int successChunks;

    // 集合名
    private String collectionName;
}
