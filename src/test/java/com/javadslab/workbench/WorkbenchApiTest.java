package com.javadslab.workbench;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工作台 API 集成测试（计划 §45）：在随机端口启动真实服务，覆盖
 * 正常参数 / 空参数 / 非法参数 / 边界值 / 数据规模限制 / 数据库异常 / 学习状态 / 错题闭环。
 * 断言只依赖稳定语义（success/error.code/字段名），不依赖 lab.db 中既有数据量。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkbenchApiTest {

    private static WorkbenchServer server;
    private static String base;
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @BeforeAll
    static void boot() throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        server = new WorkbenchServer(0, root); // 端口 0 = 随机空闲端口
        server.start();
        base = "http://127.0.0.1:" + server.server.getAddress().getPort();
    }

    @AfterAll
    static void shutdown() {
        if (server != null) server.stop();
    }

    private record Resp(int status, String body) {
        boolean ok() { return status == 200 && body.contains("\"success\":true"); }
        String errorCode() {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\"code\":\"([A-Z_]+)\"").matcher(body);
            return m.find() ? m.group(1) : "";
        }
    }

    private static Resp call(String method, String path, String json) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        if (json != null) b.header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(json));
        else b.method(method, HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Resp(r.statusCode(), r.body());
    }

    /* ---------- 健康检查（§34） ---------- */

    @Test
    @Order(1)
    void healthOk() throws Exception {
        Resp r = call("GET", "/api/health", null);
        assertTrue(r.ok(), r.body());
        assertTrue(r.body().contains("\"database\":\"ok\""));
        assertTrue(r.body().contains("\"features\":44"));
        assertTrue(r.body().contains("\"practices\":110"));
    }

    /* ---------- 功能目录 ---------- */

    @Test
    @Order(2)
    void featureCatalogComplete() throws Exception {
        Resp r = call("GET", "/api/features", null);
        assertTrue(r.ok());
        assertTrue(r.body().contains("\"catalogTotal\":44"));
        // 不允许出现未知功能
        Resp bad = call("GET", "/api/features/no-such-feature", null);
        assertEquals(404, bad.status());
        assertTrue(bad.body().contains("NOT_FOUND"));
    }

    /* ---------- 算法运行：正常 / 空参 / 非法 / 边界 / 规模限制 ---------- */

    @Test
    @Order(3)
    void runSortNormalWithTrace() throws Exception {
        Resp r = call("POST", "/api/runs",
                "{\"featureId\":\"sort-bubble\",\"input\":\"{\\\"array\\\":[3,1,2]}\"}");
        assertTrue(r.ok(), r.body());
        assertTrue(r.body().contains("\"traceAvailable\":true"));
        assertTrue(r.body().contains("\"sorted\":[1,2,3]"));
        assertTrue(r.body().contains("\"runId\""));
    }

    @Test
    @Order(4)
    void runMissingFeatureId() throws Exception {
        Resp r = call("POST", "/api/runs", "{\"featureId\":\"\",\"input\":\"{}\"}");
        assertEquals(404, r.status());
        assertEquals("NOT_FOUND", r.errorCode());
        Resp empty = call("POST", "/api/runs", "{}");
        assertEquals(404, empty.status());
    }

    @Test
    @Order(5)
    void runBinarySearchRequiresAscending() throws Exception {
        Resp r = call("POST", "/api/runs",
                "{\"featureId\":\"binary-search\",\"input\":\"{\\\"array\\\":[5,3,1],\\\"target\\\":3}\"}");
        assertEquals(400, r.status());
        assertEquals("INVALID_INPUT", r.errorCode());
        assertTrue(r.body().contains("已经升序排列"), "必须明确提示升序要求（§10）");
        assertTrue(r.body().contains("\"field\":\"array\""));
        // 不得悄悄自动排序：无 trace/result 返回
        assertFalse(r.body().contains("\"sorted\""));
    }

    @Test
    @Order(6)
    void runEmptyOpsRejected() throws Exception {
        Resp r = call("POST", "/api/runs",
                "{\"featureId\":\"stack\",\"input\":\"{\\\"ops\\\":[]}\"}");
        assertEquals(400, r.status());
        assertEquals("INVALID_INPUT", r.errorCode());
        assertTrue(r.body().contains("\"field\":\"ops\""));
    }

    @Test
    @Order(7)
    void runOversizeArrayRejected() throws Exception {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 20_001; i++) sb.append(i % 10).append(',');
        sb.setLength(sb.length() - 1);
        sb.append(']');
        String input = "{\"featureId\":\"sort-bubble\",\"input\":\"{\\\"array\\\":" + sb + "}\"}";
        Resp r = call("POST", "/api/runs", input);
        assertEquals(400, r.status());
        assertEquals("INVALID_INPUT", r.errorCode());
        assertTrue(r.body().contains("长度不能超过"), "超限必须明确提示（§36）");
    }

    @Test
    @Order(8)
    void runInvalidJsonRejected() throws Exception {
        Resp r = call("POST", "/api/runs", "{\"featureId\":\"sort-bubble\",\"input\":\"{bad json\"}");
        assertEquals(400, r.status());
        assertEquals("INVALID_INPUT", r.errorCode());
    }

    @Test
    @Order(9)
    void graphEdgeValidation() throws Exception {
        // 边超出顶点范围
        Resp r = call("POST", "/api/runs",
                "{\"featureId\":\"bfs\",\"input\":\"{\\\"n\\\":3,\\\"edges\\\":[[0,9,1]]}\"}");
        assertEquals(400, r.status());
        assertTrue(r.body().contains("\"field\":\"edges\""));
        // Dijkstra 负权边必须报错
        Resp neg = call("POST", "/api/runs",
                "{\"featureId\":\"dijkstra\",\"input\":\"{\\\"n\\\":3,\\\"edges\\\":[[0,1,-2]]}\"}");
        assertEquals(400, neg.status());
        assertTrue(neg.body().contains("非负"));
        // Bellman-Ford 允许负权
        Resp ok = call("POST", "/api/runs",
                "{\"featureId\":\"bellman-ford\",\"input\":\"{\\\"n\\\":3,\\\"start\\\":0,\\\"edges\\\":[[0,1,-2],[1,2,3]]}\"}");
        assertTrue(ok.ok(), ok.body());
        assertTrue(ok.body().contains("\"dist\""));
    }

    /* ---------- 运行历史 / Trace ---------- */

    @Test
    @Order(10)
    void runHistoryAndTrace() throws Exception {
        Resp run = call("POST", "/api/runs",
                "{\"featureId\":\"sort-insertion\",\"input\":\"{\\\"array\\\":[2,1]}\"}");
        assertTrue(run.ok());
        String runId = java.util.regex.Pattern.compile("\"runId\":\"([^\"]+)\"")
                .matcher(run.body()).results().findFirst().orElseThrow().group(1);
        Resp trace = call("GET", "/api/runs/" + runId + "/trace", null);
        assertTrue(trace.ok(), trace.body());
        assertTrue(trace.body().contains("\"steps\""));
        assertTrue(trace.body().contains("\"before\""));
        assertTrue(trace.body().contains("\"hl\""));
        // 不存在的运行
        assertEquals(404, call("GET", "/api/runs/none/trace", null).status());
    }

    /* ---------- 学习状态（§5.4 状态只能由用户设置） ---------- */

    @Test
    @Order(11)
    void learningStateRoundtrip() throws Exception {
        Resp put = call("PUT", "/api/learning/heap", "{\"status\":\"reviewing\"}");
        assertTrue(put.ok(), put.body());
        Resp get = call("GET", "/api/learning", null);
        assertTrue(get.body().contains("\"featureId\":\"heap\""));
        assertTrue(get.body().contains("reviewing"));
        // 非法状态拒绝
        Resp bad = call("PUT", "/api/learning/heap", "{\"status\":\"auto-mastered\"}");
        assertEquals(400, bad.status());
        // 清理
        call("PUT", "/api/learning/heap", "{\"status\":\"unlearned\"}");
    }

    /* ---------- 练习 ---------- */

    @Test
    @Order(12)
    void practicesListWithoutAnswerAndDetailWithAnswer() throws Exception {
        Resp list = call("GET", "/api/practices?featureId=dp-lcs", null);
        assertTrue(list.ok());
        assertFalse(list.body().contains("\"answer\""), "练习列表不得泄漏参考答案（§6.1）");
        Resp detail = call("GET", "/api/practices/22-dp-15-1", null);
        assertTrue(detail.ok());
        assertTrue(detail.body().contains("\"answer\""));
        // 草稿保存与读取
        Resp draft = call("PUT", "/api/practice-drafts/22-dp-15-1",
                "{\"answer\":\"草稿测试\",\"revealed\":false}");
        assertTrue(draft.ok());
        Resp drafts = call("GET", "/api/practice-drafts", null);
        assertTrue(drafts.body().contains("草稿测试"));
    }

    /* ---------- 错题闭环（§15） ---------- */

    @Test
    @Order(13)
    void mistakeFullCycle() throws Exception {
        Resp add = call("POST", "/api/mistakes",
                "{\"featureId\":\"heap\",\"question\":\"API测试题\",\"wrongAnswer\":\"错误解\",\"reason\":\"概念混淆\"}");
        assertTrue(add.ok(), add.body());
        long id = Long.parseLong(java.util.regex.Pattern.compile("\"id\":(\\d+)")
                .matcher(add.body()).results().findFirst().orElseThrow().group(1));
        // 重做成功才累计 success_count
        assertTrue(call("POST", "/api/mistakes/" + id + "/reviews",
                "{\"answer\":\"解法A\",\"result\":\"fail\"}").ok());
        Resp detail = call("GET", "/api/mistakes/" + id, null);
        assertTrue(detail.body().contains("\"redoCount\":1"));
        assertTrue(detail.body().contains("\"successCount\":0"));
        assertTrue(call("POST", "/api/mistakes/" + id + "/reviews",
                "{\"answer\":\"解法B\",\"result\":\"success\"}").ok());
        detail = call("GET", "/api/mistakes/" + id, null);
        assertTrue(detail.body().contains("\"redoCount\":2"));
        assertTrue(detail.body().contains("\"successCount\":1"));
        // 非法 result 拒绝
        assertEquals(404, call("POST", "/api/mistakes/" + id + "/reviews",
                "{\"result\":\"maybe\"}").status());
        // 编辑 + 删除
        assertTrue(call("PUT", "/api/mistakes/" + id,
                "{\"question\":\"API测试题改\",\"wrongAnswer\":\"错误解\",\"reason\":\"边界条件遗漏\"}").ok());
        assertTrue(call("DELETE", "/api/mistakes/" + id, null).ok());
        assertEquals(404, call("GET", "/api/mistakes/" + id, null).status());
    }

    /* ---------- 数据库实验（§22） ---------- */

    @Test
    @Order(14)
    void databaseQueryAndGuards() throws Exception {
        Resp ok = call("POST", "/api/database/query", "{\"queryId\":\"Q1\",\"param\":\"5\"}");
        assertTrue(ok.ok(), ok.body());
        assertTrue(ok.body().contains("\"plan\""));
        assertTrue(ok.body().contains("\"columns\""));
        Resp bad = call("POST", "/api/database/query", "{\"queryId\":\"Q99\"}");
        assertEquals(400, bad.status());
        // 实验库自由 SQL 的越界防护
        Resp evil = call("POST", "/api/database/experiment",
                "{\"op\":\"sql\",\"params\":{\"sql\":\"ATTACH 'data/lab.db' AS x\"}}");
        assertEquals(400, evil.status());
        assertTrue(evil.body().contains("不允许"));
    }

    /* ---------- 源码白名单（§28/§29） ---------- */

    @Test
    @Order(15)
    void sourcePathWhitelist() throws Exception {
        Resp ok = call("GET", "/api/source?path=src/main/java/com/javadslab/core/heap/MyHeap.java", null);
        assertTrue(ok.ok(), ok.body());
        Resp traversal = call("GET", "/api/source?path=src/main/java/../../../lab.db", null);
        assertEquals(403, traversal.status());
        Resp outside = call("GET", "/api/source?path=pom.xml", null);
        assertEquals(403, outside.status());
    }
}
