package com.duke.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单实体
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;
    private Long appId;
    private String menuName;
    /** 菜单类型：1目录 2菜单 3按钮 */
    private Integer menuType;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Integer visible;
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
