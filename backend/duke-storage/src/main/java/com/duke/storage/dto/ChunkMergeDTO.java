package com.duke.storage.dto;

import lombok.Data;

/**
 * 分片合并DTO
 */
@Data
public class ChunkMergeDTO {
    
    /**
     * 全局唯一文件任务ID
     */
    private String chunkId;
    
    /**
     * 原始文件名
     */
    private String fileName;
    
    /**
     * 总分片数
     */
    private Integer chunkTotal;
    
    /**
     * 文件总大小
     */
    private Long fileSize;
}
