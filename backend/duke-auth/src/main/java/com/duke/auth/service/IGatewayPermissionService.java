package com.duke.auth.service;

public interface IGatewayPermissionService {

    /**
     * 校验用户是否有权限访问指定接口
     *
     * @param userId     用户 ID（来自网关 JWT 解析）
     * @param appId      目标服务 ID
     * @param path       服务路径（已去掉网关前缀，如 /user/123）
     * @param httpMethod HTTP 方法（GET/POST/PUT/DELETE）
     * @return true=允许访问，false=拒绝
     */
    boolean checkPermission(Long userId, String appId, String path, String httpMethod);

    /** 刷新内存中的 API 规则缓存（sync() 完成后调用） */
    void refreshRules();
}
