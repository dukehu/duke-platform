package ${package}.controller;

import ${package}.dto.${classPrefix}QueryDTO;
import ${package}.service.I${classPrefix}Service;
import com.duke.framework.common.PageResult;
import com.duke.framework.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "${classPrefix} 绠＄悊")
@RestController
@RequestMapping("/${classPrefixLower}")
@RequiredArgsConstructor
public class ${classPrefix}Controller {

    private final I${classPrefix}Service ${classPrefixLower}Service;

    @Operation(summary = "鍒嗛〉鍒楄〃")
    @GetMapping("/page")
    public Result<PageResult<Object>> page(${classPrefix}QueryDTO dto) {
        return Result.success(${classPrefixLower}Service.page(dto));
    }

    @Operation(summary = "璇︽儏")
    @GetMapping("/{id}")
    public Result<Object> getById(@PathVariable Long id) {
        return Result.success(${classPrefixLower}Service.getById(id));
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
        ${classPrefixLower}Service.delete(id);
        return Result.success();
    }
}



