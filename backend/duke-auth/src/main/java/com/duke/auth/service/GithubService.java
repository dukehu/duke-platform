package com.duke.auth.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.duke.auth.config.properties.GithubProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * GitHub OAuth 服务，处理 PC 端 GitHub 授权登录。
 *
 * <p>登录流程：
 * <ol>
 *   <li>后端生成带 state 的授权 URL（防 CSRF）</li>
 *   <li>前端打开授权 URL，用户在 GitHub 授权</li>
 *   <li>GitHub 回调前端 redirect_uri，附带 code 和 state</li>
 *   <li>前端将 code+state 传给后端，后端换取用户信息</li>
 * </ol>
 *
 * <p>与微信登录的主要差异：token 换取使用 POST 请求，user API 使用 Bearer Token。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GithubService {

    private static final String AUTHORIZE_URL =
            "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=user:email&state=%s";

    private static final String TOKEN_URL =
            "https://github.com/login/oauth/access_token";

    private static final String USER_URL =
            "https://api.github.com/user";

    // 代理IP（本地代理默认127.0.0.1）
    private static final String PROXY_HOST = "127.0.0.1";
    private static final int PROXY_PORT = 6174;
    private static final Proxy PROXY = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_HOST, PROXY_PORT));

    private final GithubProperties githubProperties;

    /**
     * 拼接 GitHub OAuth 授权 URL，state 由调用方生成并存入 Redis
     */
    public String buildLoginUrl(String state) {
        return String.format(AUTHORIZE_URL,
                githubProperties.getClientId(),
                URLEncoder.encode(githubProperties.getRedirectUri(), StandardCharsets.UTF_8),
                state);
    }

    /**
     * 用授权码换取用户信息（两步调用 GitHub API）：
     * 1. POST access_token 端点，获取 access_token
     * 2. GET /user，获取用户基本信息
     */
    public GithubUserInfo getUserInfo(String code) {
        // 第一步：code 换 access_token
        // GitHub 要求 POST + Accept: application/json，否则返回 query string 格式
        HttpResponse tokenResp = null;
        try {
            tokenResp = HttpRequest.post(TOKEN_URL)
                    .header("Accept", "application/json")
                    .form("client_id", githubProperties.getClientId())
                    .form("client_secret", githubProperties.getClientSecret())
                    .form("code", code)
                    .setProxy(PROXY) // ========== 新增：添加代理 ==========
                    .timeout(10000)  // ========== 新增：设置10秒超时 ==========
                    .execute();
        } catch (Exception e) {
            log.error("GitHub 请求 access_token 接口失败（网络/代理问题）", e);
            throw new RuntimeException("GitHub 授权连接失败，请检查代理配置");
        }

        JSONObject tokenJson = JSONUtil.parseObj(tokenResp.body());
        if (tokenJson.containsKey("error")) {
            log.error("GitHub 获取 access_token 失败: {}", tokenResp.body());
            throw new RuntimeException("GitHub 授权失败，请重试");
        }
        String accessToken = tokenJson.getStr("access_token");

        // 第二步：access_token 换用户信息
        HttpResponse userResp = null;
        try {
            userResp = HttpRequest.get(USER_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .setProxy(PROXY) // ========== 新增：添加代理 ==========
                    .timeout(10000)  // ========== 新增：设置10秒超时 ==========
                    .execute();
        } catch (Exception e) {
            log.error("GitHub 请求用户信息接口失败（网络/代理问题）", e);
            throw new RuntimeException("获取GitHub用户信息失败，请检查代理配置");
        }

        JSONObject userJson = JSONUtil.parseObj(userResp.body());

        GithubUserInfo info = new GithubUserInfo();
        info.setId(userJson.getLong("id"));
        info.setLogin(userJson.getStr("login"));
        info.setName(userJson.getStr("name"));
        info.setEmail(userJson.getStr("email"));
        info.setAvatarUrl(userJson.getStr("avatar_url"));
        return info;
    }

    /**
     * GitHub 用户基本信息
     */
    @Data
    public static class GithubUserInfo {
        /**
         * GitHub 平台全局唯一用户 ID（数字类型）
         */
        private Long id;
        /**
         * GitHub 登录名（即 @username）
         */
        private String login;
        /**
         * 用户展示名称（可能为空）
         */
        private String name;
        /**
         * 邮箱（用户可能设为私有，可能为空）
         */
        private String email;
        /**
         * 头像 URL
         */
        private String avatarUrl;
    }
}