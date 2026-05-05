package com.duke.demo.controller;

import com.duke.demo.service.IExperimentService;
import com.duke.framework.common.Result;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 项目一：Prompt 模式对比实验接口
 * <p>
 * POST /api/experiment/run       运行单种模式
 * POST /api/experiment/run-all   同一段代码跑全部5种模式
 */
@RestController
@RequestMapping("/experiment")
@AllArgsConstructor
public class ExperimentController {

    private final IExperimentService experimentService;

    /**
     * 运行单种模式
     * <p>
     * POST /api/experiment/run
     * {
     * "mode": "structured",
     * "code": "catch(Exception e) {}"
     * }
     * mode 可选值：zero-shot | role-shot | few-shot | cot | structured
     */
    @PostMapping("/run")
    public Result<Map<String, Object>> run(@RequestBody RunRequest req) {
        if (req.code() == null || req.code().isBlank()) {
            return Result.fail("code 不能为空");
        }
        if (req.mode() == null || req.mode().isBlank()) {
            return Result.fail("mode 不能为空，可选值：zero-shot, role-shot, few-shot, cot, structured");
        }
        IExperimentService.ExperimentResult result = experimentService.runMode(req.mode(), req.code());
        return Result.success(toMap(result));
    }

    /**
     * 同一段代码跑全部5种模式，返回对比结果 + 汇总统计
     * <p>
     * POST /api/experiment/run-all
     * {
     * "code": "catch(Exception e) {}"
     * }
     */
    @PostMapping("/run-all")
    public Result<Map<String, Object>> runAll(@RequestBody RunAllRequest req) {
        if (req.code() == null || req.code().isBlank()) {
            return Result.fail("code 不能为空");
        }

        List<IExperimentService.ExperimentResult> results = experimentService.runAllModes(req.code());

        long formatPassCount = results.stream().filter(IExperimentService.ExperimentResult::formatOk).count();
        long avgLatency = (long) results.stream().mapToLong(IExperimentService.ExperimentResult::latencyMs).average().orElse(0);

        return Result.success(Map.of(
                "code", req.code(),
                "results", results.stream().map(this::toMap).toList(),
                "summary", Map.of(
                        "totalModes", results.size(),
                        "formatPassCount", formatPassCount,
                        "avgLatencyMs", avgLatency
                )
        ));
    }

    private Map<String, Object> toMap(IExperimentService.ExperimentResult r) {
        return Map.of(
                "mode", r.mode(),
                "response", r.response(),
                "latencyMs", r.latencyMs(),
                "formatOk", r.formatOk()
        );
    }

    record RunRequest(String mode, String code) {
    }

    record RunAllRequest(String code) {
    }
}
