package com.duke.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WeixinCallbackDTO {
    @NotBlank(message = "code不能为空")
    private String code;
    @NotBlank(message = "state不能为空")
    private String state;
}
