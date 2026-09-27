package com.javadslab.persist;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 建库 + 造数：执行 sql/schema.sql，注册算法字典，批量生成 10 万条运行记录与错题。
 * 全部经 DAO/PreparedStatement 完成，事务批量写入。
 */
public final class SeedData {

    /** 算法字典：name / category / difficulty / description。 */
    public static final List<String[]> ALGORITHMS = List.of(
            row("arraylist", "linear", "1", "动态数组"),
            row("singly-linked-list", "linear", "1", "单链表"),
            row("doubly-linked-list", "linear", "2", "双向链表"),
            row("stack", "stack_queue", "1", "顺序栈"),
            row("queue", "stack_queue", "1", "队列"),
            row("circular-queue", "stack_queue", "2", "循环队列"),
            row("monotonic-stack", "stack_queue", "3", "单调栈"),
            row("hash-chaining", "hash", "3", "拉链法哈希表"),
            row("hash-open-addressing", "hash", "4", "开放寻址哈希表"),
            row("union-find", "hash", "3", "并查集"),
            row("heap", "tree", "3", "二叉堆"),
            row("trie", "tree", "3", "前缀树"),
            row("segment-tree", "tree", "4", "线段树"),
            row("fenwick-tree", "tree", "4", "树状数组"),
            row("bst", "tree", "2", "二叉搜索树"),
            row("avl", "tree", "4", "AVL 平衡树"),
            row("red-black-tree", "tree", "5", "红黑树"),
            row("btree", "tree", "5", "B 树"),
            row("bfs", "graph", "2", "广度优先搜索"),
            row("dfs", "graph", "2", "深度优先搜索"),
            row("dijkstra", "graph", "4", "最短路"),
            row("bellman-ford", "graph", "4", "负权最短路"),
            row("floyd", "graph", "3", "全源最短路"),
            row("prim", "graph", "4", "最小生成树"),
            row("kruskal", "graph", "4", "最小生成树"),
            row("topological-sort", "graph", "3", "拓扑排序"),
            row("bubble-sort", "sort", "1", "冒泡排序"),
            row("quick-sort", "sort", "3", "快速排序"),
            row("merge-sort", "sort", "2", "归并排序"),
            row("radix-sort", "sort", "3", "基数排序"),
            row("binary-search", "search", "1", "二分查找"),
            row("kmp", "string", "4", "字符串匹配"),
            row("dp-lcs", "dp", "4", "最长公共子序列"),
            row("dp-knapsack", "dp", "4", "0/1 背包"));

    private static String[] row(String name, String cat, String diff, String desc) {
        return new String[]{name, cat, diff, desc};
    }

    /** 执行 schema.sql 建表。 */
    public static void applySchema(Db db, Path schemaFile) {
        db.inTransaction((Db.SqlWork<Void>) c -> {
            try (Statement st = c.createStatement()) {
                for (String stmt : splitStatements(Files.readString(schemaFile, StandardCharsets.UTF_8))) {
                    st.execute(stmt);
                }
            }
            return null;
        });
    }

    static List<String> splitStatements(String script) {
        List<String> out = new ArrayList<>();
        for (String raw : script.split(";")) {
            String s = raw.replaceAll("--[^\\n]*", "").trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    /**
     * 造 10 万条 runs（分布到全部算法，权重不同）、部分 steps、错题样本。
     * 返回写入统计。
     */
    public record SeedStats(long runs, long steps, long mistakes, long elapsedMs) {}

    public static SeedStats seed(Db db, Dao dao, long targetRuns, long seed) {
        long t0 = System.currentTimeMillis();
        int inserted = dao.seedAlgorithmsIfAbsent(ALGORITHMS);
        Random rnd = new Random(seed);

        // 每个算法的目标运行量：总量按难度反比加权分布
        List<String[]> algos = ALGORITHMS;
        long[] perAlgo = new long[algos.size()];
        long weightSumCalc = 0;
        for (int i = 0; i < algos.size(); i++) {
            int weight = 6 - Integer.parseInt(algos.get(i)[2]); // 难度低的运行更多
            perAlgo[i] = Math.max(1, weight);
            weightSumCalc += perAlgo[i];
        }
        final long weightSum = weightSumCalc;

        long[] actual = new long[algos.size()];
        db.inTransaction((Db.SqlWork<Void>) c -> {
            try (var batch = c.prepareStatement("INSERT INTO runs(algorithm_id, duration_ms, input_size, passed, score) VALUES (?,?,?,?,?)")) {
                long[] written = {0};
                while (written[0] < targetRuns) {
                    int idx = pickWeighted(perAlgo, weightSum, rnd);
                    if (actual[idx] >= perAlgo[idx] * targetRuns / weightSum + 2) continue; // 按比例控制
                    int difficulty = Integer.parseInt(algos.get(idx)[2]);
                    int inputSize = 100 + rnd.nextInt(10_000);
                    // 耗时 ≈ 复杂度基线 * log(输入规模) + 噪声
                    int duration = (int) (difficulty * 3.0 * Math.log(inputSize) * (0.5 + rnd.nextDouble()));
                    boolean passed = rnd.nextDouble() > difficulty * 0.05; // 难度越高越易错
                    int score = passed ? 60 + rnd.nextInt(41) : rnd.nextInt(60);
                    batch.setLong(1, idx + 1);
                    batch.setInt(2, duration);
                    batch.setInt(3, inputSize);
                    batch.setInt(4, passed ? 1 : 0);
                    batch.setDouble(5, score / 100.0);
                    batch.addBatch();
                    written[0]++;
                    if (written[0] % 10_000 == 0) batch.executeBatch();
                }
                batch.executeBatch();
            }
            return null;
        });
        for (int i = 0; i < actual.length; i++) actual[i] = -1; // 统计见 countRuns

        // steps：给前 200 个 run 各写 20 步，演示 run↔steps 的 1:N
        long stepCount = 0;
        for (long runId = 1; runId <= 200; runId++) {
            for (int s = 0; s < 20; s++) {
                dao.insertStep(runId, s, s % 2 == 0 ? "compare" : "swap", "step-" + s);
                stepCount++;
            }
        }

        // 错题样本
        String[] reasons = {"边界条件忽略空数组", "下标越界 off-by-one", "循环不变量理解错误",
                "复杂度选错算法", "递归终止条件写错", "取模/溢出处理遗漏"};
        String[] questions = {"手写实现第 %d 题", "课堂练习 %d", "期末模拟 %d"};
        long mistakeCount = 0;
        for (int i = 0; i < 60; i++) {
            long algId = 1 + rnd.nextInt(algos.size());
            dao.addMistake(algId, String.format(questions[i % 3], i + 1),
                    "WA" + (i % 9 + 1), reasons[rnd.nextInt(reasons.length)]);
            mistakeCount++;
            if (i % 3 == 0) dao.markRedo(algId); // 部分错题有重做记录
        }

        long runs = dao.countRuns();
        return new SeedStats(runs, stepCount, mistakeCount, System.currentTimeMillis() - t0);
    }

    private static int pickWeighted(long[] weights, long total, Random rnd) {
        long x = (long) (rnd.nextDouble() * total);
        long acc = 0;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (x < acc) return i;
        }
        return weights.length - 1;
    }
}
