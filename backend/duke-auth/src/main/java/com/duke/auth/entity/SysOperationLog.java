package com.duke.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String module;
    private String operation;
    private String method;
    private String requestUrl;
    private String requestMethod;
    private String requestParams;
    @TableField("response_data")
    private String responseResult;
    private Long costTime;
    @TableField("operator")
    private String operatorName;
    @TableField("operator_id")
    private Long operatorId;
    @TableField("ip")
    private String operatorIp;
    private Integer status;
    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
