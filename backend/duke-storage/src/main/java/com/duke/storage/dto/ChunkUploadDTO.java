package com.duke.storage.dto;

import lombok.Data;

/**
 * 分片上传DTO
 */
@Data
public class ChunkUploadDTO {
    
    /**
     * 全局唯一文件任务ID
     */
    private String chunkId;
    
    /**
     * 原始文件名
     */
    private String fileName;
    
    /**
     * 当前分片序号，从1开始
     */
    private Integer chunkIndex;
    
    /**
     * 总分片数
     */
    private Integer chunkTotal;
    
    /**
     * 单分片大小
     */
    private Long chunkSize;
    
    /**
     * 文件总大小
     */
    private Long fileSize;
}
