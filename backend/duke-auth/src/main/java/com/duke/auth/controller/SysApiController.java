package com.duke.auth.controller;

import com.duke.framework.common.PageResult;
import com.duke.framework.common.Result;
import com.duke.auth.dto.ApiQueryDTO;
import com.duke.auth.entity.SysApi;
import com.duke.auth.service.IApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "API管理")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SysApiController {

    private final IApiService apiService;

    @Operation(summary = "API分页列表")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:api:list')")
    public Result<PageResult<SysApi>> page(ApiQueryDTO dto) {
        return Result.success(apiService.page(dto));
    }

    @Operation(summary = "Controller分组列表")
    @GetMapping("/controllers")
    @PreAuthorize("hasAuthority('system:api:list')")
    public Result<Map<String, List<Map<String, String>>>> listControllers() {
        return Result.success(apiService.listControllers());
    }

    @Operation(summary = "按应用和Controller分组列表")
    @GetMapping("/grouped")
    @PreAuthorize("hasAuthority('system:api:list')")
    public Result<Map<String, Map<String, List<SysApi>>>> listGrouped() {
        return Result.success(apiService.listGrouped());
    }

    @Operation(summary = "手动同步API（duke-auth）")
    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('system:api:sync')")
    public Result<Void> sync() {
        apiService.sync();
        return Result.success();
    }

    @Operation(summary = "扫描指定应用的API")
    @PostMapping("/sync/{appId}")
    @PreAuthorize("hasAuthority('system:api:sync')")
    public Result<Void> syncApp(@PathVariable String appId) {
        apiService.syncAppApis(appId);
        return Result.success();
    }

    @Operation(summary = "修改API状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:api:edit')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        apiService.updateStatus(id, body.get("status"));
        return Result.success();
    }

    @Operation(summary = "修改API权限标识")
    @PutMapping("/{id}/permission")
    @PreAuthorize("hasAuthority('system:api:edit')")
    public Result<Void> updatePermission(@PathVariable Long id, @RequestBody Map<String, String> body) {
        apiService.updatePermission(id, body.get("permission"));
        return Result.success();
    }
}
