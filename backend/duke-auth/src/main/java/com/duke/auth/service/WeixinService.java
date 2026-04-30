package com.duke.auth.service;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.duke.auth.config.properties.WeixinProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 微信开放平台 OAuth 服务，用于 PC 扫码登录（snsapi_login）。
 *
 * <p>登录流程：
 * <ol>
 *   <li>后端生成带 state 的授权 URL（防 CSRF）</li>
 *   <li>前端打开授权 URL，微信 App 扫码确认</li>
 *   <li>微信回调前端 redirect_uri，附带 code 和 state</li>
 *   <li>前端将 code+state 传给后端，后端调用微信 API 换取用户信息</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeixinService {

    private static final String AUTHORIZE_URL =
            "https://open.weixin.qq.com/connect/qrconnect?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_login&state=%s#wechat_redirect";

    private static final String TOKEN_URL =
            "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";

    private static final String USER_INFO_URL =
            "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";

    private final WeixinProperties weixinProperties;

    /** 拼接微信扫码授权 URL，state 由调用方生成并存入 Redis 用于 CSRF 校验 */
    public String buildLoginUrl(String state) {
        return String.format(AUTHORIZE_URL, weixinProperties.getAppId(),
                URLEncoder.encode(weixinProperties.getRedirectUri(), StandardCharsets.UTF_8), state);
    }

    /**
     * 用授权码换取用户信息（两步调用微信 API）：
     * 1. code → access_token + openid + unionid
     * 2. access_token + openid → 用户昵称、头像
     */
    public WeixinUserInfo getUserInfo(String code) {
        String tokenUrl = String.format(TOKEN_URL, weixinProperties.getAppId(), weixinProperties.getAppSecret(), code);
        String tokenResp = HttpUtil.get(tokenUrl);
        JSONObject tokenJson = JSONUtil.parseObj(tokenResp);
        if (tokenJson.containsKey("errcode")) {
            log.error("微信获取 access_token 失败: {}", tokenResp);
            throw new RuntimeException("微信授权失败，请重试");
        }
        String accessToken = tokenJson.getStr("access_token");
        String openid = tokenJson.getStr("openid");

        String userUrl = String.format(USER_INFO_URL, accessToken, openid);
        String userResp = HttpUtil.get(userUrl);
        JSONObject userJson = JSONUtil.parseObj(userResp);

        WeixinUserInfo info = new WeixinUserInfo();
        info.setOpenid(openid);
        info.setUnionid(tokenJson.getStr("unionid"));
        info.setNickname(userJson.getStr("nickname"));
        info.setHeadimgurl(userJson.getStr("headimgurl"));
        return info;
    }

    /** 微信用户基本信息 */
    @Data
    public static class WeixinUserInfo {
        private String openid;
        /** unionid 跨公众号/小程序唯一标识，可能为空（需在开放平台绑定） */
        private String unionid;
        private String nickname;
        private String headimgurl;
    }
}
