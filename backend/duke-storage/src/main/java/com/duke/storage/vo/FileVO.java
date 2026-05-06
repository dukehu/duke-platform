package com.duke.storage.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件信息VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileVO {
    
    /**
     * 文件ID
     */
    private Long id;
    
    /**
     * 原始文件名
     */
    private String originalName;
    
    /**
     * 文件后缀
     */
    private String fileSuffix;
    
    /**
     * 文件大小字节
     */
    private Long fileSize;
    
    /**
     * 格式化后的文件大小
     */
    private String formattedSize;
    
    /**
     * 文件mime类型
     */
    private String mimeType;
    
    /**
     * 存储类型
     */
    private String storageMode;
    
    /**
     * 访问预览地址
     */
    private String fileUrl;
    
    /**
     * 文件MD5
     */
    private String fileMd5;
    
    /**
     * 上传时间
     */
    private LocalDateTime createTime;
}
