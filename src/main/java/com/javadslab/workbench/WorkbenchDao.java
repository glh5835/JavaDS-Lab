package com.javadslab.workbench;

import com.javadslab.persist.Db;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 工作台数据访问层：在既有 lab.db（错题/运行/步骤/算法字典）之上做"兼容迁移"，
 * 新增学习闭环所需表（计划 §19~§21）。绝不删除旧表旧数据。
 *
 * 新表：
 *   learning_state   — 用户主动设置的知识点状态（禁止自动推断，§5.4）
 *   practice_attempt — 练习作答记录（含是否揭示答案与自评）
 *   practice_draft   — 未完成作答草稿（刷新可续，§6.6）
 *   experiment_run   — 实验场运行历史（含 trace JSON，与结果同事务写入，§21）
 *   mistake_review   — 错题每次重做记录（§15.3）
 * 兼容 ALTER：mistakes 补 last_redo_at / success_count 列。
 */
public final class WorkbenchDao {

    private final Db db;

    public WorkbenchDao(Db db) {
        this.db = db;
    }

    /* ================= 迁移 ================= */

    public void migrate() {
        db.inTransaction(c -> {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS learning_state (
                            feature_id TEXT PRIMARY KEY,
                            status     TEXT NOT NULL CHECK (status IN ('unlearned','reviewing','mastered')),
                            updated_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))
                        )""");
                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS practice_attempt (
                            id              INTEGER PRIMARY KEY AUTOINCREMENT,
                            practice_id     TEXT NOT NULL,
                            feature_id      TEXT NOT NULL,
                            answer          TEXT NOT NULL,
                            revealed_answer INTEGER NOT NULL CHECK (revealed_answer IN (0,1)),
                            self_rating     TEXT CHECK (self_rating IN ('unknown','fuzzy','mastered')),
                            created_at      TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                            updated_at      TEXT NOT NULL DEFAULT (datetime('now','localtime'))
                        )""");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_practice_attempt_feature ON practice_attempt(feature_id, practice_id)");
                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS practice_draft (
                            practice_id TEXT PRIMARY KEY,
                            answer      TEXT NOT NULL,
                            revealed    INTEGER NOT NULL DEFAULT 0,
                            updated_at  TEXT NOT NULL DEFAULT (datetime('now','localtime'))
                        )""");
                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS experiment_run (
                            run_id          TEXT PRIMARY KEY,
                            feature_id      TEXT NOT NULL,
                            input_json      TEXT NOT NULL,
                            result_json     TEXT NOT NULL,
                            trace_available INTEGER NOT NULL CHECK (trace_available IN (0,1)),
                            trace_json      TEXT,
                            elapsed_ns      INTEGER NOT NULL CHECK (elapsed_ns >= 0),
                            source          TEXT NOT NULL DEFAULT 'user'
                                            CHECK (source IN ('user','sample','replay','experiment')),
                            created_at      TEXT NOT NULL DEFAULT (datetime('now','localtime'))
                        )""");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_experiment_run_time ON experiment_run(created_at)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_experiment_run_feature ON experiment_run(feature_id, created_at)");
                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS mistake_review (
                            id         INTEGER PRIMARY KEY AUTOINCREMENT,
                            mistake_id INTEGER NOT NULL,
                            answer     TEXT NOT NULL,
                            result     TEXT NOT NULL CHECK (result IN ('fail','partial','success')),
                            created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                            FOREIGN KEY (mistake_id) REFERENCES mistakes(id) ON DELETE CASCADE
                        )""");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_mistake_review ON mistake_review(mistake_id, created_at)");
                // 兼容迁移：mistakes 补列（重复执行安全）
                addColumnIfAbsent(c, "mistakes", "last_redo_at", "TEXT");
                addColumnIfAbsent(c, "mistakes", "success_count", "INTEGER NOT NULL DEFAULT 0");
                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS task_run (
                            id         INTEGER PRIMARY KEY AUTOINCREMENT,
                            type       TEXT NOT NULL,
                            status     TEXT NOT NULL CHECK (status IN ('queued','running','success','failed','cancelled')),
                            started_at TEXT,
                            ended_at   TEXT,
                            summary    TEXT,
                            logs       TEXT
                        )""");
                // 继续学习上下文等应用级 KV（计划 §5.2）
                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS app_kv (
                            key        TEXT PRIMARY KEY,
                            value      TEXT NOT NULL,
                            updated_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))
                        )""");
            }
            return null;
        });
    }

    private void addColumnIfAbsent(Connection c, String table, String column, String ddl) throws Exception {
        try (ResultSet rs = c.createStatement().executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) return;
            }
        }
        try (Statement st = c.createStatement()) {
            st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddl);
        }
    }

    /**
     * 把功能目录里 algorithms 表还缺少的功能补进字典（INSERT-IF-ABSENT，幂等）。
     * 老数据（如 "quick-sort"）保持不动；新功能以 trace-id 命名入库。
     */
    public void seedMissingFeatures() {
        db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id FROM algorithms WHERE name = ?")) {
                for (FeatureCatalog.FeatureDef f : FeatureCatalog.all()) {
                    ps.setString(1, f.algoName());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) continue;
                    }
                    try (PreparedStatement ins = c.prepareStatement(
                            "INSERT INTO algorithms(name, category, difficulty, description) VALUES (?,?,?,?)")) {
                        ins.setString(1, f.algoName());
                        ins.setString(2, categoryCode(f.category()));
                        ins.setInt(3, 3);
                        ins.setString(4, f.name() + "（工作台功能目录补充）");
                        ins.executeUpdate();
                    }
                }
            }
            return null;
        });
    }

    private String categoryCode(String cn) {
        return switch (cn) {
            case "线性表" -> "linear";
            case "栈与队列" -> "stack_queue";
            case "哈希" -> "hash";
            case "树" -> "tree";
            case "图" -> "graph";
            case "排序" -> "sort";
            case "查找" -> "search";
            case "字符串" -> "string";
            case "动态规划" -> "dp";
            default -> "linear";
        };
    }

    /* ================= 学习状态（用户主动设置） ================= */

    public List<Map<String, Object>> allLearningStates() {
        List<Map<String, Object>> out = new ArrayList<>();
        Connection c = db.borrow();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT feature_id, status, updated_at FROM learning_state")) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("featureId", rs.getString(1));
                m.put("status", rs.getString(2));
                m.put("updatedAt", rs.getString(3));
                out.add(m);
            }
        } catch (Exception e) {
            throw new Db.DataAccessException("读取学习状态失败", e);
        } finally {
            db.release(c);
        }
        return out;
    }

    public boolean setLearningState(String featureId, String status) {
        if (!List.of("unlearned", "reviewing", "mastered").contains(status)) return false;
        db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO learning_state(feature_id, status) VALUES (?,?)
                    ON CONFLICT(feature_id) DO UPDATE SET status = excluded.status,
                        updated_at = datetime('now','localtime')""")) {
                ps.setString(1, featureId);
                ps.setString(2, status);
                ps.executeUpdate();
            }
            return null;
        });
        return true;
    }

    /* ================= 练习 ================= */

    public void saveAttempt(String practiceId, String featureId, String answer, boolean revealed, String rating) {
        db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO practice_attempt(practice_id, feature_id, answer, revealed_answer, self_rating)
                    VALUES (?,?,?,?,?)
                    """)) {
                ps.setString(1, practiceId);
                ps.setString(2, featureId);
                ps.setString(3, answer);
                ps.setInt(4, revealed ? 1 : 0);
                ps.setString(5, rating);
                ps.executeUpdate();
            }
            // 自评后清掉草稿（该题已完成一轮）
            try (PreparedStatement del = c.prepareStatement("DELETE FROM practice_draft WHERE practice_id = ?")) {
                del.setString(1, practiceId);
                del.executeUpdate();
            }
            return null;
        });
    }

    public void saveDraft(String practiceId, String answer, boolean revealed) {
        db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO practice_draft(practice_id, answer, revealed)
                    VALUES (?,?,?)
                    ON CONFLICT(practice_id) DO UPDATE SET answer = excluded.answer,
                        revealed = excluded.revealed,
                        updated_at = datetime('now','localtime')""")) {
                ps.setString(1, practiceId);
                ps.setString(2, answer);
                ps.setInt(3, revealed ? 1 : 0);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public List<Map<String, Object>> allDrafts() {
        List<Map<String, Object>> out = new ArrayList<>();
        Connection c = db.borrow();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT practice_id, answer, revealed, updated_at FROM practice_draft")) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("practiceId", rs.getString(1));
                m.put("answer", rs.getString(2));
                m.put("revealed", rs.getInt(3) == 1);
                m.put("updatedAt", rs.getString(4));
                out.add(m);
            }
        } catch (Exception e) {
            throw new Db.DataAccessException("读取草稿失败", e);
        } finally {
            db.release(c);
        }
        return out;
    }

    public List<Map<String, Object>> attemptsByFeature(String featureId, int limit) {
        List<Map<String, Object>> out = new ArrayList<>();
        Connection c = db.borrow();
        String sql = "SELECT id, practice_id, feature_id, answer, revealed_answer, self_rating, created_at"
                + (featureId != null ? " FROM practice_attempt WHERE feature_id = ?" : " FROM practice_attempt")
                + " ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            if (featureId != null) ps.setString(i++, featureId);
            ps.setInt(i, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong(1));
                    m.put("practiceId", rs.getString(2));
                    m.put("featureId", rs.getString(3));
                    m.put("answer", rs.getString(4));
                    m.put("revealed", rs.getInt(5) == 1);
                    m.put("selfRating", rs.getString(6));
                    m.put("createdAt", rs.getString(7));
                    out.add(m);
                }
            }
        } catch (Exception e) {
            throw new Db.DataAccessException("读取练习记录失败", e);
        } finally {
            db.release(c);
        }
        return out;
    }

    /* ================= 实验运行历史（结果与 trace 同事务，§21） ================= */

    public String saveRun(String featureId, String inputJson, String resultJson,
                          boolean traceAvailable, String traceJson, long elapsedNs, String source) {
        return db.inTransaction(c -> {
            String runId = "r" + Long.toHexString(System.currentTimeMillis()) + "-" + (int) (Math.random() * 1e6);
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO experiment_run(run_id, feature_id, input_json, result_json,
                        trace_available, trace_json, elapsed_ns, source)
                    VALUES (?,?,?,?,?,?,?,?)""")) {
                ps.setString(1, runId);
                ps.setString(2, featureId);
                ps.setString(3, inputJson);
                ps.setString(4, resultJson);
                ps.setInt(5, traceAvailable ? 1 : 0);
                ps.setString(6, traceJson); // 同一事务写入；写失败整条回滚
                ps.setLong(7, elapsedNs);
                ps.setString(8, source);
                ps.executeUpdate();
            }
            return runId;
        });
    }

    public List<Map<String, Object>> recentRuns(String featureId, int limit) {
        List<Map<String, Object>> out = new ArrayList<>();
        Connection c = db.borrow();
        String sql = "SELECT run_id, feature_id, input_json, result_json, trace_available, elapsed_ns, source, created_at"
                + (featureId != null ? " FROM experiment_run WHERE feature_id = ?" : " FROM experiment_run")
                + " ORDER BY created_at DESC, rowid DESC LIMIT ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            if (featureId != null) ps.setString(i++, featureId);
            ps.setInt(i, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("runId", rs.getString(1));
                    m.put("featureId", rs.getString(2));
                    m.put("inputJson", rs.getString(3));
                    m.put("resultJson", rs.getString(4));
                    m.put("traceAvailable", rs.getInt(5) == 1);
                    m.put("elapsedNs", rs.getLong(6));
                    m.put("source", rs.getString(7));
                    m.put("createdAt", rs.getString(8));
                    out.add(m);
                }
            }
        } catch (Exception e) {
            throw new Db.DataAccessException("读取运行历史失败", e);
        } finally {
            db.release(c);
        }
        return out;
    }

    public Optional<Map<String, Object>> runById(String runId, boolean withTrace) {
        Connection c = db.borrow();
        String sql = "SELECT run_id, feature_id, input_json, result_json, trace_available, "
                + (withTrace ? "trace_json, " : "NULL, ")
                + "elapsed_ns, source, created_at FROM experiment_run WHERE run_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("runId", rs.getString(1));
                m.put("featureId", rs.getString(2));
                m.put("inputJson", rs.getString(3));
                m.put("resultJson", rs.getString(4));
                m.put("traceAvailable", rs.getInt(5) == 1);
                m.put("traceJson", rs.getString(6));
                m.put("elapsedNs", rs.getLong(7));
                m.put("source", rs.getString(8));
                m.put("createdAt", rs.getString(9));
                return Optional.of(m);
            }
        } catch (Exception e) {
            throw new Db.DataAccessException("读取运行记录失败", e);
        } finally {
            db.release(c);
        }
    }

    /* ================= 错题（共用 mistakes 表 + 新 mistake_review） ================= */

    public long addMistake(String algoName, String question, String wrongAnswer, String reason) {
        return db.inTransaction(c -> {
            long algoId = findOrInsertAlgorithm(c, algoName);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO mistakes(algorithm_id, question, wrong_answer, reason) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, algoId);
                ps.setString(2, question);
                ps.setString(3, wrongAnswer);
                ps.setString(4, reason);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    return rs.next() ? rs.getLong(1) : -1;
                }
            }
        });
    }

    public boolean updateMistake(long id, String question, String wrongAnswer, String reason) {
        return db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE mistakes SET question = ?, wrong_answer = ?, reason = ? WHERE id = ?")) {
                ps.setString(1, question);
                ps.setString(2, wrongAnswer);
                ps.setString(3, reason);
                ps.setLong(4, id);
                return ps.executeUpdate() > 0;
            }
        });
    }

    public boolean deleteMistake(long id) {
        return db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM mistakes WHERE id = ?")) {
                ps.setLong(1, id);
                return ps.executeUpdate() > 0;
            }
        });
    }

    /** 重做记录：result ∈ fail/partial/success；success 才累计 success_count（§15.3）。 */
    public boolean addMistakeReview(long mistakeId, String answer, String result) {
        if (!List.of("fail", "partial", "success").contains(result)) return false;
        return db.inTransaction(c -> {
            try (PreparedStatement chk = c.prepareStatement("SELECT id FROM mistakes WHERE id = ?")) {
                chk.setLong(1, mistakeId);
                try (ResultSet rs = chk.executeQuery()) {
                    if (!rs.next()) return false;
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO mistake_review(mistake_id, answer, result) VALUES (?,?,?)")) {
                ps.setLong(1, mistakeId);
                ps.setString(2, answer);
                ps.setString(3, result);
                ps.executeUpdate();
            }
            try (PreparedStatement up = c.prepareStatement(
                    "UPDATE mistakes SET redo_count = redo_count + 1, last_redo_at = datetime('now','localtime'), "
                            + "success_count = success_count + ? WHERE id = ?")) {
                up.setInt(1, "success".equals(result) ? 1 : 0);
                up.setLong(2, mistakeId);
                up.executeUpdate();
            }
            return true;
        });
    }

    public List<Map<String, Object>> listMistakes(String algoName, String q, int limit) {
        List<Map<String, Object>> out = new ArrayList<>();
        Connection c = db.borrow();
        StringBuilder sql = new StringBuilder("""
                SELECT m.id, m.question, m.wrong_answer, m.reason, m.redo_count, m.success_count,
                       COALESCE(m.last_redo_at, ''), m.created_at, a.name, m.algorithm_id
                FROM mistakes m JOIN algorithms a ON a.id = m.algorithm_id WHERE 1=1""");
        List<Object> params = new ArrayList<>();
        if (algoName != null && !algoName.isBlank()) {
            sql.append(" AND a.name = ?");
            params.add(algoName);
        }
        if (q != null && !q.isBlank()) {
            sql.append(" AND (m.question LIKE ? OR m.reason LIKE ? OR m.wrong_answer LIKE ?)");
            String like = "%" + q + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY m.id DESC LIMIT ?");
        params.add(limit);
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong(1));
                    m.put("question", rs.getString(2));
                    m.put("wrongAnswer", rs.getString(3));
                    m.put("reason", rs.getString(4));
                    m.put("redoCount", rs.getInt(5));
                    m.put("successCount", rs.getInt(6));
                    m.put("lastRedoAt", rs.getString(7));
                    m.put("createdAt", rs.getString(8));
                    m.put("algoName", rs.getString(9));
                    m.put("algorithmId", rs.getLong(10));
                    out.add(m);
                }
            }
        } catch (Exception e) {
            throw new Db.DataAccessException("读取错题失败", e);
        } finally {
            db.release(c);
        }
        return out;
    }

    public Optional<Map<String, Object>> mistakeById(long id) {
        Connection c = db.borrow();
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT m.id, m.question, m.wrong_answer, m.reason, m.redo_count, m.success_count,
                       COALESCE(m.last_redo_at,''), m.created_at, a.name
                FROM mistakes m JOIN algorithms a ON a.id = m.algorithm_id WHERE m.id = ?""")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getLong(1));
                m.put("question", rs.getString(2));
                m.put("wrongAnswer", rs.getString(3));
                m.put("reason", rs.getString(4));
                m.put("redoCount", rs.getInt(5));
                m.put("successCount", rs.getInt(6));
                m.put("lastRedoAt", rs.getString(7));
                m.put("createdAt", rs.getString(8));
                m.put("algoName", rs.getString(9));
                return Optional.of(m);
            }
        } catch (Exception e) {
            throw new Db.DataAccessException("读取错题失败", e);
        } finally {
            db.release(c);
        }
    }

    public List<Map<String, Object>> reviewsOfMistake(long mistakeId) {
        List<Map<String, Object>> out = new ArrayList<>();
        Connection c = db.borrow();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, answer, result, created_at FROM mistake_review WHERE mistake_id = ? ORDER BY id DESC")) {
            ps.setLong(1, mistakeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong(1));
                    m.put("answer", rs.getString(2));
                    m.put("result", rs.getString(3));
                    m.put("createdAt", rs.getString(4));
                    out.add(m);
                }
            }
        } catch (Exception e) {
            throw new Db.DataAccessException("读取重做记录失败", e);
        } finally {
            db.release(c);
        }
        return out;
    }

    /** 薄弱项：只描述记录（错题数/重做/成功），不推断掌握状态（§16）。 */
    public List<Map<String, Object>> weakness() {
        List<Map<String, Object>> out = new ArrayList<>();
        Connection c = db.borrow();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("""
                     SELECT a.name, COUNT(m.id) AS cnt, SUM(m.redo_count),
                            SUM(COALESCE(m.success_count,0)),
                            MAX(COALESCE(m.last_redo_at, m.created_at))
                     FROM mistakes m JOIN algorithms a ON a.id = m.algorithm_id
                     GROUP BY a.id ORDER BY cnt DESC, SUM(m.redo_count) DESC""")) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("algoName", rs.getString(1));
                m.put("mistakeCount", rs.getLong(2));
                m.put("redoCount", rs.getLong(3));
                m.put("successCount", rs.getLong(4));
                m.put("lastActivity", rs.getString(5));
                out.add(m);
            }
        } catch (Exception e) {
            throw new Db.DataAccessException("统计薄弱项失败", e);
        } finally {
            db.release(c);
        }
        return out;
    }

    /* ================= 继续学习上下文（KV） ================= */

    public void saveKv(String key, String value) {
        db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO app_kv(key, value) VALUES (?,?)
                    ON CONFLICT(key) DO UPDATE SET value = excluded.value,
                        updated_at = datetime('now','localtime')""")) {
                ps.setString(1, key);
                ps.setString(2, value);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public String getKv(String key) {
        Connection c = db.borrow();
        try (PreparedStatement ps = c.prepareStatement("SELECT value FROM app_kv WHERE key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (Exception e) {
            throw new Db.DataAccessException("读取 KV 失败", e);
        } finally {
            db.release(c);
        }
    }

    /* ================= 工具 ================= */

    private long findOrInsertAlgorithm(Connection c, String name) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM algorithms WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        try (PreparedStatement ins = c.prepareStatement(
                "INSERT INTO algorithms(name, category, difficulty, description) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ins.setString(1, name);
            ins.setString(2, "linear");
            ins.setInt(3, 3);
            ins.setString(4, "自动补充");
            ins.executeUpdate();
            try (ResultSet rs = ins.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        }
    }

    public long countMistakes() {
        Connection c = db.borrow();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM mistakes")) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            throw new Db.DataAccessException("统计错题失败", e);
        } finally {
            db.release(c);
        }
    }
}
