package com.duke.auth.controller;

import com.duke.auth.aspect.annotation.OperationLog;
import com.duke.framework.common.PageResult;
import com.duke.framework.common.Result;
import com.duke.auth.dto.RoleDTO;
import com.duke.auth.entity.SysRole;
import com.duke.auth.service.IRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final IRoleService roleService;

    @Operation(summary = "角色分页列表")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<PageResult<SysRole>> page(RoleDTO dto) {
        return Result.success(roleService.page(dto));
    }

    @Operation(summary = "角色列表（不分页）")
    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        return Result.success(roleService.list());
    }

    @Operation(summary = "角色详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<SysRole> getById(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    @OperationLog(module = "角色管理", operation = "新增角色")
    public Result<Void> create(@RequestBody RoleDTO dto) {
        roleService.create(dto);
        return Result.success();
    }

    @Operation(summary = "修改角色")
    @PutMapping
    @PreAuthorize("hasAuthority('system:role:edit')")
    @OperationLog(module = "角色管理", operation = "修改角色")
    public Result<Void> update(@RequestBody RoleDTO dto) {
        roleService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    @OperationLog(module = "角色管理", operation = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }

    @Operation(summary = "获取角色已分配菜单ID")
    @GetMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<List<Long>> getMenuIds(@PathVariable Long id) {
        return Result.success(roleService.getMenuIds(id));
    }

    @Operation(summary = "分配菜单权限")
    @PostMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:assignMenu')")
    @OperationLog(module = "角色管理", operation = "分配菜单权限")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return Result.success();
    }

    @Operation(summary = "获取角色已分配API ID")
    @GetMapping("/{id}/apis")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<List<Long>> getApiIds(@PathVariable Long id) {
        return Result.success(roleService.getApiIds(id));
    }

    @Operation(summary = "分配API权限")
    @PostMapping("/{id}/apis")
    @PreAuthorize("hasAuthority('system:role:assignApi')")
    @OperationLog(module = "角色管理", operation = "分配API权限")
    public Result<Void> assignApis(@PathVariable Long id, @RequestBody List<Long> apiIds) {
        roleService.assignApis(id, apiIds);
        return Result.success();
    }

    @Operation(summary = "设置数据权限")
    @PostMapping("/{id}/data-scope")
    @PreAuthorize("hasAuthority('system:role:dataScope')")
    @OperationLog(module = "角色管理", operation = "设置数据权限")
    public Result<Void> assignDataScope(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer dataScope = (Integer) body.get("dataScope");
        List<Long> deptIds = (List<Long>) body.get("deptIds");
        roleService.assignDataScope(id, dataScope, deptIds);
        return Result.success();
    }

    @Operation(summary = "获取角色已分配按钮ID")
    @GetMapping("/{id}/buttons")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<List<Long>> getButtonIds(@PathVariable Long id) {
        return Result.success(roleService.getButtonIds(id));
    }

    @Operation(summary = "分配按钮权限")
    @PostMapping("/{id}/buttons")
    @PreAuthorize("hasAuthority('system:role:assign')")
    @OperationLog(module = "角色管理", operation = "分配按钮权限")
    public Result<Void> assignButtons(@PathVariable Long id, @RequestBody List<Long> buttonIds) {
        roleService.assignButtons(id, buttonIds);
        return Result.success();
    }
}
