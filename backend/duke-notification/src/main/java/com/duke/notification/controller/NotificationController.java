package com.duke.notification.controller;

import com.duke.notification.dto.NotificationQueryDTO;
import com.duke.notification.service.INotificationService;
import com.duke.framework.common.PageResult;
import com.duke.framework.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification 绠＄悊")
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    @Operation(summary = "鍒嗛〉鍒楄〃")
    @GetMapping("/page")
    public Result<PageResult<Object>> page(NotificationQueryDTO dto) {
        return Result.success(notificationService.page(dto));
    }

    @Operation(summary = "璇︽儏")
    @GetMapping("/{id}")
    public Result<Object> getById(@PathVariable Long id) {
        return Result.success(notificationService.getById(id));
    }

    @Operation(summary = "鏂板")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody Object dto) {
        // TODO
        return Result.success();
    }

    @Operation(summary = "淇敼")
    @PutMapping
    public Result<Void> update(@RequestBody Object dto) {
        // TODO
        return Result.success();
    }

    @Operation(summary = "鍒犻櫎")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return Result.success();
    }
}



