package com.duke.auth.dto;

import com.duke.framework.dto.PageDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 菜单请求 DTO
 */
@Data
public class MenuDTO extends PageDTO {
    private Long id;
    private Long parentId;
    @NotNull(message = "应用ID不能为空")
    private Long appId;
    @NotBlank(message = "菜单名称不能为空")
    private String menuName;
    @NotNull(message = "菜单类型不能为空")
    private Integer menuType;
    private String path;
    private String component;
    private String permission;
    private String icon;
    private Integer sortOrder;
    private Integer visible;
    private Integer status;
}
