package com.demo.controller;

import com.demo.service.ExperimentService;
import com.demo.service.ExperimentService.ExperimentResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 项目一：Prompt 模式对比实验接口
 *
 * POST /api/experiment/run          —— 运行单种模式
 * POST /api/experiment/run-all      —— 对同一段代码跑全部5种模式
 */
@RestController
@RequestMapping("/api/experiment")
public class ExperimentController {

    private final ExperimentService experimentService;

    public ExperimentController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    /**
     * 运行单种模式
     *
     * POST /api/experiment/run
     * {
     *   "mode": "zero-shot",   // zero-shot | role-shot | few-shot | cot | structured
     *   "code": "catch(Exception e) {}"
     * }
     */
    @PostMapping("/run")
    public ResponseEntity<?> run(@RequestBody RunRequest req) {
        if (req.code() == null || req.code().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "code 不能为空"));
        }
        if (req.mode() == null || req.mode().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "mode 不能为空，可选值：zero-shot, role-shot, few-shot, cot, structured"));
        }
        try {
            ExperimentResult result = experimentService.runMode(req.mode(), req.code());
            return ResponseEntity.ok(toResponse(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 对同一段代码跑全部5种模式，返回对比结果
     *
     * POST /api/experiment/run-all
     * {
     *   "code": "catch(Exception e) {}"
     * }
     */
    @PostMapping("/run-all")
    public ResponseEntity<?> runAll(@RequestBody RunAllRequest req) {
        if (req.code() == null || req.code().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "code 不能为空"));
        }

        List<ExperimentResult> results = experimentService.runAllModes(req.code());

        // 汇总统计
        long formatPassCount = results.stream().filter(ExperimentResult::formatOk).count();
        double avgLatency    = results.stream().mapToLong(ExperimentResult::latencyMs).average().orElse(0);

        return ResponseEntity.ok(Map.of(
                "code",           req.code(),
                "results",        results.stream().map(this::toResponse).toList(),
                "summary", Map.of(
                        "totalModes",     results.size(),
                        "formatPassCount", formatPassCount,
                        "avgLatencyMs",   (long) avgLatency
                )
        ));
    }

    private Map<String, Object> toResponse(ExperimentResult r) {
        return Map.of(
                "mode",      r.mode(),
                "response",  r.response(),
                "latencyMs", r.latencyMs(),
                "formatOk",  r.formatOk()
        );
    }

    record RunRequest(String mode, String code) {}
    record RunAllRequest(String code) {}
}
