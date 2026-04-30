package com.duke.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 按钮实体
 */
@Data
@TableName("sys_button")
public class SysButton {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long menuId;
    private String buttonName;
    private String buttonCode;
    /** 按钮类型：1头部 2行操作 */
    private Integer buttonType;
    private Integer sortOrder;
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
