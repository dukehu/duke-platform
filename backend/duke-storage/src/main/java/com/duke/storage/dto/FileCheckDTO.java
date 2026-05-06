package com.duke.storage.dto;

import lombok.Data;

/**
 * 秒传校验DTO
 */
@Data
public class FileCheckDTO {
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件大小
     */
    private Long fileSize;
}
