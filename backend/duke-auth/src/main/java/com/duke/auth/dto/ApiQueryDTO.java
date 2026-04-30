package com.duke.auth.dto;

import com.duke.framework.dto.PageDTO;

import lombok.Data;

/**
 * API 查询 DTO
 */
@Data
public class ApiQueryDTO extends PageDTO {
    private String appId;
    private String controllerClass;
    private String keyword;
    private Integer status;
}
