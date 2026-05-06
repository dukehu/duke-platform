package com.duke.storage.dto;

import com.duke.framework.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileQueryDTO extends PageDTO {
    
    /**
     * 文件名关键字
     */
    private String keyword;
    
    /**
     * 文件类型
     */
    private String fileType;
}
