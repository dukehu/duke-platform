package com.duke.auth.controller;

import com.duke.auth.aspect.annotation.OperationLog;
import com.duke.framework.common.Result;
import com.duke.auth.dto.DeptDTO;
import com.duke.auth.service.IDeptService;
import com.duke.auth.vo.DeptTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "部门管理")
@RestController
@RequestMapping("/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final IDeptService deptService;

    @Operation(summary = "部门树")
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:dept:list')")
    public Result<List<DeptTreeVO>> tree() {
        return Result.success(deptService.tree());
    }

    @Operation(summary = "新增部门")
    @PostMapping
    @PreAuthorize("hasAuthority('system:dept:add')")
    @OperationLog(module = "部门管理", operation = "新增部门")
    public Result<Void> create(@Valid @RequestBody DeptDTO dto) {
        deptService.create(dto);
        return Result.success();
    }

    @Operation(summary = "修改部门")
    @PutMapping
    @PreAuthorize("hasAuthority('system:dept:edit')")
    @OperationLog(module = "部门管理", operation = "修改部门")
    public Result<Void> update(@RequestBody DeptDTO dto) {
        deptService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:delete')")
    @OperationLog(module = "部门管理", operation = "删除部门")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.success();
    }
}
