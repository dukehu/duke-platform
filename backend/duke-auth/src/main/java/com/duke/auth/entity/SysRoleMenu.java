package com.duke.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 角色菜单关联 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roleId;
    private Long menuId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
