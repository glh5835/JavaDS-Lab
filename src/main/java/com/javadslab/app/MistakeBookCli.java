package com.javadslab.app;

import com.javadslab.persist.Dao;
import com.javadslab.persist.Db;
import com.javadslab.persist.SeedData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * 错题本命令行交互界面。
 * 全部数据操作走 Dao（PreparedStatement + 事务），支持脚本化测试：
 * 构造时注入 InputStream/PrintStream，runLoop() 逐行读命令并回写结果。
 *
 * 命令：algo / list [算法名] / add / redo <id> / del <id> / weak / help / quit
 */
public final class MistakeBookCli {

    private final Dao dao;
    private final BufferedReader in;
    private final PrintStream out;

    public MistakeBookCli(Dao dao, InputStream in, PrintStream out) {
        this.dao = dao;
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = new PrintStream(out, true, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        String dbPath = args.length > 0 ? args[0] : "data/lab.db";
        try (Db db = new Db(dbPath, 2)) {
            var schema = java.nio.file.Path.of("sql/schema.sql");
            if (java.nio.file.Files.exists(schema)) {
                SeedData.applySchema(db, schema);
            }
            Dao dao = new Dao(db);
            if (dao.listAlgorithms().isEmpty()) {
                dao.seedAlgorithmsIfAbsent(SeedData.ALGORITHMS); // 首次使用自动初始化算法字典
            }
            System.out.println("=== JavaDS-Lab 错题本（库：" + dbPath + "）===");
            new MistakeBookCli(dao, System.in, System.out).runLoop();
        }
    }

    /* ---------------- 主循环 ---------------- */

    public void runLoop() throws IOException {
        printHelp();
        while (true) {
            out.print("mistake> ");
            out.flush();
            String line = in.readLine();
            if (line == null) break; // EOF（脚本结束/ Ctrl+D）
            String cmd = line.trim();
            if (cmd.isEmpty()) continue;
            try {
                if (dispatch(cmd)) break;
            } catch (Exception e) {
                out.println("!! 出错了：" + e.getMessage());
            }
        }
        out.println("再见。错题本已保存到数据库。");
    }

    /** @return true 表示要退出。 */
    private boolean dispatch(String cmd) throws IOException {
        String lower = cmd.toLowerCase();
        if (lower.equals("quit") || lower.equals("exit") || lower.equals("q")) return true;
        if (lower.equals("help") || lower.equals("h") || lower.equals("?")) {
            printHelp();
            return false;
        }
        if (lower.equals("algo")) {
            cmdAlgo();
            return false;
        }
        if (lower.equals("list")) {
            cmdList(null);
            return false;
        }
        if (lower.startsWith("list ")) {
            String name = cmd.substring(5).trim();
            if (name.isEmpty()) cmdList(null);
            else cmdList(name);
            return false;
        }
        if (lower.equals("add")) {
            cmdAdd();
            return false;
        }
        if (lower.startsWith("redo ")) {
            cmdRedo(arg(cmd));
            return false;
        }
        if (lower.startsWith("del ")) {
            cmdDel(arg(cmd));
            return false;
        }
        if (lower.equals("weak")) {
            cmdWeak();
            return false;
        }
        out.println("未知命令: " + cmd + "（输入 help 查看命令列表）");
        return false;
    }

    private static String arg(String cmd) {
        String a = cmd.substring(cmd.indexOf(' ') + 1).trim();
        return a;
    }

    /* ---------------- 各命令实现 ---------------- */

    private void cmdAlgo() {
        List<Dao.Algorithm> algos = dao.listAlgorithms();
        if (algos.isEmpty()) {
            out.println("(算法字典为空——请先运行基准程序或 insertAlgorithm 初始化)");
            return;
        }
        out.printf("%-4s %-24s %-12s %s%n", "id", "名称", "分类", "难度");
        for (Dao.Algorithm a : algos) {
            out.printf("%-4d %-24s %-12s %d%n", a.id(), a.name(), a.category(), a.difficulty());
        }
        out.println("共 " + algos.size() + " 个算法。");
    }

    private void cmdList(String filterName) {
        List<Dao.MistakeRow> rows = dao.listAllMistakes();
        if (filterName != null) {
            rows = rows.stream().filter(r -> r.algorithm().equalsIgnoreCase(filterName)).toList();
            if (rows.isEmpty()) {
                out.println("算法 “" + filterName + "” 没有错题记录（或算法名不存在，可用 algo 命令查看）。");
                return;
            }
        }
        if (rows.isEmpty()) {
            out.println("(还没有错题记录，用 add 添加第一条)");
            return;
        }
        out.printf("%-4s %-20s %-28s %-14s %-10s %s%n", "id", "算法", "题目", "错误答案", "重做", "原因");
        for (Dao.MistakeRow r : rows) {
            out.printf("%-4d %-20s %-28s %-14s %-10d %s%n",
                    r.id(), r.algorithm(), r.question(), r.wrongAnswer(), r.redoCount(), r.reason());
        }
        out.println("共 " + rows.size() + " 条。");
    }

    private void cmdAdd() throws IOException {
        Optional<Dao.Algorithm> algo = promptAlgorithm();
        if (algo.isEmpty()) return;
        String question = promptNonEmpty("题目（如：手写堆第 3 题）：");
        if (question == null) return;
        String wrong = promptNonEmpty("错误答案/现象（如：WA3 数组越界）：");
        if (wrong == null) return;
        String reason = promptNonEmpty("错误原因（如：下沉时漏了右孩子）：");
        if (reason == null) return;
        long id = dao.addMistake(algo.get().id(), question, wrong, reason);
        out.println("已记录错题 #" + id + "（算法：" + algo.get().name() + "）。用 redo " + id + " 记录重做成功。");
    }

    /** 提示输入算法名（或数字 id）；最多重试 3 次，失败返回 empty。 */
    private Optional<Dao.Algorithm> promptAlgorithm() throws IOException {
        for (int attempt = 0; attempt < 3; attempt++) {
            out.print("算法名或 id（algo 可查列表，直接回车取消）：");
            out.flush();
            String line = in.readLine();
            if (line == null) return Optional.empty();
            String s = line.trim();
            if (s.isEmpty()) {
                out.println("(已取消 add)");
                return Optional.empty();
            }
            Optional<Dao.Algorithm> algo = resolveAlgorithm(s);
            if (algo.isPresent()) return algo;
            out.println("!! 找不到算法 “" + s + "”，请重试（algo 查看列表）。");
        }
        out.println("(连续 3 次无效，已取消 add)");
        return Optional.empty();
    }

    private Optional<Dao.Algorithm> resolveAlgorithm(String s) {
        if (s.matches("\\d+")) {
            long id = Long.parseLong(s);
            for (Dao.Algorithm a : dao.listAlgorithms()) {
                if (a.id() == id) return Optional.of(a);
            }
            return Optional.empty();
        }
        return dao.findAlgorithmByName(s);
    }

    private String promptNonEmpty(String hint) throws IOException {
        for (int attempt = 0; attempt < 3; attempt++) {
            out.print(hint);
            out.flush();
            String line = in.readLine();
            if (line == null) return null;
            String s = line.trim();
            if (!s.isEmpty()) return s;
            out.println("!! 内容不能为空。");
        }
        out.println("(连续 3 次为空，已取消 add)");
        return null;
    }

    private void cmdRedo(String argStr) {
        Long id = parseId(argStr);
        if (id == null) return;
        if (dao.markRedo(id)) {
            out.println("错题 #" + id + " 重做成功 +1。");
        } else {
            out.println("!! 未找到错题 #" + id + "（list 查看现有 id）。");
        }
    }

    private void cmdDel(String argStr) {
        Long id = parseId(argStr);
        if (id == null) return;
        if (dao.deleteMistake(id) == 1) {
            out.println("已删除错题 #" + id + "。");
        } else {
            out.println("!! 未找到错题 #" + id + "。");
        }
    }

    /** 解析正整数 id；非法时输出提示并返回 null。 */
    private Long parseId(String s) {
        if (!s.matches("\\d+")) {
            out.println("!! 用法示例：redo 3 / del 3（id 为正整数，list 可查）。");
            return null;
        }
        return Long.parseLong(s);
    }

    private void cmdWeak() {
        List<Dao.Weakness> rows = dao.weaknessReport();
        if (rows.isEmpty()) {
            out.println("(暂无错题，没有薄弱项统计)");
            return;
        }
        out.println("薄弱知识点（按错题数降序）：");
        out.printf("%-24s %-12s %-8s %-10s %s%n", "算法", "分类", "错题数", "累计重做", "平均重做");
        for (Dao.Weakness w : rows) {
            out.printf("%-24s %-12s %-8d %-10d %.2f%n",
                    w.algorithmName(), w.category(), w.mistakeCount(), w.totalRedo(), w.avgRedo());
        }
    }

    private void printHelp() {
        out.println("""
                命令：
                  algo            列出全部算法
                  list [算法名]   列出全部/指定算法的错题
                  add             录入一条错题（算法 → 题目 → 错误答案 → 原因）
                  redo <id>       该题重做成功，重做次数 +1
                  del <id>        删除错题
                  weak            按知识点统计薄弱项
                  help            显示本帮助
                  quit            退出（数据已实时入库）""");
    }
}
