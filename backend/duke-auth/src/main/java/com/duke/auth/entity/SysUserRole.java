package com.duke.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户角色关联 */
@Data
@TableName("sys_user_role")
public class SysUserRole {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long roleId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
