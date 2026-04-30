package com.duke.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门实体
 */
@Data
@TableName("sys_dept")
public class SysDept {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;
    private String deptName;
    private String deptCode;
    private String leader;
    private String phone;
    private String email;
    private Integer sortOrder;
    private Integer status;
    /** 祖级ID列表，逗号分隔 */
    private String ancestors;

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
