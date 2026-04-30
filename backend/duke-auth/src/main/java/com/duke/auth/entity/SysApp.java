package com.duke.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用实体
 */
@Data
@TableName("sys_app")
public class SysApp {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 应用编码 */
    private String appCode;

    /** 应用名称 */
    private String appName;

    /** 应用描述 */
    private String appDesc;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 排序 */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
