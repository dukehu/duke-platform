package com.duke.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 角色API关联 */
@Data
@TableName("sys_role_api")
public class SysRoleApi {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roleId;
    private Long apiId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
