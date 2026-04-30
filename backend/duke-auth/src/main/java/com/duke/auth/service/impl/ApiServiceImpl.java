package com.duke.auth.service.impl;

import com.duke.framework.common.PageResult;
import com.duke.auth.dto.ApiQueryDTO;
import com.duke.auth.entity.SysApi;
import com.duke.auth.event.ApiSyncCompletedEvent;
import com.duke.auth.util.ApiScanner;
import com.duke.framework.exception.BusinessException;
import com.duke.auth.mapper.SysApiMapper;
import com.duke.auth.service.IApiService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * API 接口管理服务。
 * 中央 API 管理服务，扫描 duke-auth 和其他微服务的接口元数据。
 * 应用启动完成后自动扫描 duke-auth 的接口，其他服务可通过手动触发扫描。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiServiceImpl implements IApiService {

    private final SysApiMapper apiMapper;
    private final ApplicationContext applicationContext;

    /**
     * 应用启动完成后自动扫描 duke-auth 的接口
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            syncLocal();
        } catch (Exception e) {
            log.warn("API 自动同步失败: {}", e.getMessage());
        }
    }

    /**
     * 扫描本地应用（duke-auth）的接口
     */
    private void syncLocal() {
        String appId = "duke-auth";
        log.info("开始扫描本地应用 {} 的接口...", appId);

        ApiScanner scanner = new ApiScanner(applicationContext, appId);
        List<ApiScanner.ApiMetadata> scanned = scanner.scanAll();

        syncApis(scanned);
    }

    /**
     * 扫描并同步指定应用的接口（包括其他微服务）
     */
    @Override
    public void syncAppApis(String appId) {
        log.info("开始扫描应用 {} 的接口...", appId);

        // 删除该应用的所有旧 API 记录
        apiMapper.delete(new LambdaQueryWrapper<SysApi>()
                .eq(SysApi::getAppId, appId));

        // 调用其他服务的 API 元数据接口获取数据
        // 暂时简化实现，实际场景可通过网关调用
        log.info("应用 {} 的接口同步完成（需通过网关或服务发现调用）", appId);

        // 发布事件
        applicationContext.publishEvent(new ApiSyncCompletedEvent(this));
    }

    /**
     * 同步 API 列表到数据库
     */
    private void syncApis(List<ApiScanner.ApiMetadata> scanned) {
        // 一次性加载库中所有 API
        Map<String, SysApi> existingMap = apiMapper.selectList(null).stream()
                .collect(Collectors.toMap(
                        a -> a.getApiMethod() + ":" + a.getApiPath(),
                        a -> a,
                        (a, b) -> a));

        List<SysApi> toInsert = new ArrayList<>();
        List<SysApi> toUpdate = new ArrayList<>();

        for (ApiScanner.ApiMetadata metadata : scanned) {
            SysApi existing = existingMap.get(metadata.getApiMethod() + ":" + metadata.getApiPath());
            if (existing == null) {
                SysApi api = new SysApi();
                api.setAppId(metadata.getAppId());
                api.setControllerClass(metadata.getControllerClass());
                api.setControllerName(metadata.getControllerName());
                api.setApiName(metadata.getApiName());
                api.setApiPath(metadata.getApiPath());
                api.setApiMethod(metadata.getApiMethod());
                api.setApiDesc(metadata.getApiDesc());
                api.setPermission(metadata.getPermission());
                api.setStatus(1);
                toInsert.add(api);
            } else {
                existing.setApiName(metadata.getApiName());
                existing.setControllerName(metadata.getControllerName());
                existing.setApiDesc(metadata.getApiDesc());
                existing.setPermission(metadata.getPermission());
                toUpdate.add(existing);
            }
        }

        if (!toInsert.isEmpty()) {
            com.baomidou.mybatisplus.extension.toolkit.Db.saveBatch(toInsert);
        }
        toUpdate.forEach(api -> apiMapper.updateById(api));

        log.info("API 同步完成，新增 {}，更新 {}，共扫描 {} 个接口",
                toInsert.size(), toUpdate.size(), scanned.size());

        // 同步完成后通知网关权限服务刷新缓存
        applicationContext.publishEvent(new ApiSyncCompletedEvent(this));
    }

    @Override
    public PageResult<SysApi> page(ApiQueryDTO dto) {
        Page<SysApi> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<SysApi> wrapper = new LambdaQueryWrapper<SysApi>()
                .eq(StringUtils.hasText(dto.getControllerClass()), SysApi::getControllerClass, dto.getControllerClass())
                .like(StringUtils.hasText(dto.getKeyword()), SysApi::getApiName, dto.getKeyword())
                .eq(dto.getStatus() != null, SysApi::getStatus, dto.getStatus())
                .orderByAsc(SysApi::getControllerClass)
                .orderByAsc(SysApi::getApiPath);
        apiMapper.selectPage(page, wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public Map<String, List<Map<String, String>>> listControllers() {
        return apiMapper.selectList(new LambdaQueryWrapper<SysApi>()
                        .select(SysApi::getAppId, SysApi::getControllerClass, SysApi::getControllerName)
                        .groupBy(SysApi::getAppId, SysApi::getControllerClass, SysApi::getControllerName))
                .stream()
                .collect(Collectors.groupingBy(
                        SysApi::getAppId,
                        LinkedHashMap::new,
                        Collectors.mapping(api -> {
                            Map<String, String> map = new LinkedHashMap<>();
                            map.put("controllerClass", api.getControllerClass());
                            map.put("controllerName", api.getControllerName());
                            return map;
                        }, Collectors.toList())
                ));
    }

    @Override
    public Map<String, Map<String, List<SysApi>>> listGrouped() {
        return apiMapper.selectList(new LambdaQueryWrapper<SysApi>()
                        .orderByAsc(SysApi::getAppId)
                        .orderByAsc(SysApi::getControllerName)
                        .orderByAsc(SysApi::getApiPath))
                .stream()
                .collect(Collectors.groupingBy(
                        SysApi::getAppId,
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                SysApi::getControllerName,
                                LinkedHashMap::new,
                                Collectors.toList()
                        )
                ));
    }

    @Override
    public void sync() {
        syncLocal();
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        SysApi api = apiMapper.selectById(id);
        if (api == null) throw new BusinessException("API不存在");
        api.setStatus(status);
        apiMapper.updateById(api);
    }

    @Override
    public void updatePermission(Long id, String permission) {
        SysApi api = apiMapper.selectById(id);
        if (api == null) throw new BusinessException("API不存在");
        api.setPermission(permission);
        apiMapper.updateById(api);
    }

}
