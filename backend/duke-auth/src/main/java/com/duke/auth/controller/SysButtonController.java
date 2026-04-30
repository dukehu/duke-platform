package com.duke.auth.controller;

import com.duke.auth.aspect.annotation.OperationLog;
import com.duke.framework.common.Result;
import com.duke.auth.dto.ButtonDTO;
import com.duke.auth.entity.SysButton;
import com.duke.auth.service.IButtonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "按钮管理")
@RestController
@RequestMapping("/button")
@RequiredArgsConstructor
public class SysButtonController {

    private final IButtonService buttonService;

    @Operation(summary = "按钮列表")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public Result<List<SysButton>> list(@RequestParam Long menuId) {
        return Result.success(buttonService.list(menuId));
    }

    @Operation(summary = "新增按钮")
    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:add')")
    @OperationLog(module = "按钮管理", operation = "新增按钮")
    public Result<Void> create(@Valid @RequestBody ButtonDTO dto) {
        buttonService.create(dto);
        return Result.success();
    }

    @Operation(summary = "修改按钮")
    @PutMapping
    @PreAuthorize("hasAuthority('system:menu:edit')")
    @OperationLog(module = "按钮管理", operation = "修改按钮")
    public Result<Void> update(@Valid @RequestBody ButtonDTO dto) {
        buttonService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除按钮")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:delete')")
    @OperationLog(module = "按钮管理", operation = "删除按钮")
    public Result<Void> delete(@PathVariable Long id) {
        buttonService.delete(id);
        return Result.success();
    }
}
