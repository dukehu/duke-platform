package com.duke.auth.controller;

import com.duke.auth.aspect.annotation.OperationLog;
import com.duke.framework.common.PageResult;
import com.duke.framework.common.Result;
import com.duke.auth.dto.UserDTO;
import com.duke.auth.service.IUserService;
import com.duke.auth.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class SysUserController {

    private final IUserService userService;

    @Operation(summary = "用户分页列表")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<PageResult<UserVO>> page(UserDTO dto) {
        return Result.success(userService.page(dto));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    @OperationLog(module = "用户管理", operation = "新增用户")
    public Result<Void> create(@Valid @RequestBody UserDTO dto) {
        userService.create(dto);
        return Result.success();
    }

    @Operation(summary = "修改用户")
    @PutMapping
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "用户管理", operation = "修改用户")
    public Result<Void> update(@RequestBody UserDTO dto) {
        userService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @OperationLog(module = "用户管理", operation = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @Operation(summary = "修改用户状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "用户管理", operation = "修改用户状态")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        userService.updateStatus(id, body.get("status"));
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('system:user:resetPwd')")
    @OperationLog(module = "用户管理", operation = "重置密码")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.getOrDefault("password", "123456"));
        return Result.success();
    }

    @Operation(summary = "分配角色")
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:assignRole')")
    @OperationLog(module = "用户管理", operation = "分配角色")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return Result.success();
    }

    @Operation(summary = "分配部门")
    @PostMapping("/{id}/depts")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "用户管理", operation = "分配部门")
    public Result<Void> assignDepts(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        // Jackson 将 JSON 数字反序列化为 Integer，需显式转换为 Long
        @SuppressWarnings("unchecked")
        List<Long> deptIds = body.get("deptIds") == null ? List.of() :
                ((List<?>) body.get("deptIds")).stream()
                        .map(v -> Long.valueOf(v.toString()))
                        .collect(java.util.stream.Collectors.toList());
        Long primaryDeptId = body.get("primaryDeptId") != null
                ? Long.valueOf(body.get("primaryDeptId").toString()) : null;
        userService.assignDepts(id, deptIds, primaryDeptId);
        return Result.success();
    }
}
