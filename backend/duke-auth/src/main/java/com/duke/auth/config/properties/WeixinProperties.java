package com.duke.auth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 微信开放平台配置，对应 application.yml 中的 weixin.* 前缀。
 * 适用于 PC 扫码登录（snsapi_login scope）。
 * appId / appSecret 需在微信开放平台申请，生产环境通过环境变量注入。
 */
@Data
@ConfigurationProperties(prefix = "weixin")
public class WeixinProperties {
    /** 微信开放平台 AppID */
    private String appId;
    /** 微信开放平台 AppSecret，禁止硬编码到代码仓库 */
    private String appSecret;
    /** 微信回调地址，需在开放平台白名单中注册 */
    private String redirectUri = "http://localhost:3000/weixin/callback";
}
