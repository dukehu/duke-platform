package com.duke.storage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件主表实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sys_file")
public class SysFile {

    @TableId(type = IdType.AUTO)
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
     * 文件mime类型
     */
    private String mimeType;

    /**
     * 存储类型 local / minio
     */
    private String storageMode;

    /**
     * 物理存储路径
     */
    private String savePath;

    /**
     * 访问预览地址
     */
    private String fileUrl;

    /**
     * 后端计算完整文件MD5，用于秒传去重
     */
    private String fileMd5;

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

    /**
     * 删除标识 0-未删除 1-已删除
     */
    @TableLogic
    private Integer deleted;
}
