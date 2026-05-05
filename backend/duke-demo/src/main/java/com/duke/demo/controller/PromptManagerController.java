package com.duke.demo.controller;

import com.duke.demo.service.IPromptManagerService;
import com.duke.demo.service.ISiliconFlowService;
import com.duke.framework.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 项目二：Prompt 版本管理接口
 * <p>
 * ── 模板 CRUD ─────────────────────────────────────────────────────────
 * GET    /api/prompts                              列出所有 Prompt 名称
 * GET    /api/prompts/{name}/versions             列出某个 Prompt 的所有版本
 * GET    /api/prompts/{name}/versions/{version}   获取指定版本模板内容
 * GET    /api/prompts/{name}/latest               获取最新版本模板内容
 * POST   /api/prompts/{name}/versions/{version}   新增/覆盖一个版本
 * DELETE /api/prompts/{name}/versions/{version}   删除一个版本
 * <p>
 * ── 渲染 ──────────────────────────────────────────────────────────────
 * POST   /api/prompts/{name}/render               渲染模板（变量替换，不调模型）
 * <p>
 * ── A/B 测试 ──────────────────────────────────────────────────────────
 * GET    /api/prompts/{name}/ab-config            查看 A/B 测试配置
 * POST   /api/prompts/{name}/ab-config            设置 A/B 测试权重
 * GET    /api/prompts/{name}/ab-test?userId=xxx   按 userId 查看分流结果
 * <p>
 * ── 调用模型 ──────────────────────────────────────────────────────────
 * POST   /api/prompts/{name}/chat                 渲染后直接调用模型
 */
@Tag(name = "Prompt 模式管理")
@RestController
@RequestMapping("/prompts")
@AllArgsConstructor
public class PromptManagerController {

    private final IPromptManagerService managerService;
    private final ISiliconFlowService llmService;

    // ════════════════════════════════════════════════════════════════
    // 模板 CRUD
    // ════════════════════════════════════════════════════════════════

    @Operation(summary = "列出所有 Prompt 名称")
    @GetMapping
    public Result<Map<String, Object>> listNames() {
        List<String> names = managerService.listNames();
        return Result.success(Map.of("names", names, "count", names.size()));
    }

    @Operation(summary = "列出某个 Prompt 的所有版本")
    @GetMapping("/{name}/versions")
    public Result<Map<String, Object>> listVersions(@PathVariable String name) {
        return Result.success(Map.of(
                "name", name,
                "versions", managerService.listVersions(name)
        ));
    }

    @Operation(summary = "获取指定版本模板内容")
    @GetMapping("/{name}/versions/{version}")
    public Result<Map<String, Object>> getTemplate(@PathVariable String name,
                                                   @PathVariable String version) {
        try {
            return Result.success(Map.of(
                    "name", name,
                    "version", version,
                    "template", managerService.getTemplate(name, version)
            ));
        } catch (NoSuchElementException e) {
            return Result.fail(e.getMessage());
        }
    }

    @Operation(summary = "获取最新版本模板内容")
    @GetMapping("/{name}/latest")
    public Result<Map<String, Object>> getLatest(@PathVariable String name) {
        try {
            String version = managerService.getLatestVersion(name);
            String template = managerService.getTemplate(name, version);
            return Result.success(Map.of("name", name, "version", version, "template", template));
        } catch (NoSuchElementException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * POST /api/prompts/{name}/versions/{version}
     * { "template": "你是一个{{role}}专家..." }
     */
    @Operation(summary = "新增/覆盖一个版本")
    @PostMapping("/{name}/versions/{version}")
    public Result<Map<String, Object>> saveTemplate(@PathVariable String name,
                                                    @PathVariable String version,
                                                    @RequestBody SaveRequest req) {
        if (req.template() == null || req.template().isBlank()) {
            return Result.fail("template 不能为空");
        }
        managerService.saveTemplate(name, version, req.template());
        return Result.success(Map.of("message", "保存成功", "name", name, "version", version));
    }

    @Operation(summary = "删除一个版本")
    @DeleteMapping("/{name}/versions/{version}")
    public Result<Map<String, Object>> deleteTemplate(@PathVariable String name,
                                                      @PathVariable String version) {
        managerService.deleteTemplate(name, version);
        return Result.success(Map.of("message", "删除成功", "name", name, "version", version));
    }

    // ════════════════════════════════════════════════════════════════
    // 渲染
    // ════════════════════════════════════════════════════════════════

    /**
     * POST /api/prompts/{name}/render
     * {
     * "version": "v2.0",        // 不传则用最新版本
     * "vars": { "language": "Java", "code": "..." }
     * }
     */
    @Operation(summary = "渲染模板（变量替换，不调模型）")
    @PostMapping("/{name}/render")
    public Result<Map<String, Object>> render(@PathVariable String name,
                                              @RequestBody RenderRequest req) {
        try {
            String version = resolveVersion(name, req.version());
            String template = managerService.getTemplate(name, version);
            String rendered = managerService.render(template, req.vars() != null ? req.vars() : Map.of());
            return Result.success(Map.of("name", name, "version", version, "rendered", rendered));
        } catch (NoSuchElementException e) {
            return Result.fail(e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // A/B 测试
    // ════════════════════════════════════════════════════════════════
    @Operation(summary = "查看 A/B 测试配置")
    @GetMapping("/{name}/ab-config")
    public Result<Map<String, Object>> getAbConfig(@PathVariable String name) {
        return Result.success(Map.of("name", name, "config", managerService.getAbConfig(name)));
    }

    /**
     * POST /api/prompts/{name}/ab-config
     * { "weights": { "v1.0": 70, "v2.0": 30 } }
     */
    @Operation(summary = "设置 A/B 测试权重")
    @PostMapping("/{name}/ab-config")
    public Result<Map<String, Object>> setAbConfig(@PathVariable String name,
                                                   @RequestBody AbConfigRequest req) {
        if (req.weights() == null || req.weights().isEmpty()) {
            return Result.fail("weights 不能为空");
        }
        managerService.setAbConfig(name, new LinkedHashMap<>(req.weights()));
        return Result.success(Map.of("message", "A/B 配置已更新", "name", name, "weights", req.weights()));
    }

    /**
     * GET /api/prompts/{name}/ab-test?userId=user_001
     */
    @Operation(summary = "按 userId 查看分流结果")
    @GetMapping("/{name}/ab-test")
    public Result<Map<String, Object>> abTest(@PathVariable String name,
                                              @RequestParam String userId) {
        try {
            IPromptManagerService.AbTestResult result = managerService.getTemplateByUserId(name, userId);
            return Result.success(Map.of(
                    "name", name,
                    "userId", userId,
                    "version", result.version(),
                    "reason", result.reason(),
                    "template", result.template()
            ));
        } catch (NoSuchElementException e) {
            return Result.fail(e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 调用模型
    // ════════════════════════════════════════════════════════════════

    /**
     * POST /api/prompts/{name}/chat
     * {
     * "version": "v2.0",         // 不传则用最新版本
     * "vars": { "language": "Java", "code": "catch(Exception e){}" },
     * "userMessage": "请分析"     // 可选
     * }
     */
    @Operation(summary = "渲染后直接调用模型")
    @PostMapping("/{name}/chat")
    public Result<Map<String, Object>> chat(@PathVariable String name,
                                            @RequestBody ChatRequest req) {
        try {
            String version = resolveVersion(name, req.version());
            String template = managerService.getTemplate(name, version);
            String systemPrompt = managerService.render(template, req.vars() != null ? req.vars() : Map.of());
            String userMsg = (req.userMessage() != null && !req.userMessage().isBlank())
                    ? req.userMessage() : "请按要求执行";

            long start = System.currentTimeMillis();
            String response = llmService.chat(systemPrompt, userMsg);
            long latency = System.currentTimeMillis() - start;

            return Result.success(Map.of(
                    "name", name,
                    "version", version,
                    "systemPrompt", systemPrompt,
                    "userMessage", userMsg,
                    "response", response,
                    "latencyMs", latency,
                    "model", llmService.getModel()
            ));
        } catch (NoSuchElementException e) {
            return Result.fail(e.getMessage());
        }
    }

    // ── 工具方法 ─────────────────────────────────────────────────────

    private String resolveVersion(String name, String version) {
        return (version != null && !version.isBlank())
                ? version
                : managerService.getLatestVersion(name);
    }

    // ── Request Records ──────────────────────────────────────────────
    record SaveRequest(String template) {
    }

    record RenderRequest(String version, Map<String, String> vars) {
    }

    record AbConfigRequest(Map<String, Integer> weights) {
    }

    record ChatRequest(String version, Map<String, String> vars, String userMessage) {
    }
}
