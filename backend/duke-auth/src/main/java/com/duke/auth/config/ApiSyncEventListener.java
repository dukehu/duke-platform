package com.duke.auth.config;

import com.duke.auth.event.ApiSyncCompletedEvent;
import com.duke.auth.service.IGatewayPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiSyncEventListener {

    private final IGatewayPermissionService gatewayPermissionService;

    @Async
    @EventListener(ApiSyncCompletedEvent.class)
    public void onApiSyncCompleted(ApiSyncCompletedEvent event) {
        log.info("收到 API 同步完成事件，刷新权限规则缓存");
        gatewayPermissionService.refreshRules();
    }
}
