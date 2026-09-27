package com.javadslab.persist;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** JDBC 持久化集成测试：建库 → 造数(含 10 万条压力造数) → DAO 全操作 → 错题本 → 查询烟测。 */
class PersistIntegrationTest {

    @TempDir
    Path tmp;

    private Db db;
    private Dao dao;

    @BeforeEach
    void setUp() throws Exception {
        Path dbFile = tmp.resolve("it-" + System.nanoTime() + ".db");
        db = new Db(dbFile.toString(), 3);
        dao = new Dao(db);
        SeedData.applySchema(db, Path.of("sql/schema.sql"));
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void schemaCreatesAllTables() throws Exception {
        // 建表脚本里全部表存在
        long tables = db.inTransaction(c -> {
            try (var rs = c.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN ('algorithms','runs','steps','mistakes')")) {
                rs.next();
                return rs.getLong(1);
            }
        });
        assertEquals(4, tables);
    }

    @Test
    void algorithmCrudAndUniqueConstraint() {
        long id = dao.insertAlgorithm("heap", "tree", 3, "二叉堆");
        assertTrue(id > 0);
        assertTrue(dao.findAlgorithmByName("heap").isPresent());
        assertEquals("tree", dao.findAlgorithmByName("heap").get().category());
        // 唯一约束生效
        assertThrows(Db.DataAccessException.class,
                () -> dao.insertAlgorithm("heap", "tree", 3, "重复"));
        assertEquals(1, dao.listAlgorithms().size());
    }

    @Test
    void runInsertAndCheckConstraints() {
        long algId = dao.insertAlgorithm("bst", "tree", 2, "BST");
        long runId = dao.insertRun(algId, 120, 1000, true, 88.5);
        assertTrue(runId > 0);
        assertEquals(1, dao.countRuns());
        // CHECK 约束：负耗时被拒
        assertThrows(Db.DataAccessException.class, () -> dao.insertRun(algId, -5, 100, true, null));
        // CHECK 约束：passed 只能 0/1
        assertThrows(Db.DataAccessException.class, () ->
                db.inTransaction(c -> {
                    try (var ps = c.prepareStatement("INSERT INTO runs(algorithm_id,duration_ms,input_size,passed) VALUES (?,?,?,?)")) {
                        ps.setLong(1, algId); ps.setInt(2, 1); ps.setInt(3, 1); ps.setInt(4, 7);
                        return ps.executeUpdate();
                    }
                }));
        // 外键约束：不存在的 algorithm 被拒
        assertThrows(Db.DataAccessException.class, () -> dao.insertRun(9999, 10, 10, true, null));
    }

    @Test
    void stepsRoundTrip() {
        long algId = dao.insertAlgorithm("bfs", "graph", 2, "BFS");
        long runId = dao.insertRun(algId, 5, 10, true, null);
        for (int i = 0; i < 5; i++) dao.insertStep(runId, i, "visit", "node-" + i);
        List<Dao.Step> steps = dao.stepsOfRun(runId);
        assertEquals(5, steps.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(i, steps.get(i).stepNo());
            assertEquals("node-" + i, steps.get(i).detail());
        }
    }

    @Test
    void cascadeDeleteRunsRemovesSteps() {
        long algId = dao.insertAlgorithm("kmp", "string", 4, "KMP");
        long runId = dao.insertRun(algId, 1, 1, true, null);
        dao.insertStep(runId, 0, "advance", "x");
        db.inTransaction(c -> {
            try (var ps = c.prepareStatement("DELETE FROM runs WHERE id = ?")) {
                ps.setLong(1, runId);
                ps.executeUpdate();
            }
            return null;
        });
        assertEquals(0, dao.stepsOfRun(runId).size());
    }

    @Test
    void mistakesWeaknessFlow() {
        long heap = dao.insertAlgorithm("heap", "tree", 3, "二叉堆");
        long rb = dao.insertAlgorithm("red-black-tree", "tree", 5, "红黑树");
        long m1 = dao.addMistake(heap, "手写堆第 3 题", "WA2", "下沉时漏了右孩子");
        long m2 = dao.addMistake(heap, "手写堆第 7 题", "WA1", "建堆起点写错");
        long m3 = dao.addMistake(rb, "红黑树删除", "RE", "兄弟为空的情形漏判");
        dao.markRedo(m1);
        dao.markRedo(m1);
        dao.markRedo(m3);
        // 重做不存在的错题返回 false
        assertFalse(dao.markRedo(12345));

        List<Dao.Weakness> weak = dao.weaknessReport();
        assertEquals(2, weak.size());
        assertEquals("heap", weak.get(0).algorithmName()); // 2 错 > 1 错
        assertEquals(2, weak.get(0).mistakeCount());
        assertEquals(1.0, weak.get(0).avgRedo()); // (2+0)/2
        assertEquals(1, weak.get(1).mistakeCount());

        assertEquals(2, dao.listMistakesByAlgorithm(heap).size());
        // 删除错题
        assertEquals(1, dao.deleteMistake(m2));
        assertEquals(1, dao.listMistakesByAlgorithm(heap).size());
    }

    /** 大规模：10 万条 runs 批量造数 + 10 条复杂查询全部可执行。 */
    @Test
    void bulkSeedAndTenQueries() {
        SeedData.SeedStats stats = SeedData.seed(db, dao, 100_000, 42L);
        assertEquals(100_000, stats.runs());
        assertTrue(stats.steps() > 0);
        assertTrue(stats.mistakes() > 0);

        // 10 条复杂查询全部能执行且返回 >= 0 行
        for (Queries.Query q : Queries.ALL) {
            long rows = db.inTransaction(c -> {
                try (var ps = c.prepareStatement(q.sql())) {
                    if (q.hasParam()) {
                        int v = q.name().startsWith("Q1") ? 10 : (q.name().startsWith("Q4") ? 50 : 365);
                        for (int i = 1; i <= ps.getParameterMetaData().getParameterCount(); i++) ps.setInt(i, v);
                    }
                    try (var rs = ps.executeQuery()) {
                        long n = 0;
                        while (rs.next()) n++;
                        return n;
                    }
                }
            });
            assertTrue(rows >= 0, q.name() + " 执行失败");
        }
    }

    /** SQL 注入防护：把 ' OR 1=1 -- 作为参数查不到任何额外数据。 */
    @Test
    void preparedStatementBlocksInjection() {
        dao.insertAlgorithm("avl", "tree", 4, "AVL");
        var legit = dao.findAlgorithmByName("avl");
        assertTrue(legit.isPresent());
        var injected = dao.findAlgorithmByName("avl' OR '1'='1");
        assertTrue(injected.isEmpty()); // 参数化查询当字面量处理
    }

    @Test
    void connectionPoolReusesConnections() {
        // 连续借还超过池容量次数，全部成功（验证归还逻辑）
        for (int i = 0; i < 20; i++) {
            long n = db.inTransaction(c -> {
                try (var rs = c.createStatement().executeQuery("SELECT 1")) {
                    rs.next();
                    return rs.getLong(1);
                }
            });
            assertEquals(1, n);
        }
    }
}
