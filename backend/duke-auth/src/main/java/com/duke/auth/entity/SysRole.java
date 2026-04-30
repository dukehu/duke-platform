package com.duke.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体
 */
@Data
@TableName("sys_role")
public class SysRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long appId;
    private String roleCode;
    private String roleName;
    private String roleDesc;
    /** 数据权限：1全部 2自定义 3本部门 4本部门及下级 5仅本人 */
    private Integer dataScope;
    private Integer status;
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
