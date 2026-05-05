package com.duke.demo.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目二：Prompt 版本管理接口
 */
public interface PromptManagerService {

    // ── 模板 CRUD ──────────────────────────────────────────────────

    /** 获取指定版本模板内容 */
    String getTemplate(String name, String version);

    /** 获取最新版本模板内容 */
    String getLatestTemplate(String name);

    /** 获取最新版本号 */
    String getLatestVersion(String name);

    /** 保存（新增 / 覆盖）一个版本 */
    void saveTemplate(String name, String version, String template);

    /** 删除一个版本 */
    void deleteTemplate(String name, String version);

    /** 列出某个 Prompt 的所有版本号 */
    List<String> listVersions(String name);

    /** 列出所有 Prompt 名称 */
    List<String> listNames();

    // ── 渲染 ──────────────────────────────────────────────────────

    /**
     * 渲染模板：将 {{key}} 占位符替换为实际值
     *
     * @param template 模板文本
     * @param vars     变量 Map
     */
    String render(String template, Map<String, String> vars);

    // ── A/B 测试 ──────────────────────────────────────────────────

    /**
     * 设置 A/B 测试分流权重
     * 例如：{"v1.0": 70, "v2.0": 30} 表示 70% 流量走 v1.0
     */
    void setAbConfig(String name, LinkedHashMap<String, Integer> weights);

    /** 查看某个 Prompt 当前的 A/B 测试配置 */
    Map<String, Integer> getAbConfig(String name);

    /**
     * 按 userId 获取 A/B 分流结果（同一 userId 每次结果稳定）
     */
    AbTestResult getTemplateByUserId(String name, String userId);

    /**
     * A/B 分流结果
     *
     * @param version  命中的版本号
     * @param template 该版本的模板内容
     * @param reason   分流原因说明
     */
    record AbTestResult(String version, String template, String reason) {}
}
