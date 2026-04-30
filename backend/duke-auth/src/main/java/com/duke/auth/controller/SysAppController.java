package com.duke.auth.controller;

import com.duke.auth.aspect.annotation.OperationLog;
import com.duke.framework.common.PageResult;
import com.duke.framework.common.Result;
import com.duke.auth.dto.AppDTO;
import com.duke.auth.entity.SysApp;
import com.duke.auth.service.IAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "应用管理")
@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
public class SysAppController {

    private final IAppService appService;

    @Operation(summary = "应用分页列表")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:app:list')")
    public Result<PageResult<SysApp>> page(AppDTO dto) {
        return Result.success(appService.page(dto));
    }

    @Operation(summary = "所有启用应用")
    @GetMapping("/list")
    public Result<List<SysApp>> listAll() {
        return Result.success(appService.listAll());
    }

    @Operation(summary = "新增应用")
    @PostMapping
    @PreAuthorize("hasAuthority('system:app:add')")
    @OperationLog(module = "应用管理", operation = "新增应用")
    public Result<Void> create(@Valid @RequestBody AppDTO dto) {
        appService.create(dto);
        return Result.success();
    }

    @Operation(summary = "修改应用")
    @PutMapping
    @PreAuthorize("hasAuthority('system:app:edit')")
    @OperationLog(module = "应用管理", operation = "修改应用")
    public Result<Void> update(@RequestBody AppDTO dto) {
        appService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除应用")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:app:delete')")
    @OperationLog(module = "应用管理", operation = "删除应用")
    public Result<Void> delete(@PathVariable Long id) {
        appService.delete(id);
        return Result.success();
    }
}
