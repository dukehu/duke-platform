package com.duke.auth.dto;

import com.duke.framework.dto.PageDTO;

import lombok.Data;

/**
 * 操作日志查询 DTO
 */
@Data
public class LogQueryDTO extends PageDTO {
    private String module;
    private String operatorName;
    private Integer status;
    private String startTime;
    private String endTime;
}
