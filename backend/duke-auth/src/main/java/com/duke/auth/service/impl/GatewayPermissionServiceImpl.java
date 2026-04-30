package com.duke.auth.service.impl;

import com.duke.framework.common.Constants;
import com.duke.auth.mapper.SysApiMapper;
import com.duke.auth.mapper.SysRoleMapper;
import com.duke.auth.service.IGatewayPermissionService;
import com.duke.auth.vo.ApiRuleVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayPermissionServiceImpl implements IGatewayPermissionService {

    private final SysApiMapper apiMapper;
    private final SysRoleMapper roleMapper;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    // 内存中的 API 规则，启动时加载，sync() 后刷新，volatile 保证可见性
    private volatile List<ApiRuleVO> cachedRules = Collections.emptyList();

    @PostConstruct
    public void init() {
        refreshRules();
    }


    @Override
    public void refreshRules() {
        try {
            List<ApiRuleVO> rules = apiMapper.selectApiRulesByAppId(null);
            cachedRules = Collections.unmodifiableList(rules);
            log.info("API 规则缓存已刷新，共 {} 条", rules.size());
        } catch (Exception e) {
            log.error("刷新 API 规则缓存失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean checkPermission(Long userId, String appId, String path, String httpMethod) {
        // 1. 从内存规则中匹配当前请求
        List<ApiRuleVO> rules = cachedRules;
        if (appId != null) {
            rules = rules.stream()
                    .filter(r -> appId.equals(r.getAppId()))
                    .toList();
        }

        Optional<ApiRuleVO> matched = matchRule(rules, httpMethod, path);

        // 无规则 → 放行（该接口未在 sys_api 中注册，视为公开）
        if (matched.isEmpty()) return true;

        ApiRuleVO rule = matched.get();
        // permission 为空 → 公开 API，放行
        if (rule.getPermission() == null || rule.getPermission().isBlank()) return true;

        // 2. 超级管理员直接放行
        if (roleMapper.countSuperAdminByUserId(userId, Constants.SUPER_ADMIN_ROLE) > 0) return true;

        // 3. 检查用户是否拥有所需权限
        return apiMapper.selectApiPermissionsByUserId(userId).contains(rule.getPermission());
    }

    /**
     * 路径匹配：精确路径优先于含路径变量的模板
     * AntPathMatcher 原生支持 {variable}，如 /user/{id} 匹配 /user/123
     */
    private Optional<ApiRuleVO> matchRule(List<ApiRuleVO> rules, String method, String path) {
        // 1. 精确匹配（不含路径变量）
        Optional<ApiRuleVO> exact = rules.stream()
                .filter(r -> method.equalsIgnoreCase(r.getApiMethod()))
                .filter(r -> !r.getApiPath().contains("{"))
                .filter(r -> r.getApiPath().equals(path))
                .findFirst();
        if (exact.isPresent()) return exact;

        // 2. 模板匹配（含路径变量，如 /user/{id}）
        return rules.stream()
                .filter(r -> method.equalsIgnoreCase(r.getApiMethod()))
                .filter(r -> r.getApiPath().contains("{"))
                .filter(r -> PATH_MATCHER.match(r.getApiPath(), path))
                .findFirst();
    }
}
