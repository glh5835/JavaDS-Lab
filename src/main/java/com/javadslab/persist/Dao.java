package com.javadslab.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 四张表的 DAO：全部使用 PreparedStatement（防 SQL 注入），不拼接 SQL 字符串。
 * 批量写走事务；查询返回不可变记录。
 */
public final class Dao {

    private final Db db;

    public Dao(Db db) { this.db = db; }

    /* ================= 记录类型 ================= */

    public record Algorithm(long id, String name, String category, int difficulty, String description) {}

    public record Run(long id, long algorithmId, String ranAt, int durationMs, int inputSize, boolean passed, Double score) {}

    public record Step(long id, long runId, int stepNo, String op, String detail) {}

    public record Mistake(long id, long algorithmId, String question, String wrongAnswer, String reason, int redoCount, String createdAt) {}

    /** 错题重做成功：错误原因归档后重新计数前先清空错误记录时使用。 */
    public record Weakness(String algorithmName, String category, long mistakeCount, int totalRedo, double avgRedo) {}

    /* ================= algorithms ================= */

    public long insertAlgorithm(String name, String category, int difficulty, String description) {
        return db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO algorithms(name, category, difficulty, description) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, category);
                ps.setInt(3, difficulty);
                ps.setString(4, description);
                ps.executeUpdate();
                return generatedKey(ps);
            }
        });
    }

    public Optional<Algorithm> findAlgorithmByName(String name) {
        return db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT id, name, category, difficulty, description FROM algorithms WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(readAlgorithm(rs)) : Optional.<Algorithm>empty();
                }
            }
        });
    }

    public List<Algorithm> listAlgorithms() {
        return db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT id, name, category, difficulty, description FROM algorithms ORDER BY category, name");
                 ResultSet rs = ps.executeQuery()) {
                List<Algorithm> out = new ArrayList<>();
                while (rs.next()) out.add(readAlgorithm(rs));
                return out;
            }
        });
    }

    private static Algorithm readAlgorithm(ResultSet rs) throws SQLException {
        return new Algorithm(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getString(5));
    }

    /** 初始化算法字典（已存在则跳过），返回新插入的数量。 */
    public int seedAlgorithmsIfAbsent(List<String[]> rows) {
        return db.inTransaction(c -> {
            int inserted = 0;
            try (PreparedStatement check = c.prepareStatement("SELECT id FROM algorithms WHERE name = ?");
                 PreparedStatement insert = c.prepareStatement("INSERT INTO algorithms(name, category, difficulty, description) VALUES (?,?,?,?)")) {
                for (String[] r : rows) {
                    check.setString(1, r[0]);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) continue;
                    }
                    insert.setString(1, r[0]);
                    insert.setString(2, r[1]);
                    insert.setInt(3, Integer.parseInt(r[2]));
                    insert.setString(4, r[3]);
                    inserted += insert.executeUpdate();
                }
            }
            return inserted;
        });
    }

    /* ================= runs ================= */

    public long insertRun(long algorithmId, int durationMs, int inputSize, boolean passed, Double score) {
        return db.inTransaction(c -> insertRun(c, algorithmId, durationMs, inputSize, passed, score));
    }

    static long insertRun(Connection c, long algorithmId, int durationMs, int inputSize, boolean passed, Double score) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO runs(algorithm_id, duration_ms, input_size, passed, score) VALUES (?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, algorithmId);
            ps.setInt(2, durationMs);
            ps.setInt(3, inputSize);
            ps.setInt(4, passed ? 1 : 0);
            if (score == null) ps.setNull(5, java.sql.Types.REAL);
            else ps.setDouble(5, score);
            ps.executeUpdate();
            return generatedKey(ps);
        }
    }

    /** 批量插入运行记录（单事务，10 万条约 1-2 秒量级）。 */
    public void insertRunsBatch(long algorithmId, List<int[]> rows) {
        // rows: {durationMs, inputSize, passed0or1, scoreScaledBy100}
        db.inTransaction((Db.SqlWork<Void>) c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO runs(algorithm_id, duration_ms, input_size, passed, score) VALUES (?,?,?,?,?)")) {
                for (int[] r : rows) {
                    ps.setLong(1, algorithmId);
                    ps.setInt(2, r[0]);
                    ps.setInt(3, r[1]);
                    ps.setInt(4, r[2]);
                    ps.setDouble(5, r[3] / 100.0);
                    ps.addBatch();
                }
                ps.executeBatch();
                return null;
            }
        });
    }

    public long countRuns() {
        return db.inTransaction(c -> {
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM runs")) {
                rs.next();
                return rs.getLong(1);
            }
        });
    }

    /* ================= steps ================= */

    public long insertStep(long runId, int stepNo, String op, String detail) {
        return db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO steps(run_id, step_no, op, detail) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, runId);
                ps.setInt(2, stepNo);
                ps.setString(3, op);
                ps.setString(4, detail);
                ps.executeUpdate();
                return generatedKey(ps);
            }
        });
    }

    /** 一个 run 的全部步骤按序返回（覆盖索引 idx_steps_run）。 */
    public List<Step> stepsOfRun(long runId) {
        return db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, run_id, step_no, op, detail FROM steps WHERE run_id = ? ORDER BY step_no")) {
                ps.setLong(1, runId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Step> out = new ArrayList<>();
                    while (rs.next()) out.add(new Step(rs.getLong(1), rs.getLong(2), rs.getInt(3), rs.getString(4), rs.getString(5)));
                    return out;
                }
            }
        });
    }

    /* ================= mistakes（错题本） ================= */

    public long addMistake(long algorithmId, String question, String wrongAnswer, String reason) {
        return db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO mistakes(algorithm_id, question, wrong_answer, reason) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, algorithmId);
                ps.setString(2, question);
                ps.setString(3, wrongAnswer);
                ps.setString(4, reason);
                ps.executeUpdate();
                return generatedKey(ps);
            }
        });
    }

    /** 重做一次：成功则 redo_count+1（约定：仍错不计数，直接返回 false）。 */
    public boolean markRedo(long mistakeId) {
        return db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE mistakes SET redo_count = redo_count + 1 WHERE id = ?")) {
                ps.setLong(1, mistakeId);
                return ps.executeUpdate() == 1;
            }
        });
    }

    public int deleteMistake(long mistakeId) {
        return db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM mistakes WHERE id = ?")) {
                ps.setLong(1, mistakeId);
                return ps.executeUpdate();
            }
        });
    }

    public List<Mistake> listMistakesByAlgorithm(long algorithmId) {
        return db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, algorithm_id, question, wrong_answer, reason, redo_count, created_at FROM mistakes WHERE algorithm_id = ? ORDER BY created_at DESC")) {
                ps.setLong(1, algorithmId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Mistake> out = new ArrayList<>();
                    while (rs.next()) out.add(readMistake(rs));
                    return out;
                }
            }
        });
    }

    /** 按知识点统计薄弱项：错题数 + 平均重做次数，倒序（错得多在前）。 */
    public List<Weakness> weaknessReport() {
        return db.inTransaction(c -> {
            String sql = """
                    SELECT a.name, a.category, COUNT(*) AS cnt, COALESCE(SUM(m.redo_count),0) AS redo_sum,
                           ROUND(AVG(m.redo_count), 2) AS redo_avg
                    FROM mistakes m JOIN algorithms a ON a.id = m.algorithm_id
                    GROUP BY a.id ORDER BY cnt DESC, redo_avg DESC
                    """;
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                List<Weakness> out = new ArrayList<>();
                while (rs.next()) out.add(new Weakness(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getInt(4), rs.getDouble(5)));
                return out;
            }
        });
    }

    private static Mistake readMistake(ResultSet rs) throws SQLException {
        return new Mistake(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getInt(6), rs.getString(7));
    }

    /* ================= 工具 ================= */

    static long generatedKey(Statement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (!keys.next()) throw new SQLException("未取到生成主键");
            return keys.getLong(1);
        }
    }
}
