package com.demo.controller;

import com.demo.service.PromptManagerService;
import com.demo.service.PromptManagerService.AbTestResult;
import com.demo.service.SiliconFlowClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 项目二：Prompt 版本管理接口
 *
 * ── 模板 CRUD ─────────────────────────────────────────────────────────
 * GET    /api/prompts                              列出所有 Prompt 名称
 * GET    /api/prompts/{name}/versions             列出某个 Prompt 的所有版本
 * GET    /api/prompts/{name}/versions/{version}   获取指定版本模板内容
 * GET    /api/prompts/{name}/latest               获取最新版本模板内容
 * POST   /api/prompts/{name}/versions/{version}   注册（新增/覆盖）一个版本
 * DELETE /api/prompts/{name}/versions/{version}   删除一个版本
 *
 * ── 渲染 ──────────────────────────────────────────────────────────────
 * POST   /api/prompts/{name}/render               渲染模板（变量替换）
 *
 * ── A/B 测试 ──────────────────────────────────────────────────────────
 * GET    /api/prompts/{name}/ab-config            查看 A/B 测试配置
 * POST   /api/prompts/{name}/ab-config            设置 A/B 测试权重
 * GET    /api/prompts/{name}/ab-test              按 userId 获取分流版本
 *
 * ── 调用模型 ──────────────────────────────────────────────────────────
 * POST   /api/prompts/{name}/chat                 渲染模板后直接调用模型
 */
@RestController
@RequestMapping("/api/prompts")
public class PromptManagerController {

    private final PromptManagerService managerService;
    private final SiliconFlowClient    llmClient;

    public PromptManagerController(PromptManagerService managerService,
                                   SiliconFlowClient llmClient) {
        this.managerService = managerService;
        this.llmClient      = llmClient;
    }

    // ════════════════════════════════════════════════════════════════
    // 模板 CRUD
    // ════════════════════════════════════════════════════════════════

    /** 列出所有 Prompt 名称 */
    @GetMapping
    public ResponseEntity<?> listNames() {
        List<String> names = managerService.listNames();
        return ResponseEntity.ok(Map.of("names", names, "count", names.size()));
    }

    /** 列出某个 Prompt 的所有版本 */
    @GetMapping("/{name}/versions")
    public ResponseEntity<?> listVersions(@PathVariable String name) {
        List<String> versions = managerService.listVersions(name);
        return ResponseEntity.ok(Map.of("name", name, "versions", versions));
    }

    /** 获取指定版本模板 */
    @GetMapping("/{name}/versions/{version}")
    public ResponseEntity<?> getTemplate(@PathVariable String name,
                                         @PathVariable String version) {
        try {
            String template = managerService.getTemplate(name, version);
            return ResponseEntity.ok(Map.of("name", name, "version", version, "template", template));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** 获取最新版本模板 */
    @GetMapping("/{name}/latest")
    public ResponseEntity<?> getLatest(@PathVariable String name) {
        try {
            String latest   = managerService.getLatestVersion(name);
            String template = managerService.getTemplate(name, latest);
            return ResponseEntity.ok(Map.of("name", name, "version", latest, "template", template));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 注册（新增/覆盖）一个版本
     *
     * POST /api/prompts/{name}/versions/{version}
     * {
     *   "template": "你是一个{{role}}专家，分析以下代码：\n{{code}}"
     * }
     */
    @PostMapping("/{name}/versions/{version}")
    public ResponseEntity<?> saveTemplate(@PathVariable String name,
                                          @PathVariable String version,
                                          @RequestBody SaveRequest req) {
        if (req.template() == null || req.template().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "template 不能为空"));
        }
        managerService.saveTemplate(name, version, req.template());
        return ResponseEntity.ok(Map.of(
                "message", "保存成功",
                "name",    name,
                "version", version
        ));
    }

    /** 删除一个版本 */
    @DeleteMapping("/{name}/versions/{version}")
    public ResponseEntity<?> deleteTemplate(@PathVariable String name,
                                            @PathVariable String version) {
        managerService.deleteTemplate(name, version);
        return ResponseEntity.ok(Map.of("message", "删除成功", "name", name, "version", version));
    }

    // ════════════════════════════════════════════════════════════════
    // 渲染
    // ════════════════════════════════════════════════════════════════

    /**
     * 渲染模板（变量替换），不调用模型
     *
     * POST /api/prompts/{name}/render
     * {
     *   "version": "v2.0",      // 不传则用最新版本
     *   "vars": {
     *     "language": "Java",
     *     "code": "catch(Exception e){}"
     *   }
     * }
     */
    @PostMapping("/{name}/render")
    public ResponseEntity<?> render(@PathVariable String name,
                                    @RequestBody RenderRequest req) {
        try {
            String version = (req.version() != null && !req.version().isBlank())
                    ? req.version()
                    : managerService.getLatestVersion(name);

            String template = managerService.getTemplate(name, version);
            String rendered = managerService.render(template, req.vars() != null ? req.vars() : Map.of());

            return ResponseEntity.ok(Map.of(
                    "name",     name,
                    "version",  version,
                    "rendered", rendered
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // A/B 测试
    // ════════════════════════════════════════════════════════════════

    /** 查看当前 A/B 测试配置 */
    @GetMapping("/{name}/ab-config")
    public ResponseEntity<?> getAbConfig(@PathVariable String name) {
        return ResponseEntity.ok(Map.of(
                "name",    name,
                "config",  managerService.getAbConfig(name)
        ));
    }

    /**
     * 设置 A/B 测试分流权重
     *
     * POST /api/prompts/{name}/ab-config
     * {
     *   "weights": {
     *     "v1.0": 70,
     *     "v2.0": 30
     *   }
     * }
     */
    @PostMapping("/{name}/ab-config")
    public ResponseEntity<?> setAbConfig(@PathVariable String name,
                                         @RequestBody AbConfigRequest req) {
        if (req.weights() == null || req.weights().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "weights 不能为空"));
        }
        try {
            LinkedHashMap<String, Integer> ordered = new LinkedHashMap<>(req.weights());
            managerService.setAbConfig(name, ordered);
            return ResponseEntity.ok(Map.of(
                    "message", "A/B 配置已更新",
                    "name",    name,
                    "weights", req.weights()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 按 userId 获取 A/B 分流结果
     *
     * GET /api/prompts/{name}/ab-test?userId=user_001
     */
    @GetMapping("/{name}/ab-test")
    public ResponseEntity<?> abTest(@PathVariable String name,
                                    @RequestParam String userId) {
        try {
            AbTestResult result = managerService.getTemplateByUserId(name, userId);
            return ResponseEntity.ok(Map.of(
                    "name",     name,
                    "userId",   userId,
                    "version",  result.version(),
                    "reason",   result.reason(),
                    "template", result.template()
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 调用模型
    // ════════════════════════════════════════════════════════════════

    /**
     * 渲染模板后直接调用模型，返回 LLM 响应
     *
     * POST /api/prompts/{name}/chat
     * {
     *   "version": "v2.0",       // 不传则用最新版本
     *   "vars": {
     *     "language": "Java",
     *     "code": "catch(Exception e){}"
     *   },
     *   "userMessage": "请分析"   // 可选，追加在渲染结果之后作为 user 消息
     * }
     */
    @PostMapping("/{name}/chat")
    public ResponseEntity<?> chat(@PathVariable String name,
                                  @RequestBody ChatRequest req) {
        try {
            String version = (req.version() != null && !req.version().isBlank())
                    ? req.version()
                    : managerService.getLatestVersion(name);

            String template = managerService.getTemplate(name, version);
            String rendered = managerService.render(template, req.vars() != null ? req.vars() : Map.of());

            long start = System.currentTimeMillis();
            // rendered 作为 system prompt，userMessage（可选）作为 user 消息
            String userMsg  = (req.userMessage() != null && !req.userMessage().isBlank())
                    ? req.userMessage()
                    : "请按要求执行";
            String response = llmClient.chat(rendered, userMsg);
            long latency    = System.currentTimeMillis() - start;

            return ResponseEntity.ok(Map.of(
                    "name",       name,
                    "version",    version,
                    "systemPrompt", rendered,
                    "userMessage",  userMsg,
                    "response",   response,
                    "latencyMs",  latency,
                    "model",      llmClient.getModel()
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Request Records ──────────────────────────────────────────────
    record SaveRequest(String template) {}
    record RenderRequest(String version, Map<String, String> vars) {}
    record AbConfigRequest(Map<String, Integer> weights) {}
    record ChatRequest(String version, Map<String, String> vars, String userMessage) {}
}
