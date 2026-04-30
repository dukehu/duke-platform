package com.duke.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API实体
 */
@Data
@TableName("sys_api")
public class SysApi {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appId;
    private String controllerClass;
    private String controllerName;
    private String apiName;
    private String apiPath;
    private String apiMethod;
    private String apiDesc;
    private String permission;
    private Integer status;

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
