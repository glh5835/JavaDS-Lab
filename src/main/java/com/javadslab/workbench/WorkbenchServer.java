package com.javadslab.workbench;

import com.javadslab.persist.Db;
import com.javadslab.persist.SeedData;
import com.javadslab.trace.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * JavaDS-Lab 学习工作台服务（计划 §1.1 唯一入口；§26 API 设计；§28 安全边界）。
 *
 * - 仅监听 127.0.0.1；
 * - /api/* 统一 JSON 协议，错误统一 {"success":false,"error":{code,message,field}}（§27），
 *   不把 Java 堆栈返回浏览器；
 * - 静态资源只从 web/workbench/ 读取，路径规范化防穿越（§29）；
 * - 源码查看只允许 src/main/java/com/javadslab/ 下 .java 文件（白名单目录）。
 *
 * 启动：java -cp ... com.javadslab.workbench.WorkbenchServer [端口] [项目根]
 */
public final class WorkbenchServer {

    public static final String VERSION = "1.0.0";

    private final Db db;
    private final WorkbenchDao dao;
    private final PracticeLibrary practices;
    private final LabRunner lab;
    private final DbLabService dbLab;
    private final TaskManager tasks;
    private final Path root;
    private final Path staticRoot;
    final HttpServer server; // 包内可见：测试用随机端口启动后读取实际端口

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8630;
        Path root = Paths.get(args.length > 1 ? args[1] : ".").toAbsolutePath().normalize();
        WorkbenchServer s = new WorkbenchServer(port, root);
        s.start();
        System.out.println("JavaDS-Lab 学习工作台已启动：http://127.0.0.1:" + port + "/");
        System.out.println("项目根目录：" + root);
        System.out.println("按 Ctrl+C 停止服务。");
        Thread.currentThread().join(); // 前台常驻
    }

    public WorkbenchServer(int port, Path root) throws Exception {
        this.root = root;
        this.staticRoot = root.resolve("web/workbench");
        Files.createDirectories(root.resolve("logs"));
        this.db = new Db(root.resolve("data/lab.db").toString(), 4);
        SeedData.applySchema(db, root.resolve("sql/schema.sql"));
        this.dao = new WorkbenchDao(db);
        dao.migrate();
        dao.seedMissingFeatures();
        this.practices = new PracticeLibrary(root.resolve("docs/notes"));
        this.lab = new LabRunner();
        this.dbLab = new DbLabService(db, root);
        this.tasks = new TaskManager(root);
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", this::dispatch);
        server.setExecutor(Executors.newFixedThreadPool(8));
        log("服务初始化完成：功能 " + FeatureCatalog.count() + " 个，练习题 " + practices.count() + " 道");
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
        db.close();
    }

    /* ==================== 请求分发 ==================== */

    private void dispatch(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            if (path.startsWith("/api/")) {
                handleApi(ex, method, path);
            } else {
                handleStatic(ex, method, path);
            }
        } catch (Exception e) {
            log("请求处理异常: " + ex.getRequestURI() + " -> " + e);
            sendError(ex, 500, "INTERNAL", "服务器内部错误，请查看 logs/workbench.log", null);
        } finally {
            ex.close();
        }
    }

    /* ==================== API ==================== */

    private void handleApi(HttpExchange ex, String method, String path) throws IOException {
        String route = path.substring(5); // 去掉 /api/
        String[] seg = route.split("/");
        Map<String, Object> body = Map.of();
        if (method.equals("POST") || method.equals("PUT")) {
            String raw = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (!raw.isBlank()) {
                try {
                    Object parsed = Json.parse(raw);
                    if (parsed instanceof Map<?, ?> m) {
                        Map<String, Object> mm = new HashMap<>();
                        m.forEach((k, v) -> mm.put(String.valueOf(k), v));
                        body = mm;
                    }
                } catch (RuntimeException e) {
                    sendError(ex, 400, "INVALID_INPUT", "请求体不是合法 JSON", null);
                    return;
                }
            }
        }
        Map<String, String> q = queryParams(ex);

        try {
            // ---------- 健康检查 ----------
            if (route.equals("health")) {
                String dbStatus = "ok";
                try (var st = db.borrow().createStatement();
                     var rs = st.executeQuery("SELECT 1")) {
                    rs.next();
                } catch (Exception e) {
                    dbStatus = "error: " + e.getMessage();
                }
                sendOk(ex, Map.of("status", dbStatus.equals("ok") ? "ok" : "degraded",
                        "database", dbStatus, "version", VERSION,
                        "features", FeatureCatalog.count(), "practices", practices.count()));
                return;
            }

            // ---------- 功能目录 ----------
            if (route.equals("features") && method.equals("GET")) {
                String category = q.get("category");
                String kw = q.get("q");
                List<Map<String, Object>> out = new ArrayList<>();
                for (FeatureCatalog.FeatureDef d : FeatureCatalog.all()) {
                    if (category != null && !category.isBlank() && !category.equals(d.category())) continue;
                    if (kw != null && !kw.isBlank()
                            && !(d.name() + d.id() + d.description()).toLowerCase().contains(kw.toLowerCase())) continue;
                    Map<String, Object> m = FeatureCatalog.toMap(d);
                    m.put("learningState", learningStateOf(d.id()));
                    m.put("practiceCount", practices.byFeature(d.id()).size());
                    out.add(m);
                }
                sendOk(ex, Map.of("total", out.size(), "catalogTotal", FeatureCatalog.count(),
                        "categories", FeatureCatalog.CATEGORIES, "features", out));
                return;
            }
            if (seg.length == 2 && seg[0].equals("features") && method.equals("GET")) {
                var def = FeatureCatalog.find(seg[1]);
                if (def.isEmpty()) {
                    sendError(ex, 404, "NOT_FOUND", "功能不存在：" + seg[1], null);
                    return;
                }
                FeatureCatalog.FeatureDef d = def.get();
                Map<String, Object> m = FeatureCatalog.toMap(d);
                m.put("learningState", learningStateOf(d.id()));
                m.put("practiceCount", practices.byFeature(d.id()).size());
                m.put("practiceIds", practices.byFeature(d.id()).stream().map(PracticeLibrary.Practice::practiceId).toList());
                m.put("dependents", FeatureCatalog.dependents(d.id()).stream().map(FeatureCatalog.FeatureDef::id).toList());
                m.put("mistakeCount", countMistakesByAlgo(d.algoName()));
                sendOk(ex, m);
                return;
            }

            // ---------- 算法运行 ----------
            if (route.equals("runs")) {
                if (method.equals("POST")) {
                    String featureId = str(body, "featureId");
                    String inputJson = str(body, "input");
                    String source = body.getOrDefault("source", "user").toString();
                    var def = FeatureCatalog.find(featureId);
                    if (def.isEmpty()) {
                        sendError(ex, 404, "NOT_FOUND", "未知功能：" + featureId, "featureId");
                        return;
                    }
                    LabRunner.LabResult r = lab.run(featureId, inputJson); // 可能抛 InvalidInput
                    String runId = dao.saveRun(featureId, inputJson, r.resultJson(),
                            r.traceJson() != null, r.traceJson(), r.elapsedNs(), source);
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("runId", runId);
                    out.put("featureId", featureId);
                    out.put("result", Json.parse(r.resultJson()));
                    out.put("traceAvailable", r.traceJson() != null);
                    out.put("elapsedNs", r.elapsedNs());
                    sendOk(ex, out);
                    return;
                }
                if (method.equals("GET")) {
                    int limit = intQ(q, "limit", 20, 100);
                    sendOk(ex, Map.of("runs", dao.recentRuns(q.get("featureId"), limit)));
                    return;
                }
            }
            if (seg.length == 2 && seg[0].equals("runs") && method.equals("GET")) {
                var run = dao.runById(seg[1], false);
                if (run.isEmpty()) {
                    sendError(ex, 404, "NOT_FOUND", "运行记录不存在：" + seg[1], null);
                    return;
                }
                sendOk(ex, run.get());
                return;
            }
            if (seg.length == 3 && seg[0].equals("runs") && seg[2].equals("trace") && method.equals("GET")) {
                var run = dao.runById(seg[1], true);
                if (run.isEmpty()) {
                    sendError(ex, 404, "NOT_FOUND", "运行记录不存在：" + seg[1], null);
                    return;
                }
                String traceJson = (String) run.get().get("traceJson");
                if (traceJson == null) {
                    sendError(ex, 404, "NO_TRACE", "该运行没有保存 Trace（可能仅结果模式执行）", null);
                    return;
                }
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("runId", run.get().get("runId"));
                out.put("featureId", run.get().get("featureId"));
                out.put("trace", Json.parse(traceJson));
                sendOk(ex, out);
                return;
            }

            // ---------- 练习 ----------
            if (route.equals("practices") && method.equals("GET")) {
                String featureId = q.get("featureId");
                String kw = q.get("q");
                List<PracticeLibrary.Practice> list = featureId != null && !featureId.isBlank()
                        ? practices.byFeature(featureId) : practices.all();
                List<Map<String, Object>> out = new ArrayList<>();
                for (PracticeLibrary.Practice p : list) {
                    if (kw != null && !kw.isBlank()
                            && !(p.question() + p.answer()).toLowerCase().contains(kw.toLowerCase())) continue;
                    Map<String, Object> m = practiceMap(p, false); // 列表不带答案（§6.1）
                    out.add(m);
                }
                sendOk(ex, Map.of("total", out.size(), "practices", out));
                return;
            }
            if (seg.length == 2 && seg[0].equals("practices") && method.equals("GET")) {
                var p = practices.find(seg[1]);
                if (p.isEmpty()) {
                    sendError(ex, 404, "NOT_FOUND", "练习不存在：" + seg[1], null);
                    return;
                }
                sendOk(ex, practiceMap(p.get(), true)); // 详情带答案；前端默认折叠（§6.3）
                return;
            }
            if (route.equals("practice-attempts") && method.equals("POST")) {
                String pid = str(body, "practiceId");
                var p = practices.find(pid);
                if (p.isEmpty()) {
                    sendError(ex, 404, "NOT_FOUND", "练习不存在：" + pid, "practiceId");
                    return;
                }
                dao.saveAttempt(pid, p.get().featureId(), str(body, "answer"),
                        Boolean.TRUE.equals(body.get("revealed")),
                        body.get("selfRating") == null ? null : body.get("selfRating").toString());
                sendOk(ex, Map.of("saved", true));
                return;
            }
            if (route.equals("practice-drafts")) {
                if (method.equals("GET")) {
                    sendOk(ex, Map.of("drafts", dao.allDrafts()));
                    return;
                }
            }
            if (seg.length == 2 && seg[0].equals("practice-drafts")) {
                if (method.equals("PUT")) {
                    dao.saveDraft(seg[1], str(body, "answer"), Boolean.TRUE.equals(body.get("revealed")));
                    sendOk(ex, Map.of("saved", true));
                    return;
                }
                if (method.equals("DELETE")) {
                    dao.saveDraft(seg[1], "", false);
                    sendOk(ex, Map.of("deleted", true));
                    return;
                }
            }
            if (route.equals("practice-attempts") && method.equals("GET")) {
                sendOk(ex, Map.of("attempts", dao.attemptsByFeature(q.get("featureId"),
                        intQ(q, "limit", 50, 200))));
                return;
            }

            // ---------- 学习状态 ----------
            if (route.equals("learning") && method.equals("GET")) {
                sendOk(ex, Map.of("states", dao.allLearningStates()));
                return;
            }
            if (seg.length == 2 && seg[0].equals("learning") && method.equals("PUT")) {
                String status = str(body, "status");
                if (!dao.setLearningState(seg[1], status)) {
                    sendError(ex, 400, "INVALID_INPUT", "status 只能是 unlearned/reviewing/mastered", "status");
                    return;
                }
                sendOk(ex, Map.of("saved", true));
                return;
            }

            // ---------- 错题 ----------
            if (route.equals("mistakes")) {
                if (method.equals("GET")) {
                    sendOk(ex, Map.of("mistakes", dao.listMistakes(q.get("algoName"), q.get("q"),
                            intQ(q, "limit", 100, 500))));
                    return;
                }
                if (method.equals("POST")) {
                    String algoName = str(body, "algoName");
                    var def = FeatureCatalog.find(str(body, "featureId"));
                    if ((algoName == null || algoName.isBlank()) && def.isPresent()) algoName = def.get().algoName();
                    if (algoName == null || algoName.isBlank()) {
                        sendError(ex, 400, "INVALID_INPUT", "缺少知识点（featureId 或 algoName）", "algoName");
                        return;
                    }
                    String question = str(body, "question");
                    String wrong = str(body, "wrongAnswer");
                    String reason = str(body, "reason");
                    if (question.isBlank() || wrong.isBlank()) {
                        sendError(ex, 400, "INVALID_INPUT", "题目与错误答案不能为空", "question");
                        return;
                    }
                    long id = dao.addMistake(algoName, question, wrong, reason);
                    sendOk(ex, Map.of("id", id));
                    return;
                }
            }
            if (seg.length == 2 && seg[0].equals("mistakes")) {
                long id;
                try {
                    id = Long.parseLong(seg[1]);
                } catch (NumberFormatException e) {
                    sendError(ex, 400, "INVALID_INPUT", "错题 id 必须是数字", "id");
                    return;
                }
                if (method.equals("GET")) {
                    var m = dao.mistakeById(id);
                    if (m.isEmpty()) {
                        sendError(ex, 404, "NOT_FOUND", "错题不存在：" + id, null);
                        return;
                    }
                    Map<String, Object> out = new LinkedHashMap<>(m.get());
                    out.put("reviews", dao.reviewsOfMistake(id));
                    sendOk(ex, out);
                    return;
                }
                if (method.equals("PUT")) {
                    boolean ok = dao.updateMistake(id, str(body, "question"), str(body, "wrongAnswer"), str(body, "reason"));
                    sendOk(ex, Map.of("updated", ok));
                    return;
                }
                if (method.equals("DELETE")) {
                    sendOk(ex, Map.of("deleted", dao.deleteMistake(id)));
                    return;
                }
            }
            if (seg.length == 3 && seg[0].equals("mistakes") && seg[2].equals("reviews") && method.equals("POST")) {
                long id;
                try {
                    id = Long.parseLong(seg[1]);
                } catch (NumberFormatException e) {
                    sendError(ex, 400, "INVALID_INPUT", "错题 id 必须是数字", "id");
                    return;
                }
                String result = str(body, "result");
                boolean ok = dao.addMistakeReview(id, str(body, "answer"), result);
                if (!ok) {
                    sendError(ex, 404, "NOT_FOUND", "错题不存在或 result 非法（fail/partial/success）：" + id, "result");
                    return;
                }
                sendOk(ex, Map.of("saved", true));
                return;
            }
            if (route.equals("weakness") && method.equals("GET")) {
                sendOk(ex, Map.of("weakness", dao.weakness()));
                return;
            }

            // ---------- 笔记 ----------
            if (route.equals("notes") && method.equals("GET")) {
                List<Map<String, Object>> out = new ArrayList<>();
                practices.noteTitles().forEach((noteId, title) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("noteId", noteId);
                    m.put("title", title);
                    m.put("featureIds", FeatureCatalog.byNote(noteId).stream().map(FeatureCatalog.FeatureDef::id).toList());
                    out.add(m);
                });
                sendOk(ex, Map.of("total", out.size(), "notes", out));
                return;
            }
            if (seg.length == 2 && seg[0].equals("notes") && method.equals("GET")) {
                Path note = root.resolve("docs/notes/" + seg[1] + ".md").normalize();
                if (!seg[1].matches("[0-9a-z\\-]+") || !note.startsWith(root.resolve("docs/notes")) || !Files.exists(note)) {
                    sendError(ex, 404, "NOT_FOUND", "笔记不存在：" + seg[1], null);
                    return;
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("noteId", seg[1]);
                m.put("title", practices.noteTitles().getOrDefault(seg[1], seg[1]));
                m.put("markdown", Files.readString(note, StandardCharsets.UTF_8));
                m.put("featureIds", FeatureCatalog.byNote(seg[1]).stream().map(FeatureCatalog.FeatureDef::id).toList());
                sendOk(ex, m);
                return;
            }

            // ---------- 复习地图 ----------
            if (route.equals("review/map") && method.equals("GET")) {
                List<Map<String, Object>> out = new ArrayList<>();
                for (FeatureCatalog.FeatureDef d : FeatureCatalog.all()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", d.id());
                    m.put("name", d.name());
                    m.put("category", d.category());
                    m.put("prerequisites", d.prerequisites());
                    m.put("learningState", learningStateOf(d.id()));
                    m.put("practiceCount", practices.byFeature(d.id()).size());
                    m.put("mistakeCount", countMistakesByAlgo(d.algoName()));
                    out.add(m);
                }
                sendOk(ex, Map.of("features", out));
                return;
            }

            // ---------- 数据库实验 ----------
            if (route.equals("database/queries") && method.equals("GET")) {
                sendOk(ex, Map.of("queries", dbLab.queryCatalog()));
                return;
            }
            if (route.equals("database/query") && method.equals("POST")) {
                sendOk(ex, dbLab.runCatalogQuery(str(body, "queryId"), body.get("param") == null ? null : body.get("param").toString()));
                return;
            }
            if (route.equals("database/status") && method.equals("GET")) {
                sendOk(ex, dbLab.experimentStatus());
                return;
            }
            if (route.equals("database/schema") && method.equals("GET")) {
                // 表关系/表结构读取 sql/ 目录真实文件（§23 禁止模拟数据）
                List<String> tables = new ArrayList<>();
                Connection cn = db.borrow();
                try (var st = cn.createStatement();
                     var rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")) {
                    while (rs.next()) tables.add(rs.getString(1));
                } catch (Exception ignored) {
                } finally {
                    db.release(cn);
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("tables", tables);
                Path er = root.resolve("sql/ER.md");
                m.put("er", Files.exists(er) ? Files.readString(er, StandardCharsets.UTF_8) : "");
                Path schema = root.resolve("sql/schema.sql");
                m.put("schema", Files.exists(schema) ? Files.readString(schema, StandardCharsets.UTF_8) : "");
                sendOk(ex, m);
                return;
            }
            if (route.equals("database/experiment") && method.equals("POST")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> params = body.get("params") instanceof Map<?, ?> pm
                        ? (Map<String, Object>) pm : Map.of();
                sendOk(ex, dbLab.experiment(str(body, "op"), params));
                return;
            }

            // ---------- 后台任务 ----------
            if (route.equals("tasks")) {
                if (method.equals("POST")) {
                    TaskManager.Task t = tasks.submit(str(body, "type"));
                    sendOk(ex, t.toMap(false));
                    return;
                }
                if (method.equals("GET")) {
                    sendOk(ex, Map.of("tasks", tasks.list()));
                    return;
                }
            }
            if (seg.length == 2 && seg[0].equals("tasks") && method.equals("GET")) {
                var t = tasks.find(Long.parseLong(seg[1]));
                if (t.isEmpty()) {
                    sendError(ex, 404, "NOT_FOUND", "任务不存在：" + seg[1], null);
                    return;
                }
                sendOk(ex, t.get().toMap(true));
                return;
            }

            // ---------- 源码查看（白名单） ----------
            if (route.equals("source") && method.equals("GET")) {
                String rel = q.get("path") == null ? "" : q.get("path");
                Path p = root.resolve(rel).normalize();
                if (!rel.startsWith("src/main/java/com/javadslab/") || !rel.endsWith(".java")
                        || !p.startsWith(root.resolve("src/main/java")) || !Files.exists(p)) {
                    sendError(ex, 403, "FORBIDDEN", "仅允许查看 src/main/java/com/javadslab/ 下的 Java 源码", "path");
                    return;
                }
                sendOk(ex, Map.of("path", rel, "content", Files.readString(p, StandardCharsets.UTF_8)));
                return;
            }

            // ---------- 历史报告（明确标注为历史文件，§25） ----------
            if (route.equals("report/jdbc") && method.equals("GET")) {
                Path r = root.resolve("reports/jdbc-report.md");
                if (!Files.exists(r)) {
                    sendError(ex, 404, "NOT_FOUND", "历史报告不存在（可运行数据库性能任务生成新的实测数据）", null);
                    return;
                }
                sendOk(ex, Map.of("path", "reports/jdbc-report.md",
                        "markdown", Files.readString(r, StandardCharsets.UTF_8),
                        "kind", "历史报告：项目已有文件，不是本次刚执行的结果"));
                return;
            }

            // ---------- 继续学习上下文 ----------
            if (route.equals("context")) {
                if (method.equals("GET")) {
                    String v = dao.getKv("continue-context");
                    sendOk(ex, Map.of("context", v == null ? Map.of() : Json.parse(v)));
                    return;
                }
                if (method.equals("PUT")) {
                    body.remove("savedAt");
                    Map<String, Object> ctx = new LinkedHashMap<>(body);
                    ctx.put("savedAt", java.time.LocalDateTime.now().toString());
                    dao.saveKv("continue-context", Json.write(ctx));
                    sendOk(ex, Map.of("saved", true));
                    return;
                }
            }

            // ---------- 首页聚合 ----------
            if (route.equals("home") && method.equals("GET")) {
                Map<String, Object> out = new LinkedHashMap<>();
                Map<String, String> states = new LinkedHashMap<>();
                dao.allLearningStates().forEach(m -> states.put((String) m.get("featureId"), (String) m.get("status")));
                out.put("states", states);
                out.put("featureCount", FeatureCatalog.count());
                out.put("practiceCount", practices.count());
                out.put("mistakeCount", dao.countMistakes());
                out.put("recentRuns", dao.recentRuns(null, 5));
                out.put("weakness", dao.weakness().subList(0, Math.min(5, dao.weakness().size())));
                String ctx = dao.getKv("continue-context");
                out.put("continueContext", ctx == null ? Map.of() : Json.parse(ctx));
                sendOk(ex, out);
                return;
            }

            sendError(ex, 404, "NOT_FOUND", "未知接口：" + method + " /api/" + route, null);
        } catch (LabRunner.InvalidInput e) {
            sendError(ex, 400, "INVALID_INPUT", e.getMessage(), e.field);
        } catch (IllegalArgumentException e) {
            sendError(ex, 400, "INVALID_INPUT", e.getMessage(), null);
        } catch (IllegalStateException e) {
            sendError(ex, 500, "EXECUTION_ERROR", e.getMessage(), null);
        } catch (Exception e) {
            log("API 异常 " + route + ": " + e);
            sendError(ex, 500, "INTERNAL", "服务器内部错误：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()), null);
        }
    }

    /* ==================== 静态资源 ==================== */

    private void handleStatic(HttpExchange ex, String method, String path) throws IOException {
        if (!method.equals("GET") && !method.equals("HEAD")) {
            sendError(ex, 405, "METHOD_NOT_ALLOWED", "仅支持 GET", null);
            return;
        }
        String rel = path.equals("/") ? "index.html" : path.substring(1);
        Path file = staticRoot.resolve(rel).normalize();
        if (!file.startsWith(staticRoot) || !Files.isRegularFile(file)) {
            sendError(ex, 404, "NOT_FOUND", "页面不存在（Hash Router 页面请从首页进入，如 /#/practice）", null);
            return;
        }
        byte[] bytes = Files.readAllBytes(file);
        String mime = switch (rel.substring(rel.lastIndexOf('.') + 1)) {
            case "html" -> "text/html; charset=utf-8";
            case "css" -> "text/css; charset=utf-8";
            case "js", "mjs" -> "text/javascript; charset=utf-8";
            case "json" -> "application/json; charset=utf-8";
            case "svg" -> "image/svg+xml";
            case "png" -> "image/png";
            case "ico" -> "image/x-icon";
            default -> "application/octet-stream";
        };
        ex.getResponseHeaders().set("Content-Type", mime);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    /* ==================== 响应工具 ==================== */

    private void sendOk(HttpExchange ex, Object data) throws IOException {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("data", data);
        send(ex, 200, Json.write(out));
    }

    private void sendError(HttpExchange ex, int code, String errCode, String message, String field) throws IOException {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("code", errCode);
        err.put("message", message);
        if (field != null) err.put("field", field);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("error", err);
        send(ex, code, Json.write(out));
    }

    private void send(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        // API 响应禁缓存：既防数据陈旧，也保证断线检测不被浏览器缓存掩盖
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> queryParams(HttpExchange ex) {
        Map<String, String> out = new HashMap<>();
        String query = ex.getRequestURI().getRawQuery();
        if (query == null) return out;
        for (String pair : query.split("&")) {
            int i = pair.indexOf('=');
            if (i <= 0) continue;
            out.put(URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
        }
        return out;
    }

    private String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? "" : v.toString();
    }

    private int intQ(Map<String, String> q, String key, int dft, int max) {
        String v = q.get(key);
        if (v == null || v.isBlank()) return dft;
        try {
            return Math.max(1, Math.min(max, Integer.parseInt(v)));
        } catch (NumberFormatException e) {
            return dft;
        }
    }

    private String learningStateOf(String featureId) {
        for (Map<String, Object> m : dao.allLearningStates()) {
            if (featureId.equals(m.get("featureId"))) return (String) m.get("status");
        }
        return "unlearned";
    }

    private long countMistakesByAlgo(String algoName) {
        return dao.listMistakes(algoName, null, 1_000_000).size();
    }

    private Map<String, Object> practiceMap(PracticeLibrary.Practice p, boolean withAnswer) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("practiceId", p.practiceId());
        m.put("noteId", p.noteId());
        m.put("noteTitle", p.noteTitle());
        m.put("featureId", p.featureId());
        m.put("featureIds", p.featureIds());
        m.put("index", p.index());
        m.put("question", p.question());
        if (withAnswer) m.put("answer", p.answer());
        return m;
    }

    private void log(String line) {
        try {
            Files.writeString(root.resolve("logs/workbench.log"),
                    java.time.LocalDateTime.now() + " " + line + System.lineSeparator(),
                    StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}
