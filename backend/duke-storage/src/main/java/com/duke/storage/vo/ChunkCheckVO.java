package com.duke.storage.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分片检查结果VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChunkCheckVO {
    
    /**
     * 已上传的分片序号列表
     */
    private List<Integer> uploadedChunks;
    
    /**
     * 是否全部上传完成
     */
    private Boolean completed;
}
