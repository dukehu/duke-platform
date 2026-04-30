package com.duke.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ButtonDTO {
    private Long id;
    @NotNull(message = "菜单ID不能为空")
    private Long menuId;
    @NotBlank(message = "按钮名称不能为空")
    private String buttonName;
    @NotBlank(message = "按钮编码不能为空")
    private String buttonCode;
    @NotNull(message = "按钮类型不能为空")
    private Integer buttonType;
    private Integer sortOrder;
    private Integer status;
}
