package com.duke.storage.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒传结果VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileCheckVO {
    
    /**
     * 是否存在
     */
    private Boolean exists;
    
    /**
     * 文件信息（存在时返回）
     */
    private FileVO fileInfo;
}
