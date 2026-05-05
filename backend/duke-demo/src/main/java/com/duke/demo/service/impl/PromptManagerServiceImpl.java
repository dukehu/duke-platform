package com.duke.demo.service.impl;

import com.duke.demo.config.properties.PromptProperties;
import com.duke.demo.service.IPromptManagerService;
import com.duke.framework.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class PromptManagerServiceImpl implements IPromptManagerService {

    private static final Logger log = LoggerFactory.getLogger(PromptManagerServiceImpl.class);

    private final PromptProperties props;
    private Path storageDir;

    // 内存缓存：key = "name::version"
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    // A/B 测试配置：name -> {version -> weight}（有序，保证分流稳定）
    private final ConcurrentHashMap<String, LinkedHashMap<String, Integer>> abConfigs
            = new ConcurrentHashMap<>();

    // 热更新
    private WatchService watchService;
    private ScheduledExecutorService watchExecutor;
    private final AtomicReference<ConcurrentHashMap<WatchKey, Path>> watchKeys
            = new AtomicReference<>(new ConcurrentHashMap<>());

    public PromptManagerServiceImpl(PromptProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() throws IOException {
        this.storageDir = Paths.get(props.getStorageDir());
        Files.createDirectories(storageDir);

        this.watchService = FileSystems.getDefault().newWatchService();
        this.watchExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "prompt-watcher");
            t.setDaemon(true);
            return t;
        });

        registerDir(storageDir);
        startWatcher();
        log.info("PromptManagerService 已启动，存储目录：{}", storageDir.toAbsolutePath());
    }

    @PreDestroy
    public void destroy() throws IOException {
        if (watchExecutor != null) watchExecutor.shutdownNow();
        if (watchService != null) watchService.close();
    }

    // ── 热更新 ────────────────────────────────────────────────────────

    private void startWatcher() {
        watchExecutor.scheduleWithFixedDelay(() -> {
            WatchKey key;
            while ((key = watchService.poll()) != null) {
                Path dir = watchKeys.get().get(key);
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;

                    @SuppressWarnings("unchecked")
                    Path changed = dir.resolve(((WatchEvent<Path>) event).context());

                    // 新建子目录时也注册监听
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE
                            && Files.isDirectory(changed)) {
                        try {
                            registerDir(changed);
                        } catch (IOException ignored) {
                        }
                    }

                    if (changed.toString().endsWith(".txt")) {
                        String ck = toCacheKey(changed);
                        if (ck != null) {
                            cache.remove(ck);
                            log.info("[热更新] 缓存已失效：{}", ck);
                        }
                    }
                }
                key.reset();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void registerDir(Path dir) throws IOException {
        WatchKey key = dir.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
        watchKeys.get().put(key, dir);
    }

    private String toCacheKey(Path filePath) {
        try {
            Path rel = storageDir.relativize(filePath);
            if (rel.getNameCount() < 2) return null;
            String name = rel.getName(0).toString();
            String version = rel.getFileName().toString().replace(".txt", "");
            return buildKey(name, version);
        } catch (Exception e) {
            return null;
        }
    }

    // ── 模板 CRUD ─────────────────────────────────────────────────────

    @Override
    public String getTemplate(String name, String version) {
        return cache.computeIfAbsent(buildKey(name, version), k -> loadFromDisk(name, version));
    }

    @Override
    public String getLatestTemplate(String name) {
        return getTemplate(name, getLatestVersion(name));
    }

    @Override
    public String getLatestVersion(String name) {
        List<String> versions = listVersions(name);
        if (versions.isEmpty()) {
            throw new NoSuchElementException("没有找到任何版本：" + name);
        }
        return versions.stream()
                .max(Comparator.comparingDouble(PromptManagerServiceImpl::parseVersion))
                .orElseThrow();
    }

    @Override
    public void saveTemplate(String name, String version, String template) {
        Path dir = storageDir.resolve(name);
        Path file = dir.resolve(version + ".txt");
        try {
            Files.createDirectories(dir);
            registerDir(dir);
            Files.writeString(file, template, StandardCharsets.UTF_8);
            cache.put(buildKey(name, version), template);
            log.info("[保存] {} v{}", name, version);
        } catch (IOException e) {
            throw new BusinessException("写入模板失败: " + file);
        }
    }

    @Override
    public void deleteTemplate(String name, String version) {
        Path file = storageDir.resolve(name).resolve(version + ".txt");
        try {
            Files.deleteIfExists(file);
            cache.remove(buildKey(name, version));
            log.info("[删除] {} v{}", name, version);
        } catch (IOException e) {
            throw new BusinessException("删除模板失败: " + file);
        }
    }

    @Override
    public List<String> listVersions(String name) {
        Path dir = storageDir.resolve(name);
        if (!Files.exists(dir)) return Collections.emptyList();
        try {
            return Files.list(dir)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .map(p -> p.getFileName().toString().replace(".txt", ""))
                    .sorted(Comparator.comparingDouble(PromptManagerServiceImpl::parseVersion))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> listNames() {
        try {
            if (!Files.exists(storageDir)) return Collections.emptyList();
            return Files.list(storageDir)
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    // ── 渲染 ──────────────────────────────────────────────────────────

    @Override
    public String render(String template, Map<String, String> vars) {
        String result = template;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            result = result.replace("{{" + e.getKey() + "}}", e.getValue());
        }
        log.info("[渲染] vars={} outputLen={}chars", vars.keySet(), result.length());
        return result;
    }

    // ── A/B 测试 ──────────────────────────────────────────────────────

    @Override
    public void setAbConfig(String name, LinkedHashMap<String, Integer> weights) {
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) throw new IllegalArgumentException("权重之和必须 > 0");
        abConfigs.put(name, weights);
        log.info("[A/B配置] {} -> {}", name, weights);
    }

    @Override
    public Map<String, Integer> getAbConfig(String name) {
        return abConfigs.getOrDefault(name, new LinkedHashMap<>());
    }

    @Override
    public AbTestResult getTemplateByUserId(String name, String userId) {
        LinkedHashMap<String, Integer> config = abConfigs.get(name);
        if (config == null || config.isEmpty()) {
            String latest = getLatestVersion(name);
            return new AbTestResult(latest, getTemplate(name, latest), "无A/B配置，返回最新版本");
        }

        int total = config.values().stream().mapToInt(Integer::intValue).sum();
        int slot = Math.abs(userId.hashCode()) % total;

        int cumulative = 0;
        for (Map.Entry<String, Integer> e : config.entrySet()) {
            cumulative += e.getValue();
            if (slot < cumulative) {
                String version = e.getKey();
                log.info("[A/B] userId={} -> {} v{}", userId, name, version);
                return new AbTestResult(version, getTemplate(name, version),
                        "命中版本 " + version + "（slot=" + slot + " / total=" + total + "）");
            }
        }
        // 兜底
        String latest = getLatestVersion(name);
        return new AbTestResult(latest, getTemplate(name, latest), "兜底：返回最新版本");
    }

    // ── 内部工具 ──────────────────────────────────────────────────────

    private String loadFromDisk(String name, String version) {
        Path file = storageDir.resolve(name).resolve(version + ".txt");
        if (!Files.exists(file)) {
            throw new NoSuchElementException("模板文件不存在: " + file);
        }
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            log.info("[加载] {} {} ({}chars)", name, version, content.length());
            return content;
        } catch (IOException e) {
            throw new BusinessException("读取模板失败: " + file);
        }
    }

    private static String buildKey(String name, String version) {
        return name + "::" + version;
    }

    private static double parseVersion(String v) {
        try {
            return Double.parseDouble(v.replaceFirst("^v", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
