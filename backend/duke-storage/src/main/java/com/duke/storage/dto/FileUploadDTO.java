package com.duke.storage.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传DTO
 */
@Data
public class FileUploadDTO {
    
    /**
     * 文件
     */
    private MultipartFile file;
}
