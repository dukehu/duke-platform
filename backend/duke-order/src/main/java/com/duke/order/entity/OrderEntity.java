package com.duke.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Order 瀹炰綋
 * 褰?useDatabase=n 鏃跺彲鍒犻櫎姝ゆ枃浠跺強 mapper 鍖? */
@Data
@TableName("order")
public class OrderEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    // TODO

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



