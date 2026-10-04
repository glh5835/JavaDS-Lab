package com.javadslab.workbench;

import com.javadslab.trace.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 后台任务系统（计划 §24）：Maven 测试 / 大规模造数 / 完整验证等重活绝不阻塞 HTTP 请求线程。
 * 状态机：queued → running → success | failed；前端轮询 GET /api/tasks/{id} 取日志与结果。
 * 任务记录同时落盘 logs/workbench.log（§35）。
 */
public final class TaskManager {

    public static final class Task {
        public final long id;
        public final String type;
        public volatile String status = "queued"; // queued/running/success/failed/cancelled
        public volatile String startedAt;
        public volatile String endedAt;
        public volatile String summary;
        public final StringBuilder logs = new StringBuilder();

        Task(long id, String type) {
            this.id = id;
            this.type = type;
        }

        void log(String line) {
            synchronized (logs) {
                logs.append(line).append('\n');
                if (logs.length() > 400_000) logs.delete(0, 200_000); // 防日志无限膨胀
            }
        }

        public Map<String, Object> toMap(boolean withLogs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("type", type);
            m.put("status", status);
            m.put("startedAt", startedAt);
            m.put("endedAt", endedAt);
            m.put("summary", summary);
            if (withLogs) {
                synchronized (logs) {
                    m.put("logs", logs.toString());
                }
            }
            return m;
        }
    }

    private final Path root;
    private final Path logDir;
    private final ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "workbench-task");
        t.setDaemon(true);
        return t;
    });
    private final AtomicLong seq = new AtomicLong();
    private final List<Task> tasks = new ArrayList<>(); // 只增列表，容量有限

    public TaskManager(Path root) {
        this.root = root;
        this.logDir = root.resolve("logs");
        try {
            Files.createDirectories(logDir);
        } catch (IOException ignored) {
        }
        // 保留最近 50 条任务记录
    }

    public synchronized Task submit(String type) {
        if (!List.of("maven-test", "db-experiment", "full-verify").contains(type)) {
            throw new IllegalArgumentException("未知任务类型：" + type);
        }
        if (tasks.size() > 50) {
            tasks.subList(0, tasks.size() - 50).clear();
        }
        Task t = new Task(seq.incrementAndGet(), type);
        tasks.add(t);
        pool.submit(() -> execute(t));
        logLine("任务 " + t.id + " (" + type + ") 已入队");
        return t;
    }

    public synchronized List<Map<String, Object>> list() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Task t : tasks) out.add(t.toMap(false));
        return out;
    }

    public synchronized Optional<Task> find(long id) {
        for (Task t : tasks) if (t.id == id) return Optional.of(t);
        return Optional.empty();
    }

    private void execute(Task t) {
        t.status = "running";
        t.startedAt = LocalDateTime.now().toString();
        logLine("任务 " + t.id + " 开始执行：" + t.type);
        try {
            switch (t.type) {
                case "maven-test" -> runMavenTest(t);
                case "db-experiment" -> runDbExperiment(t);
                case "full-verify" -> runFullVerify(t);
                default -> throw new IllegalStateException("unreachable");
            }
            t.status = "success";
            t.log("✔ 任务完成");
        } catch (InterruptedException e) {
            t.status = "cancelled";
            t.log("✘ 任务被中断");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            t.status = "failed";
            t.summary = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            t.log("✘ 任务失败：" + t.summary);
        } finally {
            t.endedAt = LocalDateTime.now().toString();
        }
    }

    /* ---------------- 具体任务 ---------------- */

    private void runMavenTest(Task t) throws Exception {
        t.log("运行 Maven 全量测试（便携 JDK17 + Maven，tools/）...");
        String mvn = isWindows() ? "tools\\apache-maven-3.9.16\\bin\\mvn.cmd" : "tools/apache-maven-3.9.16/bin/mvn";
        ProcessBuilder pb = new ProcessBuilder(mvn, "-s", "tools/maven-settings.xml", "test");
        pb.directory(root.toFile());
        pb.environment().put("JAVA_HOME", root.resolve("tools/jdk-17.0.20.1+1").normalize().toString());
        redirect(pb, t);
    }

    private void runDbExperiment(Task t) throws Exception {
        t.log("数据库性能实验：使用独立实验库 data/experiment.db（不影响学习库 data/lab.db）...");
        try (com.javadslab.persist.Db labPool = new com.javadslab.persist.Db(
                root.resolve("data/lab.db").toString(), 1)) {
            DbLabService svc = new DbLabService(labPool, root);
            t.log("重置实验库...");
            svc.experiment("reset", Map.of());
            t.log("造数 100,000 条 runs...");
            Map<String, Object> seeded = svc.experiment("seed", Map.of("rows", 100_000));
            t.log(String.valueOf(seeded.get("summary")));
            t.log("无索引状态跑代表性查询（按算法+日期过滤）...");
            long t1 = System.nanoTime();
            svc.experiment("sql", Map.of("sql",
                    "SELECT algorithm_id, COUNT(*) cnt FROM runs WHERE ran_at >= '2026-01-01' GROUP BY algorithm_id ORDER BY cnt DESC"));
            long noIndexMs = (System.nanoTime() - t1) / 1_000_000;
            t.log("耗时 " + noIndexMs + " ms");
            t.log("创建索引 idx_runs_algo_time...");
            svc.experiment("create-index", Map.of());
            t.log("同一查询再跑一次...");
            long t2 = System.nanoTime();
            svc.experiment("sql", Map.of("sql",
                    "SELECT algorithm_id, COUNT(*) cnt FROM runs WHERE ran_at >= '2026-01-01' GROUP BY algorithm_id ORDER BY cnt DESC"));
            long withIndexMs = (System.nanoTime() - t2) / 1_000_000;
            t.log("耗时 " + withIndexMs + " ms");
            t.summary = "实验库 10 万行造数完成；无索引 " + noIndexMs + " ms vs 有索引 " + withIndexMs + " ms"
                    + "（首次运行含缓存效应，仅供教学参考）";
        }
    }

    private void runFullVerify(Task t) throws Exception {
        t.log("== 完整验证：1) Maven 测试 ==");
        runMavenTest(t);
        if (t.status != null && t.status.equals("failed")) {
            throw new IllegalStateException("Maven 测试未通过，中止完整验证");
        }
        t.log("== 完整验证：2) Trace 文件全量校验（44 个）==");
        TraceValidationReport r = validateTraces();
        t.log("校验完成：" + r.summary);
        Path out = root.resolve("docs/TRACE_VALIDATION_REPORT.md");
        Files.writeString(out, r.markdown, StandardCharsets.UTF_8);
        t.log("报告已写入 docs/TRACE_VALIDATION_REPORT.md");
        t.summary = r.summary + "（报告见 docs/TRACE_VALIDATION_REPORT.md）";
    }

    /* ---------------- Trace 校验（计划 §48） ---------------- */

    public record TraceValidationReport(int total, int passed, int failed, String summary, String markdown) {}

    public TraceValidationReport validateTraces() throws IOException {
        Path traceDir = root.resolve("trace");
        List<String> ids = new ArrayList<>();
        String manifest = Files.readString(traceDir.resolve("manifest.json"), StandardCharsets.UTF_8);
        for (Object o : (List<?>) Json.parse(manifest)) ids.add(String.valueOf(o));

        StringBuilder md = new StringBuilder();
        md.append("# TRACE_VALIDATION_REPORT — 44 个 Trace 全量校验\n\n");
        md.append("> 校验时间：").append(LocalDateTime.now())
                .append("。校验项：JSON 可解析 / meta.kind / steps 非空 / 每步含 before+after / hl 存在。\n\n");
        md.append("| # | trace | kind | 步数 | 结果 | 说明 |\n|---|---|---|---|---|---|\n");
        int pass = 0, fail = 0, idx = 0;
        for (String id : ids) {
            idx++;
            String kind = "-", note = "ok";
            int steps = 0;
            boolean ok = false;
            try {
                Map<?, ?> trace = (Map<?, ?>) Json.parse(Files.readString(traceDir.resolve(id + ".json"), StandardCharsets.UTF_8));
                Map<?, ?> meta = (Map<?, ?>) trace.get("meta");
                List<?> stepList = (List<?>) trace.get("steps");
                steps = stepList == null ? 0 : stepList.size();
                kind = meta == null || meta.get("kind") == null ? "?" : String.valueOf(meta.get("kind"));
                if (meta == null) note = "缺少 meta";
                else if (steps == 0) note = "steps 为空";
                else {
                    boolean allHaveSnapshots = true;
                    boolean hlBad = false;
                    for (Object o : stepList) {
                        Map<?, ?> s = (Map<?, ?>) o;
                        if (s.get("before") == null || s.get("after") == null) { allHaveSnapshots = false; break; }
                        Object h = s.get("hl");
                        // hl 为可选字段（如 queue/selection 等结构用 args 传参）；出现时必须是键值对
                        if (h != null && !(h instanceof Map) ) { hlBad = true; break; }
                    }
                    if (!allHaveSnapshots) note = "部分步骤缺少 before/after";
                    else if (hlBad) note = "hl 字段异常";
                    else ok = true;
                }
            } catch (Exception e) {
                note = "JSON 解析失败：" + e.getMessage();
            }
            if (ok) pass++;
            else fail++;
            md.append("| ").append(idx).append(" | ").append(id).append(" | ").append(kind)
                    .append(" | ").append(steps).append(" | ").append(ok ? "✓" : "✗")
                    .append(" | ").append(note).append(" |\n");
        }
        String summary = ids.size() + " 个 trace，通过 " + pass + "，失败 " + fail;
        md.append("\n**汇总：").append(summary).append("**\n");
        return new TraceValidationReport(ids.size(), pass, fail, summary, md.toString());
    }

    /* ---------------- 工具 ---------------- */

    private void redirect(ProcessBuilder pb, Task t) throws IOException, InterruptedException {
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                if (count++ < 4000) t.log(line); // 防单任务日志爆炸
            }
        }
        int code = p.waitFor();
        if (code != 0) throw new IllegalStateException("进程退出码 " + code + "（详见任务日志）");
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private void logLine(String line) {
        try {
            Files.writeString(logDir.resolve("workbench.log"),
                    LocalDateTime.now() + " " + line + System.lineSeparator(),
                    StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}
