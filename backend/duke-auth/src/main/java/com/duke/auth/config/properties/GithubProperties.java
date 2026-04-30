package com.duke.auth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GitHub OAuth 应用配置，对应 application.yml 中的 github.* 前缀。
 * 需在 GitHub Developer Settings → OAuth Apps 中申请，
 * clientId/clientSecret 通过环境变量注入，禁止硬编码。
 */
@Data
@ConfigurationProperties(prefix = "github")
public class GithubProperties {
    /** GitHub OAuth App Client ID */
    private String clientId;
    /** GitHub OAuth App Client Secret，禁止提交到代码仓库 */
    private String clientSecret;
    /** 授权回调地址，需在 GitHub OAuth App 中注册 */
    private String redirectUri = "http://localhost:3000/auth/github/callback";
}
