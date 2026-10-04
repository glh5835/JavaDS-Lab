package com.javadslab.workbench;

import com.javadslab.persist.Dao;
import com.javadslab.persist.Db;
import com.javadslab.persist.Queries;
import com.javadslab.persist.SeedData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 数据库实验服务（计划 §22/§23）。
 * - 学习库 data/lab.db：只跑 Queries.ALL 的 10 条固定 SELECT（PreparedStatement），只读；
 * - 实验库 data/experiment.db：独立文件，允许造数/建删索引/EXPLAIN QUERY PLAN/任意单条 SQL，
 *   但禁止 ATTACH/DETACH/PRAGMA 等越界语句，绝不触碰 lab.db。
 */
public final class DbLabService {

    private final Db labDb;
    private final String labDbPath;   // data/lab.db
    private final String experimentDbPath; // data/experiment.db
    private final Path root;

    public DbLabService(Db labDb, Path root) {
        this.labDb = labDb;
        this.root = root;
        this.labDbPath = root.resolve("data/lab.db").toString();
        this.experimentDbPath = root.resolve("data/experiment.db").toString();
    }

    /* ================= 10 条查询（学习库，只读） ================= */

    public List<Map<String, Object>> queryCatalog() {
        List<Map<String, Object>> out = new ArrayList<>();
        int i = 0;
        for (Queries.Query q : Queries.ALL) {
            i++;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", "Q" + i);
            m.put("name", q.name());
            m.put("sql", q.sql().trim());
            m.put("hasParam", q.hasParam());
            m.put("target", "lab.db（只读）");
            out.add(m);
        }
        return out;
    }

    public Map<String, Object> runCatalogQuery(String queryId, String param) {
        int idx = -1;
        try {
            idx = Integer.parseInt(queryId.replaceFirst("(?i)q", ""));
        } catch (NumberFormatException ignored) {
        }
        if (idx < 1 || idx > Queries.ALL.size()) {
            throw new IllegalArgumentException("未知查询编号：" + queryId);
        }
        Queries.Query q = Queries.ALL.get(idx - 1);
        Connection c = labDb.borrow();
        long t0 = System.nanoTime();
        try (PreparedStatement ps = c.prepareStatement(q.sql())) {
            if (q.hasParam()) {
                long p = 0;
                if (param != null && !param.isBlank()) {
                    try {
                        p = Long.parseLong(param.trim());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("查询参数必须是非负整数，得到：" + param);
                    }
                }
                ps.setLong(1, p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                QueryResult qr = collect(rs, 500);
                long ns = System.nanoTime() - t0;
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("queryId", "Q" + idx);
                out.put("name", q.name());
                out.put("sql", q.sql().trim());
                out.put("param", q.hasParam() ? (param == null ? "" : param) : null);
                out.put("columns", qr.columns);
                out.put("rows", qr.rows);
                out.put("rowCount", qr.rows.size());
                out.put("truncated", qr.truncated);
                out.put("elapsedMs", ns / 1_000_000.0);
                out.put("plan", explainLab(q, q.hasParam() ? param : null));
                return out;
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("查询执行失败：" + e.getMessage(), e);
        } finally {
            labDb.release(c);
        }
    }

    private String explainLab(Queries.Query q, String param) {
        Connection c = labDb.borrow();
        try (PreparedStatement ps = c.prepareStatement("EXPLAIN QUERY PLAN " + q.sql())) {
            if (q.hasParam()) {
                long p = 0;
                if (param != null && !param.isBlank()) p = Long.parseLong(param.trim());
                ps.setLong(1, p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(rs.getString("detail"));
                }
                return sb.toString();
            }
        } catch (Exception e) {
            return "(无法获取执行计划: " + e.getMessage() + ")";
        } finally {
            labDb.release(c);
        }
    }

    /* ================= 实验库（独立文件，可写） ================= */

    public Map<String, Object> experimentStatus() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dbPath", "data/experiment.db");
        m.put("exists", Files.exists(Path.of(experimentDbPath)));
        try (Db db = new Db(experimentDbPath, 1)) {
            Connection c = db.borrow();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='runs'")) {
                boolean hasRuns = rs.next() && rs.getInt(1) > 0;
                m.put("seeded", hasRuns);
                if (hasRuns) {
                    try (ResultSet r2 = c.createStatement().executeQuery("SELECT COUNT(*) FROM runs")) {
                        m.put("runs", r2.next() ? r2.getLong(1) : 0);
                    }
                    try (ResultSet r3 = c.createStatement().executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name LIKE 'idx_%'")) {
                        m.put("indexes", r3.next() ? r3.getLong(1) : 0);
                    }
                }
            } finally {
                db.release(c);
            }
        } catch (Exception e) {
            m.put("error", e.getMessage());
        }
        return m;
    }

    /** 实验库操作：seed（造数）/ drop-index / create-index / explain / sql（受限自由查询，仅实验库）。 */
    public Map<String, Object> experiment(String op, Map<String, Object> params) {
        return switch (op == null ? "" : op) {
            case "seed" -> expSeed(intOf(params, "rows", 100_000, 10_000_000));
            case "create-index" -> expSql("CREATE INDEX IF NOT EXISTS " + indexName(params) +
                    " ON runs(algorithm_id, ran_at)", "创建索引 " + indexName(params));
            case "drop-index" -> expSql("DROP INDEX IF EXISTS " + indexName(params), "删除索引 " + indexName(params));
            case "explain" -> expExplain(String.valueOf(params.get("sql")));
            case "sql" -> expFreeSql(String.valueOf(params.get("sql")));
            case "reset" -> expReset();
            default -> throw new IllegalArgumentException("未知实验操作：" + op);
        };
    }

    private Map<String, Object> expSeed(int rows) {
        try (Db db = new Db(experimentDbPath, 1)) {
            Dao dao = new Dao(db);
            SeedData.applySchema(db, root.resolve("sql/schema.sql"));
            long t0 = System.nanoTime();
            SeedData.SeedStats stats = SeedData.seed(db, dao, rows, 20261005L);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("summary", "实验库造数完成：runs=" + stats.runs() + " steps=" + stats.steps()
                    + " mistakes=" + stats.mistakes() + "，耗时 " + ms + " ms");
            out.put("elapsedMs", ms);
            out.put("status", experimentStatus());
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("造数失败：" + e.getMessage(), e);
        }
    }

    private Map<String, Object> expReset() {
        try {
            Files.deleteIfExists(Path.of(experimentDbPath));
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("summary", "实验库已重置（文件已删除，下次造数时重建）");
            out.put("status", experimentStatus());
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("重置失败：" + e.getMessage(), e);
        }
    }

    private String indexName(Map<String, Object> params) {
        String name = String.valueOf(params.getOrDefault("name", "idx_runs_algo_time"));
        if (!name.matches("[a-z][a-z0-9_]{0,40}")) {
            throw new IllegalArgumentException("索引名只允许小写字母/数字/下划线：" + name);
        }
        return name;
    }

    /** 自由 SQL 只允许打实验库，且单条语句、禁越界关键词。 */
    private Map<String, Object> expFreeSql(String sql) {
        String s = guardSql(sql);
        try (Db db = new Db(experimentDbPath, 1)) {
            Connection c = db.borrow();
            long t0 = System.nanoTime();
            try (Statement st = c.createStatement()) {
                boolean hasRs = st.execute(s);
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("sql", s);
                if (hasRs) {
                    QueryResult qr = collect(st.getResultSet(), 200);
                    out.put("columns", qr.columns);
                    out.put("rows", qr.rows);
                    out.put("rowCount", qr.rows.size());
                    out.put("truncated", qr.truncated);
                } else {
                    out.put("updated", st.getUpdateCount());
                }
                out.put("elapsedMs", (System.nanoTime() - t0) / 1_000_000.0);
                return out;
            } finally {
                db.release(c);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("SQL 执行失败：" + e.getMessage(), e);
        }
    }

    private Map<String, Object> expExplain(String sql) {
        String s = guardSql(sql, true);
        try (Db db = new Db(experimentDbPath, 1)) {
            Connection c = db.borrow();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("EXPLAIN QUERY PLAN " + s)) {
                QueryResult qr = collect(rs, 50);
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("sql", s);
                out.put("columns", qr.columns);
                out.put("rows", qr.rows);
                return out;
            } finally {
                db.release(c);
            }
        } catch (Exception e) {
            throw new IllegalStateException("EXPLAIN 失败：" + e.getMessage(), e);
        }
    }

    private Map<String, Object> expSql(String sql, String summary) {
        try (Db db = new Db(experimentDbPath, 1)) {
            Connection c = db.borrow();
            try (Statement st = c.createStatement()) {
                st.executeUpdate(sql);
            } finally {
                db.release(c);
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("summary", summary + " 完成");
            out.put("status", experimentStatus());
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("操作失败：" + e.getMessage(), e);
        }
    }

    private String guardSql(String sql) {
        return guardSql(sql, false);
    }

    private String guardSql(String sql, boolean explain) {
        if (sql == null || sql.isBlank()) throw new IllegalArgumentException("SQL 不能为空");
        String s = sql.trim().replaceAll(";\\s*$", "");
        if (s.contains(";")) throw new IllegalArgumentException("只允许单条 SQL 语句");
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.contains("attach") || lower.contains("detach") || lower.contains("pragma")
                || lower.contains("lab.db") || lower.contains("..")) {
            throw new IllegalArgumentException("实验库 SQL 不允许 ATTACH/DETACH/PRAGMA 或引用学习库文件");
        }
        return s;
    }

    /* ================= 通用结果收集 ================= */

    private record QueryResult(List<String> columns, List<List<Object>> rows, boolean truncated) {}

    private QueryResult collect(ResultSet rs, int maxRows) throws Exception {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= cols; i++) columns.add(md.getColumnLabel(i));
        List<List<Object>> rows = new ArrayList<>();
        boolean truncated = false;
        while (rs.next()) {
            if (rows.size() >= maxRows) {
                truncated = true;
                break;
            }
            List<Object> row = new ArrayList<>(cols);
            for (int i = 1; i <= cols; i++) row.add(rs.getString(i)); // 统一转字符串便于前端表格展示
            rows.add(row);
        }
        return new QueryResult(columns, rows, truncated);
    }

    private int intOf(Map<String, Object> params, String key, int dft, int max) {
        Object v = params == null ? null : params.get(key);
        if (v == null) return dft;
        long n;
        try {
            n = Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " 必须是整数");
        }
        if (n < 0 || n > max) throw new IllegalArgumentException(key + " 需在 0~" + max + " 之间");
        return (int) n;
    }
}
