package com.javadslab.workbench;

import com.javadslab.core.dp.DynamicProgramming;
import com.javadslab.core.graph.Graph;
import com.javadslab.core.graph.GraphSearch;
import com.javadslab.core.graph.MinimumSpanningTree;
import com.javadslab.core.graph.ShortestPath;
import com.javadslab.core.graph.TopologicalSorter;
import com.javadslab.core.hash.ChainingHashMap;
import com.javadslab.core.hash.OpenAddressingHashMap;
import com.javadslab.core.heap.MyHeap;
import com.javadslab.core.linear.DoublyLinkedList;
import com.javadslab.core.linear.MyArrayList;
import com.javadslab.core.linear.SinglyLinkedList;
import com.javadslab.core.search.BinarySearch;
import com.javadslab.core.search.KMP;
import com.javadslab.core.sort.QuickSortOptimized;
import com.javadslab.core.sort.SortingAlgorithms;
import com.javadslab.core.stackqueue.ArrayQueue;
import com.javadslab.core.stackqueue.ArrayStack;
import com.javadslab.core.stackqueue.CircularQueue;
import com.javadslab.core.stackqueue.MonotonicStack;
import com.javadslab.core.tree.AVLTree;
import com.javadslab.core.tree.BST;
import com.javadslab.core.tree.BTree;
import com.javadslab.core.tree.BinaryTrees;
import com.javadslab.core.tree.FenwickTree;
import com.javadslab.core.tree.RedBlackTree;
import com.javadslab.core.tree.SegmentTree;
import com.javadslab.core.tree.Trie;
import com.javadslab.core.unionfind.UnionFind;
import com.javadslab.trace.Json;
import com.javadslab.trace.TraceableDP;
import com.javadslab.trace.TraceableQuickSort3Way;
import com.javadslab.trace.TraceableSearch;
import com.javadslab.trace.TraceableSorts;
import com.javadslab.trace.TraceableTrees;
import com.javadslab.trace.Tracer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 实验场执行器：解析前端输入 → 服务端校验（最终可信边界，§10）→ 调用项目真实 Java 实现 →
 * 产出 result 与（可选）trace。绝不重新实现算法——排序/查找/DP 直接复用 core 与 Traceable*，
 * 数据结构复用 core 结构并 attachTracer，与 TraceGenerator 演示脚本同一机制，trace 格式不变（§11.2）。
 *
 * 耗时区分（§11.3）：elapsedResultNs=纯算法耗时；elapsedTraceNs=Trace 记录耗时。
 * 规模上限（§36）：超过则明确报错；trace 步数超限自动降级为"仅结果模式"并说明。
 */
public final class LabRunner {

    /** 输入不合法（前端需要原样展示给用户）。 */
    public static final class InvalidInput extends RuntimeException {
        public final String field;
        public InvalidInput(String field, String message) {
            super(message);
            this.field = field;
        }
    }

    /** 规模上限（§36：以现有算法性能实测为准的保守值）。 */
    private static final int MAX_ARRAY = 20_000;
    private static final int MAX_ARRAY_FOR_TRACE = 5_000;
    private static final int MAX_GRAPH_N = 300;
    private static final int MAX_GRAPH_E = 3_000;
    private static final int MAX_DP_N = 1_000_000;          // 爬楼梯仅 O(n) 递推
    private static final int MAX_COIN_AMOUNT = 100_000;
    private static final int MAX_KNAPSACK_CAP = 100_000;
    private static final int MAX_STRING = 2_000;
    private static final int MAX_OPS = 500;
    private static final int MAX_TRACE_STEPS = 3_000;

    public record LabResult(String resultJson, String traceJson, long elapsedNs) {}

    /* ==================== 主入口 ==================== */

    @SuppressWarnings("unchecked")
    public LabResult run(String featureId, String inputJson) {
        FeatureCatalog.FeatureDef def = FeatureCatalog.find(featureId)
                .orElseThrow(() -> new InvalidInput("featureId", "未知功能：" + featureId));
        Object input;
        try {
            input = Json.parse(inputJson);
        } catch (RuntimeException e) {
            throw new InvalidInput("input", "输入 JSON 无法解析：" + e.getMessage());
        }
        if (!(input instanceof Map)) throw new InvalidInput("input", "输入必须是 JSON 对象");
        Map<String, Object> in = (Map<String, Object>) input;

        return switch (featureId) {
            /* ---- 排序（真实 core 实现 + Traceable 双跑） ---- */
            case "sort-bubble" -> runSort(def, in, SortingAlgorithms::bubbleSort, TraceableSorts::bubbleSort);
            case "sort-insertion" -> runSort(def, in, SortingAlgorithms::insertionSort, TraceableSorts::insertionSort);
            case "sort-selection" -> runSort(def, in, SortingAlgorithms::selectionSort, TraceableSorts::selectionSort);
            case "sort-shell" -> runSort(def, in, SortingAlgorithms::shellSort, TraceableSorts::shellSort);
            case "sort-quick" -> runSort(def, in, SortingAlgorithms::quickSort, TraceableSorts::quickSort);
            case "sort-heap" -> runSort(def, in, SortingAlgorithms::heapSort, TraceableSorts::heapSort);
            case "sort-merge" -> runSort(def, in,
                    a -> { int[] r = com.javadslab.core.sort.MergeSort.sort(a); System.arraycopy(r, 0, a, 0, a.length); },
                    TraceableSorts::mergeSort);
            case "sort-counting" -> runSort(def, in, SortingAlgorithms::countingSort, TraceableSorts::countingSort);
            case "sort-radix" -> runSort(def, in, SortingAlgorithms::radixSort, TraceableSorts::radixSort);
            case "sort-bucket" -> runSort(def, in, SortingAlgorithms::bucketSort, TraceableSorts::bucketSort);
            case "quick-sort-3way" -> runSort(def, in, QuickSortOptimized::sort, TraceableQuickSort3Way::sort);

            /* ---- 查找 / 字符串 ---- */
            case "binary-search" -> runBinarySearch(in);
            case "kmp" -> runKmp(in);

            /* ---- 树/图（core 类自带 tracer 机制，一次调用同时得 result 与 trace） ---- */
            case "tree-traversal" -> runTreeTraversal(in);
            case "bfs" -> runBfs(in);
            case "dfs" -> runDfs(in);
            case "dijkstra" -> runDijkstra(in);
            case "bellman-ford" -> runBellmanFord(in);
            case "floyd" -> runFloyd(in);
            case "prim" -> runPrim(in);
            case "kruskal" -> runKruskal(in);
            case "topological-sort" -> runTopo(in);

            /* ---- DP ---- */
            case "dp-climb-stairs" -> runClimbStairs(in);
            case "dp-coin-change" -> runCoinChange(in);
            case "dp-lcs" -> runLcs(in);
            case "dp-knapsack" -> runKnapsack(in);
            case "monotonic-stack" -> runMonotonicStack(in);

            /* ---- 数据结构操作序列 ---- */
            default -> runStructure(featureId, in);
        };
    }

    /* ==================== 排序 ==================== */

    interface Sorter { void sort(int[] a); }
    interface TracedSorter { Tracer sort(int[] a); }

    private LabResult runSort(FeatureCatalog.FeatureDef def, Map<String, Object> in,
                              Sorter real, TracedSorter traced) {
        int[] a = intArrayField(in, "array", MAX_ARRAY, null);
        int[] copy1 = a.clone();
        long t0 = System.nanoTime();
        real.sort(copy1);
        long resultNs = System.nanoTime() - t0;

        String traceJson = null;
        long traceNs = 0;
        String skipped = null;
        if (a.length <= MAX_ARRAY_FOR_TRACE) {
            try {
                long t1 = System.nanoTime();
                Object tr = traced.sort(a.clone()).toTrace();
                traceNs = System.nanoTime() - t1;
                traceJson = Json.write(tr);
            } catch (RuntimeException e) {
                skipped = "Trace 生成失败（" + e.getMessage() + "），已降级为仅结果模式。";
            }
        } else {
            skipped = "数组长度超过 " + MAX_ARRAY_FOR_TRACE + "，为避免 Trace 数据量过大已跳过 Trace 记录（仅结果模式）。";
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", def.name() + "：n=" + a.length);
        result.put("sorted", toList(copy1));
        if (skipped != null) result.put("traceSkipped", skipped);
        return finish(result, traceJson, resultNs, traceNs);
    }

    /* ==================== 查找 / 字符串 ==================== */

    private LabResult runBinarySearch(Map<String, Object> in) {
        int[] a = intArrayField(in, "array", MAX_ARRAY, null);
        for (int i = 1; i < a.length; i++) {
            if (a[i - 1] > a[i]) {
                throw new InvalidInput("array", "该算法要求输入数组已经升序排列。当前数组在位置 " + (i - 1) + "→" + i
                        + "（" + a[i - 1] + " > " + a[i] + "）不是升序。");
            }
        }
        int target = intField(in, "target", null);
        long t0 = System.nanoTime();
        int idx = BinarySearch.search(a, target);
        long resultNs = System.nanoTime() - t0;
        long t1 = System.nanoTime();
        String traceJson = Json.write(TraceableSearch.binarySearch(a.clone(), target).toTrace());
        long traceNs = System.nanoTime() - t1;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", idx >= 0 ? "找到目标值，下标 " + idx : "数组中不存在目标值（返回 -1）");
        result.put("index", idx);
        result.put("target", target);
        return finish(result, traceJson, resultNs, traceNs);
    }

    private LabResult runKmp(Map<String, Object> in) {
        String text = stringField(in, "text", MAX_STRING, "text");
        String pattern = stringField(in, "pattern", 1000, "pattern");
        if (pattern.isEmpty()) throw new InvalidInput("pattern", "模式串不能为空");
        long t0 = System.nanoTime();
        int[] next = KMP.buildNext(pattern);
        List<Integer> hits = KMP.searchAll(text, pattern);
        long resultNs = System.nanoTime() - t0;
        long t1 = System.nanoTime();
        String traceJson = Json.write(TraceableSearch.kmp(text, pattern).toTrace());
        long traceNs = System.nanoTime() - t1;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", hits.isEmpty()
                ? "未匹配到（文本长度 " + text.length() + "，模式长度 " + pattern.length() + "）"
                : "匹配成功 " + hits.size() + " 处：" + hits);
        result.put("matches", hits);
        result.put("next", next);
        return finish(result, traceJson, resultNs, traceNs);
    }

    /* ==================== 树遍历 ==================== */

    private LabResult runTreeTraversal(Map<String, Object> in) {
        int[] arr = intArrayField(in, "array", 1023, null); // 完全二叉数组上限 2^10-1
        Integer[] boxed = new Integer[arr.length];
        for (int i = 0; i < arr.length; i++) boxed[i] = arr[i];
        long t0 = System.nanoTime();
        BinaryTrees.TreeNode root = BinaryTrees.build(boxed);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("preOrder", root == null ? List.of() : BinaryTrees.preOrder(root));
        result.put("inOrder", root == null ? List.of() : BinaryTrees.inOrder(root));
        result.put("postOrder", root == null ? List.of() : BinaryTrees.postOrder(root));
        result.put("levelOrder", root == null ? List.of() : BinaryTrees.levelOrder(root));
        result.put("height", root == null ? 0 : BinaryTrees.height(root));
        long resultNs = System.nanoTime() - t0;
        long t1 = System.nanoTime();
        String traceJson = Json.write(TraceableTrees.fourTraversals(boxed).toTrace());
        long traceNs = System.nanoTime() - t1;
        result.put("summary", "层序建树（" + arr.length + " 个节点，高度 " + result.get("height") + "），四种遍历完成");
        return finish(result, traceJson, resultNs, traceNs);
    }

    /* ==================== 图 ==================== */

    private record GraphInput(Graph g, List<Graph.Edge> edgeList, int n, int start, boolean directed) {}

    private GraphInput parseGraph(Map<String, Object> in, boolean allowNegative, boolean needStart) {
        int n = (int) longField(in, "n", "n");
        if (n < 1 || n > MAX_GRAPH_N) throw new InvalidInput("n", "顶点数需在 1~" + MAX_GRAPH_N + " 之间");
        boolean directed = boolField(in, "directed", false);
        List<List<Object>> rawEdges = edgesField(in);
        if (rawEdges.size() > MAX_GRAPH_E) throw new InvalidInput("edges", "边数不能超过 " + MAX_GRAPH_E);
        Graph g = new Graph(n, directed);
        List<Graph.Edge> edgeList = new ArrayList<>();
        int idx = 0;
        for (List<Object> e : rawEdges) {
            idx++;
            if (e.size() < 2) throw new InvalidInput("edges", "第 " + idx + " 条边至少需要 顶点u 顶点v 两个数");
            long u = numOf(e.get(0)), v = numOf(e.get(1));
            long w = e.size() >= 3 ? numOf(e.get(2)) : 1;
            if (u < 0 || u >= n || v < 0 || v >= n) {
                throw new InvalidInput("edges", "第 " + idx + " 条边 (" + u + "," + v + ") 超出顶点范围 0~" + (n - 1));
            }
            if (w < -100_000 || w > 100_000) throw new InvalidInput("edges", "第 " + idx + " 条边权值超出 ±100000");
            if (!allowNegative && w < 0) {
                throw new InvalidInput("edges", "该算法要求边权非负，第 " + idx + " 条边权值为 " + w);
            }
            // Graph.addEdge 拒绝负权边（core 设计）；bellman-ford 只需要边表，不建 Graph
            if (!allowNegative) g.addEdge((int) u, (int) v, (int) w);
            edgeList.add(new Graph.Edge((int) u, (int) v, (int) w));
        }
        int start = 0;
        if (needStart) {
            start = (int) longField(in, "start", "start");
            if (start < 0 || start >= n) throw new InvalidInput("start", "起点必须在 0~" + (n - 1) + " 之间");
        }
        return new GraphInput(g, edgeList, n, start, directed);
    }

    private LabResult runBfs(Map<String, Object> in) {
        GraphInput gi = parseGraph(in, true, true);
        GraphSearch search = new GraphSearch();
        Tracer t = new Tracer("graph", "广度优先搜索 BFS");
        search.attachTracer(t);
        long t0 = System.nanoTime();
        List<Integer> order = search.bfs(gi.g(), gi.start());
        long resultNs = System.nanoTime() - t0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "从 " + gi.start() + " 出发访问 " + order.size() + " 个顶点");
        result.put("order", order);
        return finishWithTraceObj(result, t, resultNs);
    }

    private LabResult runDfs(Map<String, Object> in) {
        GraphInput gi = parseGraph(in, true, true);
        GraphSearch search = new GraphSearch();
        Tracer t = new Tracer("graph", "深度优先搜索 DFS");
        search.attachTracer(t);
        long t0 = System.nanoTime();
        List<Integer> order = search.dfsRecursive(gi.g(), gi.start());
        long resultNs = System.nanoTime() - t0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "从 " + gi.start() + " 出发访问 " + order.size() + " 个顶点");
        result.put("order", order);
        return finishWithTraceObj(result, t, resultNs);
    }

    private LabResult runDijkstra(Map<String, Object> in) {
        GraphInput gi = parseGraph(in, false, true);
        ShortestPath sp = new ShortestPath();
        Tracer t = new Tracer("graph", "Dijkstra 最短路（堆优化）");
        sp.attachTracer(t);
        long t0 = System.nanoTime();
        ShortestPath.Result r = sp.dijkstra(gi.g(), gi.start());
        long resultNs = System.nanoTime() - t0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "单源最短路（源 " + gi.start() + "）计算完成");
        result.put("dist", toList(r.dist()));
        result.put("prev", toList(r.prev()));
        return finishWithTraceObj(result, t, resultNs);
    }

    private LabResult runBellmanFord(Map<String, Object> in) {
        GraphInput gi = parseGraph(in, true, true);
        ShortestPath sp = new ShortestPath();
        Tracer t = new Tracer("graph", "Bellman-Ford（逐轮松弛）");
        sp.attachTracer(t);
        long t0 = System.nanoTime();
        int[] dist = sp.bellmanFord(gi.n(), gi.edgeList(), gi.start());
        long resultNs = System.nanoTime() - t0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "Bellman-Ford 完成（n=" + gi.n() + "，m=" + gi.edgeList().size() + "）");
        result.put("dist", toList(dist));
        return finishWithTraceObj(result, t, resultNs);
    }

    private LabResult runFloyd(Map<String, Object> in) {
        GraphInput gi = parseGraph(in, false, false);
        ShortestPath sp = new ShortestPath();
        Tracer t = new Tracer("matrix", "Floyd 全源最短路（DP 表）");
        sp.attachTracer(t);
        long t0 = System.nanoTime();
        int[][] dist = sp.floyd(gi.n(), gi.edgeList(), gi.directed());
        long resultNs = System.nanoTime() - t0;
        List<List<Object>> matrix = new ArrayList<>();
        for (int[] row : dist) matrix.add(toList(row));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "全源最短路矩阵（" + gi.n() + "×" + gi.n() + "）计算完成");
        result.put("matrix", matrix);
        return finishWithTraceObj(result, t, resultNs);
    }

    private LabResult runPrim(Map<String, Object> in) {
        GraphInput gi = parseGraph(in, false, true);
        MinimumSpanningTree mst = new MinimumSpanningTree();
        Tracer t = new Tracer("graph", "Prim 最小生成树");
        mst.attachTracer(t);
        long t0 = System.nanoTime();
        MinimumSpanningTree.MstResult r = mst.prim(gi.g(), gi.start());
        long resultNs = System.nanoTime() - t0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "最小生成树总权 " + r.totalWeight() + "，含 " + r.edges().size() + " 条边");
        result.put("totalWeight", r.totalWeight());
        List<List<Object>> edges = new ArrayList<>();
        for (Graph.Edge e : r.edges()) edges.add(List.of(e.u(), e.v(), e.w()));
        result.put("edges", edges);
        return finishWithTraceObj(result, t, resultNs);
    }

    private LabResult runKruskal(Map<String, Object> in) {
        GraphInput gi = parseGraph(in, false, false);
        MinimumSpanningTree mst = new MinimumSpanningTree();
        Tracer t = new Tracer("graph", "Kruskal 最小生成树（排序 + 并查集）");
        mst.attachTracer(t);
        long t0 = System.nanoTime();
        MinimumSpanningTree.MstResult r = mst.kruskal(gi.g());
        long resultNs = System.nanoTime() - t0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "最小生成树总权 " + r.totalWeight() + "，含 " + r.edges().size() + " 条边");
        result.put("totalWeight", r.totalWeight());
        List<List<Object>> edges = new ArrayList<>();
        for (Graph.Edge e : r.edges()) edges.add(List.of(e.u(), e.v(), e.w()));
        result.put("edges", edges);
        return finishWithTraceObj(result, t, resultNs);
    }

    private LabResult runTopo(Map<String, Object> in) {
        GraphInput gi = parseGraph(in, true, false);
        TopologicalSorter sorter = new TopologicalSorter();
        Tracer t = new Tracer("graph", "拓扑排序（Kahn 入度法）");
        sorter.attachTracer(t);
        long t0 = System.nanoTime();
        List<Integer> order = sorter.kahn(gi.g());
        long resultNs = System.nanoTime() - t0;
        Map<String, Object> result = new LinkedHashMap<>();
        boolean cyclic = order.size() < gi.n();
        result.put("summary", cyclic
                ? "图中存在环，无法完成拓扑排序（仅访问 " + order.size() + "/" + gi.n() + " 个顶点）"
                : "拓扑序：" + order);
        result.put("order", order);
        result.put("hasCycle", cyclic);
        return finishWithTraceObj(result, t, resultNs);
    }

    /* ==================== DP ==================== */

    private LabResult runClimbStairs(Map<String, Object> in) {
        int n = (int) longField(in, "n", "n");
        if (n < 1 || n > MAX_DP_N) throw new InvalidInput("n", "阶梯数需在 1~" + MAX_DP_N + " 之间");
        long t0 = System.nanoTime();
        long ways = DynamicProgramming.climbStairs(n);
        long resultNs = System.nanoTime() - t0;
        String traceJson = null;
        long traceNs = 0;
        String skipped = null;
        if (n <= 40) {
            traceNs = System.nanoTime();
            traceJson = Json.write(TraceableDP.climbStairs(n).toTrace());
            traceNs = System.nanoTime() - traceNs;
        } else {
            skipped = "n > 40 时 DP 表过大，已跳过 Trace 记录（仅结果模式）。";
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "爬到第 " + n + " 阶共有 " + ways + " 种方法");
        result.put("ways", ways);
        if (skipped != null) result.put("traceSkipped", skipped);
        return finish(result, traceJson, resultNs, traceNs);
    }

    private LabResult runCoinChange(Map<String, Object> in) {
        int[] coins = intArrayField(in, "coins", 100, "coins");
        if (coins.length == 0) throw new InvalidInput("coins", "币值数组不能为空");
        for (int c : coins) if (c <= 0) throw new InvalidInput("coins", "币值必须为正整数");
        int amount = (int) longField(in, "amount", "amount");
        if (amount < 0 || amount > MAX_COIN_AMOUNT) throw new InvalidInput("amount", "金额需在 0~" + MAX_COIN_AMOUNT + " 之间");
        long t0 = System.nanoTime();
        int ans = DynamicProgramming.coinChange(coins, amount);
        long resultNs = System.nanoTime() - t0;
        String traceJson = null;
        long traceNs = 0;
        String skipped = null;
        if ((long) coins.length * amount <= 10_000) {
            traceNs = System.nanoTime();
            traceJson = Json.write(TraceableDP.coinChange(coins.clone(), amount).toTrace());
            traceNs = System.nanoTime() - traceNs;
        } else {
            skipped = "规模超过 Trace 上限（币值数×金额 ≤ 10000），已跳过 Trace 记录（仅结果模式）。";
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", ans < 0 ? "无法凑出金额 " + amount : "最少需要 " + ans + " 枚硬币");
        result.put("minCoins", ans);
        if (skipped != null) result.put("traceSkipped", skipped);
        return finish(result, traceJson, resultNs, traceNs);
    }

    private LabResult runLcs(Map<String, Object> in) {
        String s1 = stringField(in, "s1", MAX_STRING, "s1");
        String s2 = stringField(in, "s2", MAX_STRING, "s2");
        if (s1.isEmpty() || s2.isEmpty()) throw new InvalidInput("s1", "两个字符串都不能为空");
        long t0 = System.nanoTime();
        int len = DynamicProgramming.longestCommonSubsequence(s1, s2);
        long resultNs = System.nanoTime() - t0;
        String traceJson = null;
        long traceNs = 0;
        String skipped = null;
        if ((long) s1.length() * s2.length() <= 400) {
            traceNs = System.nanoTime();
            traceJson = Json.write(TraceableDP.lcs(s1, s2).toTrace());
            traceNs = System.nanoTime() - traceNs;
        } else {
            skipped = "规模超过 Trace 上限（长度乘积 ≤ 400），已跳过 Trace 记录（仅结果模式）。";
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "最长公共子序列长度 " + len);
        result.put("length", len);
        if (skipped != null) result.put("traceSkipped", skipped);
        return finish(result, traceJson, resultNs, traceNs);
    }

    private LabResult runKnapsack(Map<String, Object> in) {
        int[] w = intArrayField(in, "weights", 200, "weights");
        int[] v = intArrayField(in, "values", 200, "values");
        if (w.length != v.length) throw new InvalidInput("weights", "重量与价值数组长度必须相同（当前 " + w.length + " vs " + v.length + "）");
        if (w.length == 0) throw new InvalidInput("weights", "物品不能为空");
        int cap = (int) longField(in, "capacity", "capacity");
        if (cap < 0 || cap > MAX_KNAPSACK_CAP) throw new InvalidInput("capacity", "容量需在 0~" + MAX_KNAPSACK_CAP + " 之间");
        long t0 = System.nanoTime();
        int best = DynamicProgramming.knapsack01(w, v, cap);
        long resultNs = System.nanoTime() - t0;
        String traceJson = null;
        long traceNs = 0;
        String skipped = null;
        if ((long) w.length * cap <= 2_000) {
            traceNs = System.nanoTime();
            traceJson = Json.write(TraceableDP.knapsack01(w.clone(), v.clone(), cap).toTrace());
            traceNs = System.nanoTime() - traceNs;
        } else {
            skipped = "规模超过 Trace 上限（物品数×容量 ≤ 2000），已跳过 Trace 记录（仅结果模式）。";
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "容量 " + cap + " 下最大价值 " + best);
        result.put("best", best);
        if (skipped != null) result.put("traceSkipped", skipped);
        return finish(result, traceJson, resultNs, traceNs);
    }

    private LabResult runMonotonicStack(Map<String, Object> in) {
        int[] a = intArrayField(in, "array", MAX_ARRAY, null);
        MonotonicStack ms = new MonotonicStack();
        Tracer t = new Tracer("monostack", "单调栈：下一个更大元素");
        ms.attachTracer(t);
        long t0 = System.nanoTime();
        int[] idx = ms.nextGreaterIndex(a);
        int[] val = ms.nextGreaterValue(a);
        long resultNs = System.nanoTime() - t0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "单调栈一遍扫描完成（n=" + a.length + "）");
        result.put("nextGreaterIndex", toList(idx));
        result.put("nextGreaterValue", toList(val));
        return finishWithTraceObj(result, t, resultNs);
    }

    /* ==================== 数据结构操作序列 ==================== */

    private LabResult runStructure(String featureId, Map<String, Object> in) {
        List<String> ops = opsField(in);
        Integer capacity = optIntField(in, "capacity");
        int[] init = optIntArrayField(in, "init", MAX_ARRAY);
        List<Map<String, Object>> opResults = new ArrayList<>();

        long t0 = System.nanoTime();
        RunOutcome outcome = executeStructure(featureId, ops, capacity, init, opResults);
        long resultNs = System.nanoTime() - t0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "执行了 " + ops.size() + " 个操作"
                + (outcome.finalState() != null && !outcome.finalState().isEmpty()
                        ? "；" + outcome.finalState() : ""));
        result.put("ops", opResults);
        if (outcome.traceJson() == null) {
            result.put("traceSkipped", "未生成 Trace（该结构无步骤可记录，或步骤数超过上限 " + MAX_TRACE_STEPS + "）。");
        }
        return finish(result, outcome.traceJson(), resultNs, 0);
    }

    private record RunOutcome(String traceJson, String finalState) {}

    /** 执行操作序列；返回 trace JSON（null 表示结构不支持 tracer 或步数超限）与最终状态描述。 */
    private RunOutcome executeStructure(String featureId, List<String> ops, Integer capacity, int[] init,
                                        List<Map<String, Object>> opResults) {
        Tracer t = new Tracer(kindOf(featureId), titleOf(featureId));
        StepRecorder rec = new StepRecorder(opResults);
        switch (featureId) {
            case "arraylist" -> {
                MyArrayList<Object> l = new MyArrayList<>();
                l.attachTracer(t);
                for (String[] p : parse(ops)) {
                    switch (p[0]) {
                        case "add" -> { if (p.length >= 3) { int i = pi(p[1], "下标"); l.add(i, pv(p[2])); rec.add(p, "inserted"); } else { l.add(pv(p[1])); rec.add(p, "added"); } }
                        case "set" -> { int i = pi(p[1], "下标"); Object old = l.set(i, pv(p[2])); rec.add(p, "replaced " + old); }
                        case "get" -> { int i = pi(p[1], "下标"); rec.add(p, String.valueOf(l.get(i))); }
                        case "remove" -> rec.add(p, String.valueOf(l.remove(pi(p[1], "下标"))));
                        case "contains" -> rec.add(p, String.valueOf(l.contains(pv(p[1]))));
                        case "size" -> rec.add(p, String.valueOf(l.size()));
                        default -> bad(p, "add [i] v | set i v | get i | remove i | contains v | size");
                    }
                }
                rec.finalState("size=" + l.size() + " 内容=" + l);
            }
            case "singly-linked-list" -> {
                SinglyLinkedList<Object> l = new SinglyLinkedList<>();
                l.attachTracer(t);
                for (String[] p : parse(ops)) {
                    switch (p[0]) {
                        case "addFirst" -> { l.addFirst(pv(p[1])); rec.add(p, "ok"); }
                        case "addLast" -> { l.addLast(pv(p[1])); rec.add(p, "ok"); }
                        case "add" -> { l.add(pi(p[1], "下标"), pv(p[2])); rec.add(p, "ok"); }
                        case "get" -> rec.add(p, String.valueOf(l.get(pi(p[1], "下标"))));
                        case "set" -> rec.add(p, "replaced " + l.set(pi(p[1], "下标"), pv(p[2])));
                        case "removeFirst" -> rec.add(p, String.valueOf(l.removeFirst()));
                        case "removeAt" -> rec.add(p, String.valueOf(l.removeAt(pi(p[1], "下标"))));
                        case "removeValue" -> rec.add(p, String.valueOf(l.removeValue(pv(p[1]))));
                        case "indexOf" -> rec.add(p, String.valueOf(l.indexOf(pv(p[1]))));
                        case "size" -> rec.add(p, String.valueOf(l.size()));
                        default -> bad(p, "addFirst v | addLast v | add i v | get i | set i v | removeFirst | removeAt i | removeValue v | indexOf v | size");
                    }
                }
                rec.finalState("size=" + l.size() + " 内容=" + l);
            }
            case "doubly-linked-list" -> {
                DoublyLinkedList<Object> l = new DoublyLinkedList<>();
                l.attachTracer(t);
                for (String[] p : parse(ops)) {
                    switch (p[0]) {
                        case "addFirst" -> { l.addFirst(pv(p[1])); rec.add(p, "ok"); }
                        case "addLast" -> { l.addLast(pv(p[1])); rec.add(p, "ok"); }
                        case "add" -> { l.add(pi(p[1], "下标"), pv(p[2])); rec.add(p, "ok"); }
                        case "get" -> rec.add(p, String.valueOf(l.get(pi(p[1], "下标"))));
                        case "getFirst" -> rec.add(p, String.valueOf(l.getFirst()));
                        case "getLast" -> rec.add(p, String.valueOf(l.getLast()));
                        case "removeFirst" -> rec.add(p, String.valueOf(l.removeFirst()));
                        case "removeLast" -> rec.add(p, String.valueOf(l.removeLast()));
                        case "removeAt" -> rec.add(p, String.valueOf(l.removeAt(pi(p[1], "下标"))));
                        case "size" -> rec.add(p, String.valueOf(l.size()));
                        default -> bad(p, "addFirst v | addLast v | add i v | get i | getFirst | getLast | removeFirst | removeLast | removeAt i | size");
                    }
                }
                rec.finalState("size=" + l.size() + " 内容=" + l);
            }
            case "stack" -> {
                ArrayStack<Object> s = new ArrayStack<>();
                s.attachTracer(t);
                for (String[] p : parse(ops)) {
                    switch (p[0]) {
                        case "push" -> { s.push(pv(p[1])); rec.add(p, "ok"); }
                        case "pop" -> rec.add(p, String.valueOf(s.pop()));
                        case "peek" -> rec.add(p, String.valueOf(s.peek()));
                        case "size" -> rec.add(p, String.valueOf(s.size()));
                        default -> bad(p, "push v | pop | peek | size");
                    }
                }
                rec.finalState("size=" + s.size() + " 栈=" + s);
            }
            case "queue" -> {
                ArrayQueue<Object> q = new ArrayQueue<>();
                q.attachTracer(t);
                for (String[] p : parse(ops)) {
                    switch (p[0]) {
                        case "offer" -> { q.offer(pv(p[1])); rec.add(p, "ok"); }
                        case "poll" -> rec.add(p, String.valueOf(q.poll()));
                        case "peek" -> rec.add(p, String.valueOf(q.peek()));
                        case "size" -> rec.add(p, String.valueOf(q.size()));
                        default -> bad(p, "offer v | poll | peek | size");
                    }
                }
                rec.finalState("size=" + q.size() + " 队列=" + q);
            }
            case "circular-queue" -> {
                int cap = requireCapacity(capacity, 2, 64);
                CircularQueue<Object> q = new CircularQueue<>(cap);
                q.attachTracer(t);
                for (String[] p : parse(ops)) {
                    switch (p[0]) {
                        case "offer" -> rec.add(p, "accepted=" + q.offer(pv(p[1])));
                        case "poll" -> rec.add(p, String.valueOf(q.poll()));
                        case "peek" -> rec.add(p, String.valueOf(q.peek()));
                        case "size" -> rec.add(p, String.valueOf(q.size()));
                        default -> bad(p, "offer v | poll | peek | size");
                    }
                }
                rec.finalState("size=" + q.size() + "/" + cap + " 队列=" + q);
            }
            case "hash-chaining" -> {
                int cap = requireCapacity(capacity, 2, 64);
                ChainingHashMap<Integer, String> m = new ChainingHashMap<>(cap);
                m.attachTracer(t);
                for (String[] p : parse(ops)) {
                    switch (p[0]) {
                        case "put" -> { m.put(pi(p[1], "键"), p[2]); rec.add(p, "ok"); }
                        case "get" -> rec.add(p, String.valueOf(m.get(pi(p[1], "键"))));
                        case "containsKey" -> rec.add(p, String.valueOf(m.containsKey(pi(p[1], "键"))));
                        case "remove" -> rec.add(p, String.valueOf(m.remove(pi(p[1], "键"))));
                        case "size" -> rec.add(p, String.valueOf(m.size()));
                        default -> bad(p, "put k v | get k | containsKey k | remove k | size");
                    }
                }
                rec.finalState("size=" + m.size() + " keys=" + m.keys());
            }
            case "hash-open-addressing" -> {
                int cap = requireCapacity(capacity, 2, 64);
                OpenAddressingHashMap<Integer, String> m = new OpenAddressingHashMap<>(cap);
                m.attachTracer(t);
                for (String[] p : parse(ops)) {
                    switch (p[0]) {
                        case "put" -> { m.put(pi(p[1], "键"), p[2]); rec.add(p, "ok"); }
                        case "get" -> rec.add(p, String.valueOf(m.get(pi(p[1], "键"))));
                        case "containsKey" -> rec.add(p, String.valueOf(m.containsKey(pi(p[1], "键"))));
                        case "remove" -> rec.add(p, String.valueOf(m.remove(pi(p[1], "键"))));
                        case "size" -> rec.add(p, String.valueOf(m.size()));
                        default -> bad(p, "put k v | get k | containsKey k | remove k | size");
                    }
                }
                rec.finalState("size=" + m.size() + " keys=" + m.keys());
            }
            case "union-find" -> {
                Integer n = capacity;
                if (n == null) {
                    for (String[] p : parse(ops)) if (p[0].equals("n")) { n = pi(p[1], "元素数"); break; }
                }
                if (n == null) throw new InvalidInput("ops", "并查集需要先用 \"n 元素数\" 声明元素个数（或填 capacity 字段）");
                if (n < 1 || n > 1000) throw new InvalidInput("n", "元素个数需在 1~1000 之间");
                UnionFind uf = new UnionFind(n);
                uf.attachTracer(t);
                for (String[] p : parse(ops)) {
                    switch (p[0]) {
                        case "n" -> rec.add(p, "元素数 " + n);
                        case "union" -> rec.add(p, "merged=" + uf.union(pi(p[1], "元素"), pi(p[2], "元素")));
                        case "find" -> rec.add(p, String.valueOf(uf.find(pi(p[1], "元素"))));
                        case "connected" -> rec.add(p, String.valueOf(uf.connected(pi(p[1], "元素"), pi(p[2], "元素"))));
                        case "sizeOf" -> rec.add(p, String.valueOf(uf.sizeOf(pi(p[1], "元素"))));
                        case "components" -> rec.add(p, String.valueOf(uf.components()));
                        default -> bad(p, "union a b | find a | connected a b | sizeOf a | components");
                    }
                }
                rec.finalState("连通分量数=" + uf.components());
            }
            case "heap" -> {
                MyHeap<Integer> h = new MyHeap<>();
                h.attachTracer(t);
                for (String[] p : parse(ops)) {
                    switch (p[0]) {
                        case "push" -> { h.push(pi(p[1], "值")); rec.add(p, "ok"); }
                        case "pop" -> rec.add(p, String.valueOf(h.pop()));
                        case "peek" -> rec.add(p, String.valueOf(h.peek()));
                        case "size" -> rec.add(p, String.valueOf(h.size()));
                        default -> bad(p, "push v | pop | peek | size");
                    }
                }
                rec.finalState("size=" + h.size());
            }
            case "trie" -> {
                Trie trie = new Trie();
                trie.attachTracer(t);
                for (String[] p : parse(ops)) {
                    switch (p[0]) {
                        case "insert" -> { word(p[1]); trie.insert(p[1]); rec.add(p, "ok"); }
                        case "search" -> rec.add(p, String.valueOf(trie.search(word(p[1]))));
                        case "startsWith" -> rec.add(p, String.valueOf(trie.startsWith(word(p[1]))));
                        case "countWithPrefix" -> rec.add(p, String.valueOf(trie.countWithPrefix(word(p[1]))));
                        case "delete" -> rec.add(p, String.valueOf(trie.delete(word(p[1]))));
                        case "words" -> rec.add(p, String.valueOf(trie.words()));
                        case "size" -> rec.add(p, String.valueOf(trie.size()));
                        default -> bad(p, "insert w | search w | startsWith p | countWithPrefix p | delete w | words | size");
                    }
                }
                rec.finalState("words=" + trie.words());
            }
            case "segment-tree" -> {
                if (init == null || init.length == 0) throw new InvalidInput("init", "线段树需要 init 字段提供初始数组，如 \"init: 2, 5, 1, 4\"");
                SegmentTree st = new SegmentTree(init);
                st.attachTracer(t);
                for (String[] p : parse(ops)) {
                    switch (p[0]) {
                        case "query" -> rec.add(p, String.valueOf(st.query(pi(p[1], "左端点"), pi(p[2], "右端点"))));
                        case "update" -> { st.update(pi(p[1], "下标"), pi(p[2], "新值")); rec.add(p, "ok"); }
                        default -> bad(p, "query l r | update i v");
                    }
                }
                rec.finalState("区间和 [0,n-1] 已计算，当前数组=" + java.util.Arrays.toString(init));
            }
            case "fenwick-tree" -> {
                if (init == null || init.length == 0) throw new InvalidInput("init", "树状数组需要 init 字段提供初始数组");
                FenwickTree ft = new FenwickTree(init);
                ft.attachTracer(t);
                for (String[] p : parse(ops)) {
                    switch (p[0]) {
                        case "add" -> { ft.add(pi(p[1], "下标(1-based)"), pi(p[2], "增量")); rec.add(p, "ok"); }
                        case "sum" -> rec.add(p, String.valueOf(ft.rangeSum(pi(p[1], "左端点(1-based)"), pi(p[2], "右端点(1-based)"))));
                        case "prefix" -> rec.add(p, String.valueOf(ft.prefixSum(pi(p[1], "前缀长度(1-based)"))));
                        case "point" -> rec.add(p, String.valueOf(ft.pointValue(pi(p[1], "下标(1-based)"))));
                        default -> bad(p, "add i v | sum l r | prefix i | point i（下标从 1 开始）");
                    }
                }
                rec.finalState("树状数组已建立（1-based）");
            }
            case "bst" -> {
                BST tree = new BST();
                tree.attachTracer(t);
                for (String[] p : parse(ops)) treeOp(tree, p, rec);
                rec.finalState("中序=" + tree.inOrder());
            }
            case "avl" -> {
                AVLTree tree = new AVLTree();
                tree.attachTracer(t);
                for (String[] p : parse(ops)) treeOp(tree, p, rec);
                rec.finalState("中序=" + tree.inOrder() + " 高度=" + tree.height());
            }
            case "red-black-tree" -> {
                RedBlackTree tree = new RedBlackTree();
                tree.attachTracer(t);
                for (String[] p : parse(ops)) treeOp(tree, p, rec);
                rec.finalState("中序=" + tree.inOrder());
            }
            case "btree" -> {
                BTree tree = new BTree(2);
                tree.attachTracer(t);
                for (String[] p : parse(ops)) treeOp(tree, p, rec);
                rec.finalState("中序=" + tree.inOrder());
            }
            default -> throw new InvalidInput("featureId", "功能 " + featureId + " 暂不支持操作序列输入");
        }

        String traceJson = (t.stepCount() == 0 || t.stepCount() > MAX_TRACE_STEPS) ? null : Json.write(t.toTrace());
        return new RunOutcome(traceJson, rec.finalState());
    }

    private void treeOp(Object tree, String[] p, StepRecorder rec) {
        switch (p[0]) {
            case "insert" -> {
                int key = pi(p[1], "键");
                if (tree instanceof BST b) rec.add(p, "inserted=" + b.insert(key));
                else if (tree instanceof AVLTree a) rec.add(p, "inserted=" + a.insert(key));
                else if (tree instanceof RedBlackTree r) rec.add(p, "inserted=" + r.insert(key));
                else if (tree instanceof BTree b) { b.insert(key); rec.add(p, "ok"); }
            }
            case "remove" -> {
                int key = pi(p[1], "键");
                if (tree instanceof BST b) rec.add(p, "removed=" + b.remove(key));
                else if (tree instanceof AVLTree a) rec.add(p, "removed=" + a.remove(key));
                else if (tree instanceof RedBlackTree r) rec.add(p, "removed=" + r.remove(key));
                else if (tree instanceof BTree b) rec.add(p, "removed=" + b.remove(key));
            }
            case "contains" -> {
                int key = pi(p[1], "键");
                if (tree instanceof BST b) rec.add(p, String.valueOf(b.contains(key)));
                else if (tree instanceof AVLTree a) rec.add(p, String.valueOf(a.contains(key)));
                else if (tree instanceof RedBlackTree r) rec.add(p, String.valueOf(r.contains(key)));
                else if (tree instanceof BTree b) rec.add(p, String.valueOf(b.contains(key)));
            }
            case "min" -> { if (tree instanceof BST b) rec.add(p, String.valueOf(b.min())); else if (tree instanceof AVLTree a) rec.add(p, String.valueOf(a.min())); else if (tree instanceof RedBlackTree r) rec.add(p, String.valueOf(r.min())); }
            case "max" -> { if (tree instanceof BST b) rec.add(p, String.valueOf(b.max())); else if (tree instanceof AVLTree a) rec.add(p, String.valueOf(a.max())); else if (tree instanceof RedBlackTree r) rec.add(p, String.valueOf(r.inOrder().get(r.inOrder().size() - 1))); else if (tree instanceof BTree b) rec.add(p, String.valueOf(b.inOrder().get(b.inOrder().size() - 1))); }
            case "height" -> { if (tree instanceof BST b) rec.add(p, String.valueOf(b.height())); else if (tree instanceof AVLTree a) rec.add(p, String.valueOf(a.height())); else if (tree instanceof RedBlackTree r) rec.add(p, String.valueOf(r.height())); }
            case "inorder" -> { if (tree instanceof BST b) rec.add(p, String.valueOf(b.inOrder())); else if (tree instanceof AVLTree a) rec.add(p, String.valueOf(a.inOrder())); else if (tree instanceof RedBlackTree r) rec.add(p, String.valueOf(r.inOrder())); else if (tree instanceof BTree b) rec.add(p, String.valueOf(b.inOrder())); }
            case "size" -> { if (tree instanceof BST b) rec.add(p, String.valueOf(b.size())); else if (tree instanceof AVLTree a) rec.add(p, String.valueOf(a.size())); else if (tree instanceof RedBlackTree r) rec.add(p, String.valueOf(r.size())); else if (tree instanceof BTree b) rec.add(p, String.valueOf(b.size())); }
            default -> bad(p, "insert v | remove v | contains v | min | max | height | inorder | size");
        }
    }

    /* ==================== 输入解析 / 校验工具 ==================== */

    private static final class StepRecorder {
        private final List<Map<String, Object>> opResults;
        private String finalState;

        StepRecorder(List<Map<String, Object>> opResults) { this.opResults = opResults; }

        void add(String[] p, String ret) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("op", String.join(" ", p));
            m.put("ret", ret);
            opResults.add(m);
        }

        void finalState(String s) { this.finalState = s; }

        String finalState() { return finalState; }
    }

    private String kindOf(String featureId) {
        return switch (featureId) {
            case "heap" -> "heap";
            case "monotonic-stack" -> "monostack";
            case "hash-chaining", "hash-open-addressing" -> "hash";
            case "union-find" -> "uf";
            case "arraylist", "singly-linked-list", "doubly-linked-list" -> "linked";
            default -> "tree";
        };
    }

    private String titleOf(String featureId) {
        return FeatureCatalog.find(featureId).map(FeatureCatalog.FeatureDef::name).orElse(featureId);
    }

    private record OpRecord(List<String[]> list) {}

    private List<String[]> parse(List<String> ops) {
        List<String[]> out = new ArrayList<>();
        for (String line : ops) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length == 0 || parts[0].isEmpty()) continue;
            out.add(parts);
        }
        return out;
    }

    private LabResult finish(Map<String, Object> result, String traceJson, long resultNs, long traceNs) {
        result.put("elapsedResultNs", resultNs);
        result.put("elapsedTraceNs", traceNs);
        return new LabResult(Json.write(result), traceJson, resultNs + traceNs);
    }

    private LabResult finishWithTraceObj(Map<String, Object> result, Tracer t, long resultNs) {
        // 图/树类：一次调用同时产出 result 与 trace，无法拆分纯算法耗时，如实标注
        String traceJson = t.stepCount() > MAX_TRACE_STEPS ? null : Json.write(t.toTrace());
        return finish(result, traceJson, resultNs, 0);
    }

    private int[] intArrayField(Map<String, Object> in, String key, int maxLen, String errField) {
        Object v = in.get(key);
        if (v instanceof List<?> list) {
            if (list.size() > maxLen) throw new InvalidInput(errField == null ? key : errField,
                    "数组长度不能超过 " + maxLen + "（当前 " + list.size() + "）");
            int[] a = new int[list.size()];
            for (int i = 0; i < list.size(); i++) a[i] = (int) numOf(list.get(i));
            return a;
        }
        throw new InvalidInput(key, "缺少整数数组字段 " + key);
    }

    private int[] optIntArrayField(Map<String, Object> in, String key, int maxLen) {
        Object v = in.get(key);
        if (!(v instanceof List<?> list)) return null;
        if (list.size() > maxLen) throw new InvalidInput(key, "数组长度不能超过 " + maxLen);
        int[] a = new int[list.size()];
        for (int i = 0; i < list.size(); i++) a[i] = (int) numOf(list.get(i));
        return a;
    }

    @SuppressWarnings("unchecked")
    private List<List<Object>> edgesField(Map<String, Object> in) {
        Object v = in.get("edges");
        if (!(v instanceof List<?> list)) throw new InvalidInput("edges", "缺少边表 edges");
        List<List<Object>> out = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof List<?> e)) throw new InvalidInput("edges", "每条边必须是 [u, v, w] 形式的数组");
            out.add((List<Object>) e);
        }
        return out;
    }

    private List<String> opsField(Map<String, Object> in) {
        Object v = in.get("ops");
        if (!(v instanceof List<?> list) || list.isEmpty()) throw new InvalidInput("ops", "操作序列不能为空");
        if (list.size() > MAX_OPS) throw new InvalidInput("ops", "操作数不能超过 " + MAX_OPS);
        List<String> out = new ArrayList<>();
        for (Object o : list) {
            if (o == null) continue;
            String s = String.valueOf(o).trim();
            if (!s.isEmpty()) out.add(s);
        }
        if (out.isEmpty()) throw new InvalidInput("ops", "操作序列不能为空");
        return out;
    }

    private int intField(Map<String, Object> in, String key, String errField) {
        return (int) longField(in, key, errField);
    }

    private long longField(Map<String, Object> in, String key, String errField) {
        Object v = in.get(key);
        if (v instanceof Number n) return n.longValue();
        throw new InvalidInput(errField == null ? key : errField, "缺少整数字段 " + key);
    }

    private long numOf(Object o) {
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            throw new InvalidInput("input", "无法识别的数值：" + o);
        }
    }

    private boolean boolField(Map<String, Object> in, String key, boolean dft) {
        Object v = in.get(key);
        if (v instanceof Boolean b) return b;
        return dft;
    }

    private String stringField(Map<String, Object> in, String key, int maxLen, String errField) {
        Object v = in.get(key);
        if (!(v instanceof String s)) throw new InvalidInput(errField, "缺少文本字段 " + key);
        if (s.length() > maxLen) throw new InvalidInput(errField, key + " 长度不能超过 " + maxLen);
        return s;
    }

    private Integer optIntField(Map<String, Object> in, String key) {
        Object v = in.get(key);
        if (v instanceof Number n) return n.intValue();
        return null;
    }

    private int requireCapacity(Integer cap, int min, int max) {
        if (cap == null) return Math.min(8, max);
        if (cap < min || cap > max) throw new InvalidInput("capacity", "容量需在 " + min + "~" + max + " 之间");
        return cap;
    }

    private int pi(String s, String what) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new InvalidInput("ops", "操作参数需要整数" + what + "，得到：" + s);
        }
    }

    private Object pv(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return s;
        }
    }

    private String word(String w) {
        if (w == null || !w.matches("[a-z]{1,32}")) {
            throw new InvalidInput("ops", "单词必须是 1~32 个小写字母：" + w);
        }
        return w;
    }

    private void bad(String[] p, String usage) {
        throw new InvalidInput("ops", "无法识别的操作 \"" + String.join(" ", p) + "\"。可用操作：" + usage);
    }

    private List<Object> toList(int[] a) {
        List<Object> out = new ArrayList<>(a.length);
        for (int v : a) out.add(v);
        return out;
    }
}
