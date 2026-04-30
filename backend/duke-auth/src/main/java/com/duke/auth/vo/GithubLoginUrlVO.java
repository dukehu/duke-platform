package com.duke.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * GitHub 登录授权 URL 响应
 */
@Data
@AllArgsConstructor
public class GithubLoginUrlVO {
    private String url;
}
