package com.duke.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户部门关联 */
@Data
@TableName("sys_user_dept")
public class SysUserDept {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long deptId;
    /** 是否主部门：1是 0否 */
    private Integer isPrimary;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
