package com.duke.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 角色数据权限部门关联 */
@Data
@TableName("sys_role_dept")
public class SysRoleDept {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roleId;
    private Long deptId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
