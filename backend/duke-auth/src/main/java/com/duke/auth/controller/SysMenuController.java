package com.duke.auth.controller;

import com.duke.auth.aspect.annotation.OperationLog;
import com.duke.framework.common.Result;
import com.duke.auth.dto.MenuDTO;
import com.duke.auth.entity.SysMenu;
import com.duke.auth.service.IMenuService;
import com.duke.auth.vo.MenuTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "菜单管理")
@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final IMenuService menuService;

    @Operation(summary = "菜单树")
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public Result<List<MenuTreeVO>> tree(@RequestParam(required = false) Long appId) {
        return Result.success(menuService.tree(appId));
    }

    @Operation(summary = "菜单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public Result<SysMenu> getById(@PathVariable Long id) {
        return Result.success(menuService.getById(id));
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:add')")
    @OperationLog(module = "菜单管理", operation = "新增菜单")
    public Result<Void> create(@Valid @RequestBody MenuDTO dto) {
        menuService.create(dto);
        return Result.success();
    }

    @Operation(summary = "修改菜单")
    @PutMapping
    @PreAuthorize("hasAuthority('system:menu:edit')")
    @OperationLog(module = "菜单管理", operation = "修改菜单")
    public Result<Void> update(@RequestBody MenuDTO dto) {
        menuService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:delete')")
    @OperationLog(module = "菜单管理", operation = "删除菜单")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.success();
    }
}
