package com.duke.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role_button")
public class SysRoleButton {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roleId;
    private Long buttonId;
}
