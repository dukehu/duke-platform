package com.duke.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 部门请求 DTO
 */
@Data
public class DeptDTO {
    private Long id;
    private Long parentId;
    @NotBlank(message = "部门名称不能为空")
    private String deptName;
    private String deptCode;
    private String leader;
    private String phone;
    private String email;
    private Integer sortOrder;
    private Integer status;
}
