package com.duke.storage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 断点续传分片临时表实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sys_file_chunk")
public class SysFileChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 前端生成全局唯一文件任务ID
     */
    private String chunkId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件后缀
     */
    private String fileSuffix;

    /**
     * 当前分片序号，从1开始
     */
    private Integer chunkIndex;

    /**
     * 总分片数
     */
    private Integer chunkTotal;

    /**
     * 单分片大小字节
     */
    private Long chunkSize;

    /**
     * 文件总大小字节
     */
    private Long fileSize;

    /**
     * 分片临时存放路径
     */
    private String chunkPath;

    /**
     * 存储模式 local/minio
     */
    private String storageMode;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
