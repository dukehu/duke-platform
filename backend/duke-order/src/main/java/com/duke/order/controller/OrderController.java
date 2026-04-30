package com.duke.order.controller;

import com.duke.order.dto.OrderQueryDTO;
import com.duke.order.service.IOrderService;
import com.duke.framework.common.PageResult;
import com.duke.framework.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Order 绠＄悊")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    @Operation(summary = "鍒嗛〉鍒楄〃")
    @GetMapping("/page")
    public Result<PageResult<Object>> page(OrderQueryDTO dto) {
        return Result.success(orderService.page(dto));
    }

    @Operation(summary = "璇︽儏")
    @GetMapping("/{id}")
    public Result<Object> getById(@PathVariable Long id) {
        return Result.success(orderService.getById(id));
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
        orderService.delete(id);
        return Result.success();
    }
}



