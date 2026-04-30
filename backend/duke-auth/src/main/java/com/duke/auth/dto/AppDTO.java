package com.duke.auth.dto;

import com.duke.framework.dto.PageDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 应用请求 DTO
 */
@Data
public class AppDTO extends PageDTO {
    private Long id;
    @NotBlank(message = "应用编码不能为空")
    private String appCode;
    @NotBlank(message = "应用名称不能为空")
    private String appName;
    private String appDesc;
    private Integer status;
    private Integer sortOrder;
    private String keyword;
}
