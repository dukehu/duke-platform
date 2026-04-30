package com.duke.auth.controller;

import com.duke.framework.common.PageResult;
import com.duke.framework.common.Result;
import com.duke.auth.dto.LogQueryDTO;
import com.duke.auth.entity.SysOperationLog;
import com.duke.auth.service.IOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "操作日志")
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
public class SysOperationLogController {

    private final IOperationLogService logService;

    @Operation(summary = "操作日志分页列表")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:log:list')")
    public Result<PageResult<SysOperationLog>> page(LogQueryDTO dto) {
        return Result.success(logService.page(dto));
    }

    @Operation(summary = "删除操作日志")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:log:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        logService.delete(id);
        return Result.success();
    }
}
