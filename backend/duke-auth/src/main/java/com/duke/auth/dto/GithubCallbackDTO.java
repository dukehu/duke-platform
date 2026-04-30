package com.duke.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * GitHub OAuth 回调参数
 */
@Data
public class GithubCallbackDTO {
    @NotBlank(message = "code不能为空")
    private String code;
    @NotBlank(message = "state不能为空")
    private String state;
}
