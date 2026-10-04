package com.javadslab.workbench;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 统一功能目录：整个工作台的"单一事实来源"（计划 §4）。
 * 所有页面（首页/练习/实验/笔记/错题/复习地图）都从这里获取知识点信息，
 * 禁止在页面里另行维护第二份算法名单。
 *
 * id 以 trace/manifest.json 的 44 个 id 为基准（FEATURE_MATRIX.md）；
 * algoName 是该功能在 SQLite algorithms 表中的名称（老数据命名兼容映射）。
 */
public final class FeatureCatalog {

    private FeatureCatalog() {}

    public record FeatureDef(
            String id,
            String name,
            String category,
            String description,
            String javaClass,
            String sourcePath,
            String noteId,
            boolean traceSupported,
            String inputType,
            String sampleInput,
            List<String> prerequisites,
            String algoName) {

        public String practiceIdBase() { return noteId; }
    }

    /** 分类展示顺序（计划 §8 要求的覆盖面）。 */
    public static final List<String> CATEGORIES = List.of(
            "线性表", "栈与队列", "哈希", "树", "图", "排序", "查找", "字符串", "动态规划");

    /** 输入类型（驱动实验场表单与服务端校验）。 */
    public static final String IN_ARRAY = "array";               // 整数数组
    public static final String IN_ARRAY_TARGET = "array-target"; // 升序数组 + 目标值
    public static final String IN_TEXT_PATTERN = "text-pattern"; // 文本 + 模式串
    public static final String IN_TREE_ARRAY = "tree-array";     // 层序整数（建树遍历）
    public static final String IN_GRAPH = "graph";               // n + 边表 + (起点)
    public static final String IN_GRAPH_NEG = "graph-neg";       // n + 可含负权边表 + 起点
    public static final String IN_DP_N = "dp-n";                 // 阶梯数 n
    public static final String IN_COINS = "coins-amount";        // 币值数组 + 金额
    public static final String IN_TWO_STRINGS = "two-strings";   // 两个字符串
    public static final String IN_KNAPSACK = "knapsack";         // 重量 + 价值 + 容量
    public static final String IN_OPS = "ops";                   // 结构操作序列（+ 可选容量/初始数组）

    private static final List<FeatureDef> ALL = List.of(
            /* ---------------- 线性表 ---------------- */
            f("arraylist", "动态数组", "线性表",
                    "手写动态数组：尾插均摊 O(1)、按下标增删改查、扩容迁移。",
                    "com.javadslab.core.linear.MyArrayList", "core/linear/MyArrayList.java",
                    "01-linear-lists", true, IN_OPS, "add 10\nadd 20\nadd 30\nadd 1 15\nset 0 5\nremove 3",
                    List.of(), "arraylist"),
            f("singly-linked-list", "单链表", "线性表",
                    "单向链表：头/尾插、按下标插入删除、遍历查找。",
                    "com.javadslab.core.linear.SinglyLinkedList", "core/linear/SinglyLinkedList.java",
                    "01-linear-lists", true, IN_OPS, "addLast 1\naddLast 2\naddFirst 0\naddLast 3\nremoveAt 2\nadd 2 99",
                    List.of("arraylist"), "singly-linked-list"),
            f("doubly-linked-list", "双向链表", "线性表",
                    "带哨兵的双向链表：首尾 O(1)、前驱后继双向遍历。",
                    "com.javadslab.core.linear.DoublyLinkedList", "core/linear/DoublyLinkedList.java",
                    "01-linear-lists", true, IN_OPS, "addLast 1\naddLast 2\naddFirst -1\nremoveLast\nadd 1 8",
                    List.of("singly-linked-list"), "doubly-linked-list"),

            /* ---------------- 栈与队列 ---------------- */
            f("stack", "顺序栈", "栈与队列",
                    "数组实现的栈：push/pop/peek，后进先出。",
                    "com.javadslab.core.stackqueue.ArrayStack", "core/stackqueue/ArrayStack.java",
                    "02-stack-queue", true, IN_OPS, "push a\npush b\npush c\npop\npush d",
                    List.of("arraylist"), "stack"),
            f("queue", "队列", "栈与队列",
                    "数组实现的队列：offer/poll，先进先出。",
                    "com.javadslab.core.stackqueue.ArrayQueue", "core/stackqueue/ArrayQueue.java",
                    "02-stack-queue", true, IN_OPS, "offer 1\noffer 2\npoll\noffer 3\npoll\noffer 4",
                    List.of("stack"), "queue"),
            f("circular-queue", "循环队列", "栈与队列",
                    "定容循环队列：head/tail 回绕，判满判空。",
                    "com.javadslab.core.stackqueue.CircularQueue", "core/stackqueue/CircularQueue.java",
                    "02-stack-queue", true, IN_OPS, "offer 1\noffer 2\noffer 3\npoll\noffer 4\npoll\noffer 5",
                    List.of("queue"), "circular-queue"),
            f("monotonic-stack", "单调栈", "栈与队列",
                    "单调栈求下一个更大元素：一遍扫描 O(n)。",
                    "com.javadslab.core.stackqueue.MonotonicStack", "core/stackqueue/MonotonicStack.java",
                    "02-stack-queue", true, IN_ARRAY, "2, 1, 5, 6, 2, 3",
                    List.of("stack"), "monotonic-stack"),

            /* ---------------- 哈希 ---------------- */
            f("hash-chaining", "拉链法哈希表", "哈希",
                    "拉链法：哈希函数、冲突链、扩容。",
                    "com.javadslab.core.hash.ChainingHashMap", "core/hash/ChainingHashMap.java",
                    "03-hash", true, IN_OPS, "capacity 4\nput 3 v1\nput 6 v2\nput 9 v3\nput 12 v4\nremove 9\nput 100 new",
                    List.of("arraylist", "singly-linked-list"), "hash-chaining"),
            f("hash-open-addressing", "开放寻址哈希表", "哈希",
                    "开放寻址 + 线性探测 + 墓碑标记删除。",
                    "com.javadslab.core.hash.OpenAddressingHashMap", "core/hash/OpenAddressingHashMap.java",
                    "03-hash", true, IN_OPS, "put 10 a\nput 18 b\nput 26 c\nremove 18\nput 34 d\nget 26",
                    List.of("arraylist"), "hash-open-addressing"),
            f("union-find", "并查集", "哈希",
                    "路径压缩 + 按大小合并，近似 O(α(n))。",
                    "com.javadslab.core.unionfind.UnionFind", "core/unionfind/UnionFind.java",
                    "04-union-find", true, IN_OPS, "n 8\nunion 0 1\nunion 1 2\nunion 3 4\nunion 2 4\nconnected 1 4\nfind 1",
                    List.of("arraylist"), "union-find"),

            /* ---------------- 树 ---------------- */
            f("heap", "二叉堆", "树",
                    "小顶堆：push 上浮 / pop 下沉，O(log n)。",
                    "com.javadslab.core.heap.MyHeap", "core/heap/MyHeap.java",
                    "05-heap", true, IN_OPS, "push 8\npush 3\npush 5\npush 1\npush 9\npop\npop",
                    List.of("arraylist"), "heap"),
            f("trie", "前缀树", "树",
                    "26 叉 Trie：插入/查找/前缀统计/删除。",
                    "com.javadslab.core.tree.Trie", "core/tree/Trie.java",
                    "06-trie", true, IN_OPS, "insert cat\ninsert car\ninsert card\ninsert dog\nstartsWith ca\ndelete car\nsearch car",
                    List.of("hash-chaining"), "trie"),
            f("segment-tree", "线段树", "树",
                    "区间和线段树：单点改、区间查，O(log n)。",
                    "com.javadslab.core.tree.SegmentTree", "core/tree/SegmentTree.java",
                    "07-segment-tree", true, IN_OPS, "init 2, 5, 1, 4, 9, 3\nquery 1 4\nupdate 3 10\nquery 0 5",
                    List.of("arraylist"), "segment-tree"),
            f("fenwick-tree", "树状数组", "树",
                    "lowbit 跳跃：单点加、前缀和，代码量极小。",
                    "com.javadslab.core.tree.FenwickTree", "core/tree/FenwickTree.java",
                    "08-fenwick-tree", true, IN_OPS, "init 2, 5, 1, 4, 9, 3\nadd 3 5\nsum 1 4\nsum 2 6",
                    List.of("segment-tree"), "fenwick-tree"),
            f("tree-traversal", "二叉树遍历", "树",
                    "前/中/后/层序 8 种写法（递归+迭代）。",
                    "com.javadslab.core.tree.BinaryTrees", "core/tree/BinaryTrees.java",
                    "09-binary-tree-traversal", true, IN_TREE_ARRAY, "1, 2, 3, 4, 5, 6, 7",
                    List.of("singly-linked-list"), "tree-traversal"),
            f("bst", "二叉搜索树", "树",
                    "BST：查找/插入/删除（含双孩后继顶替）。",
                    "com.javadslab.core.tree.BST", "core/tree/BST.java",
                    "10-bst", true, IN_OPS, "insert 50\ninsert 30\ninsert 70\ninsert 20\ninsert 40\ninsert 60\ninsert 80\nremove 30\ncontains 40",
                    List.of("tree-traversal", "binary-search"), "bst"),
            f("avl", "AVL 平衡树", "树",
                    "AVL：高度平衡因子、LL/RR/LR/RL 旋转。",
                    "com.javadslab.core.tree.AVLTree", "core/tree/AVLTree.java",
                    "11-avl", true, IN_OPS, "insert 30\ninsert 20\ninsert 10\ninsert 40\ninsert 50\ninsert 25",
                    List.of("bst"), "avl"),
            f("red-black-tree", "红黑树", "树",
                    "红黑树：变色与旋转维持五性质。",
                    "com.javadslab.core.tree.RedBlackTree", "core/tree/RedBlackTree.java",
                    "12-red-black-tree", true, IN_OPS, "insert 10\ninsert 20\ninsert 30\ninsert 15\ninsert 25\ninsert 5\nremove 20",
                    List.of("bst"), "red-black-tree"),
            f("btree", "B 树", "树",
                    "B 树（t=2）：节点分裂与借位合并。",
                    "com.javadslab.core.tree.BTree", "core/tree/BTree.java",
                    "13-btree", true, IN_OPS, "insert 10\ninsert 20\ninsert 30\ninsert 40\ninsert 50\ninsert 60\ninsert 70\nremove 20",
                    List.of("bst"), "btree"),

            /* ---------------- 图 ---------------- */
            f("bfs", "广度优先搜索", "图",
                    "队列驱动逐层扩展，求最短跳数。",
                    "com.javadslab.core.graph.GraphSearch", "core/graph/GraphSearch.java",
                    "14-graph-bfs-dfs", true, IN_GRAPH, "n 6\ndirected true\nstart 0\nedges\n0 1 4\n0 2 2\n1 2 1\n1 3 5\n2 3 8\n2 4 10\n3 5 3\n4 5 4\n3 4 2",
                    List.of("tree-traversal", "queue"), "bfs"),
            f("dfs", "深度优先搜索", "图",
                    "递归/迭代深入到底再回溯。",
                    "com.javadslab.core.graph.GraphSearch", "core/graph/GraphSearch.java",
                    "14-graph-bfs-dfs", true, IN_GRAPH, "n 6\ndirected true\nstart 0\nedges\n0 1 4\n0 2 2\n1 2 1\n1 3 5\n2 3 8\n2 4 10\n3 5 3\n4 5 4\n3 4 2",
                    List.of("tree-traversal", "stack"), "dfs"),
            f("dijkstra", "Dijkstra 最短路", "图",
                    "堆优化单源最短路（非负权）。",
                    "com.javadslab.core.graph.ShortestPath", "core/graph/ShortestPath.java",
                    "15-shortest-path", true, IN_GRAPH, "n 6\ndirected true\nstart 0\nedges\n0 1 4\n0 2 2\n1 2 1\n1 3 5\n2 3 8\n2 4 10\n3 5 3\n4 5 4\n3 4 2",
                    List.of("bfs", "heap"), "dijkstra"),
            f("bellman-ford", "Bellman-Ford", "图",
                    "逐轮松弛可处理负权边。",
                    "com.javadslab.core.graph.ShortestPath", "core/graph/ShortestPath.java",
                    "15-shortest-path", true, IN_GRAPH_NEG, "n 5\ndirected true\nstart 0\nedges\n0 1 4\n0 2 5\n1 3 -3\n2 3 4\n3 4 2",
                    List.of("dijkstra"), "bellman-ford"),
            f("floyd", "Floyd 全源最短路", "图",
                    "动态规划求全源最短路，三重循环。",
                    "com.javadslab.core.graph.ShortestPath", "core/graph/ShortestPath.java",
                    "15-shortest-path", true, IN_GRAPH, "n 6\ndirected false\nedges\n0 1 7\n0 2 9\n0 5 14\n1 2 10\n1 3 15\n2 3 11\n2 5 2\n3 4 6\n4 5 9",
                    List.of("bellman-ford"), "floyd"),
            f("prim", "Prim 最小生成树", "图",
                    "点扩张 + 堆选最小横切边。",
                    "com.javadslab.core.graph.MinimumSpanningTree", "core/graph/MinimumSpanningTree.java",
                    "16-mst", true, IN_GRAPH, "n 6\ndirected false\nstart 0\nedges\n0 1 7\n0 2 9\n0 5 14\n1 2 10\n1 3 15\n2 3 11\n2 5 2\n3 4 6\n4 5 9",
                    List.of("bfs", "heap"), "prim"),
            f("kruskal", "Kruskal 最小生成树", "图",
                    "边排序 + 并查集判环。",
                    "com.javadslab.core.graph.MinimumSpanningTree", "core/graph/MinimumSpanningTree.java",
                    "16-mst", true, IN_GRAPH, "n 6\ndirected false\nedges\n0 1 7\n0 2 9\n0 5 14\n1 2 10\n1 3 15\n2 3 11\n2 5 2\n3 4 6\n4 5 9",
                    List.of("union-find", "sort-merge"), "kruskal"),
            f("topological-sort", "拓扑排序", "图",
                    "Kahn 入度法（BFS）处理 DAG。",
                    "com.javadslab.core.graph.TopologicalSorter", "core/graph/TopologicalSorter.java",
                    "17-topological-sort", true, IN_GRAPH, "n 7\ndirected true\nedges\n0 1 1\n0 2 1\n1 3 1\n2 3 1\n3 4 1\n2 5 1\n5 6 1",
                    List.of("bfs", "dfs"), "topological-sort"),

            /* ---------------- 排序 ---------------- */
            f("sort-bubble", "冒泡排序", "排序",
                    "相邻比较交换，提前退出优化。",
                    "com.javadslab.core.sort.SortingAlgorithms", "core/sort/SortingAlgorithms.java",
                    "19-sorting", true, IN_ARRAY, "5, 2, 9, 1, 7, 3",
                    List.of("arraylist"), "bubble-sort"),
            f("sort-insertion", "插入排序", "排序",
                    "有序区右移插入，近乎有序时 O(n)。",
                    "com.javadslab.core.sort.SortingAlgorithms", "core/sort/SortingAlgorithms.java",
                    "19-sorting", true, IN_ARRAY, "6, 3, 8, 1, 5",
                    List.of("sort-bubble"), "sort-insertion"),
            f("sort-selection", "选择排序", "排序",
                    "每轮选最值放前，交换次数最少。",
                    "com.javadslab.core.sort.SortingAlgorithms", "core/sort/SortingAlgorithms.java",
                    "19-sorting", true, IN_ARRAY, "7, 2, 5, 3, 8",
                    List.of("sort-bubble"), "sort-selection"),
            f("sort-shell", "希尔排序", "排序",
                    "递减增量分组插入排序。",
                    "com.javadslab.core.sort.SortingAlgorithms", "core/sort/SortingAlgorithms.java",
                    "19-sorting", true, IN_ARRAY, "9, 4, 7, 1, 8, 2, 6",
                    List.of("sort-insertion"), "sort-shell"),
            f("sort-merge", "归并排序", "排序",
                    "二分递归 + 线性合并，稳定 O(n log n)。",
                    "com.javadslab.core.sort.MergeSort", "core/sort/MergeSort.java",
                    "19-sorting", true, IN_ARRAY, "5, 2, 8, 1, 6",
                    List.of("sort-insertion"), "merge-sort"),
            f("sort-quick", "快速排序", "排序",
                    "Lomuto 分区快排，平均 O(n log n)。",
                    "com.javadslab.core.sort.SortingAlgorithms", "core/sort/SortingAlgorithms.java",
                    "19-sorting", true, IN_ARRAY, "8, 3, 5, 2, 9, 1, 6",
                    List.of("sort-merge"), "quick-sort"),
            f("sort-heap", "堆排序", "排序",
                    "建堆 + 逐个弹出，原地 O(n log n)。",
                    "com.javadslab.core.sort.SortingAlgorithms", "core/sort/SortingAlgorithms.java",
                    "19-sorting", true, IN_ARRAY, "4, 10, 3, 5, 1",
                    List.of("heap"), "sort-heap"),
            f("sort-counting", "计数排序", "排序",
                    "非负小值域桶计数，O(n+k)。",
                    "com.javadslab.core.sort.SortingAlgorithms", "core/sort/SortingAlgorithms.java",
                    "19-sorting", true, IN_ARRAY, "4, 1, 3, 4, 0, 2",
                    List.of("sort-bubble"), "sort-counting"),
            f("sort-radix", "基数排序", "排序",
                    "LSD 按位多轮稳定排序。",
                    "com.javadslab.core.sort.SortingAlgorithms", "core/sort/SortingAlgorithms.java",
                    "19-sorting", true, IN_ARRAY, "170, 45, 75, 90, 802, 24",
                    List.of("sort-counting"), "radix-sort"),
            f("sort-bucket", "桶排序", "排序",
                    "值域均匀分桶 + 桶内插入排序。",
                    "com.javadslab.core.sort.SortingAlgorithms", "core/sort/SortingAlgorithms.java",
                    "19-sorting", true, IN_ARRAY, "29, 25, 3, 49, 9, 37, 21, 43",
                    List.of("sort-counting"), "sort-bucket"),
            f("quick-sort-3way", "三路快排", "排序",
                    "三分区处理大量重复元素。",
                    "com.javadslab.core.sort.QuickSortOptimized", "core/sort/QuickSortOptimized.java",
                    "20-quicksort-optimizations", true, IN_ARRAY, "4, 2, 4, 1, 4, 3, 2, 4",
                    List.of("sort-quick"), "quick-sort-3way"),

            /* ---------------- 查找 / 字符串 ---------------- */
            f("binary-search", "二分查找", "查找",
                    "升序有序数组折半查找，O(log n)。",
                    "com.javadslab.core.search.BinarySearch", "core/search/BinarySearch.java",
                    "21-binary-search", true, IN_ARRAY_TARGET, "array: 1, 3, 5, 7, 9, 11, 13, 15\ntarget: 7",
                    List.of("arraylist"), "binary-search"),
            f("kmp", "KMP 字符串匹配", "字符串",
                    "next 数组失配跳转，O(n+m)。",
                    "com.javadslab.core.search.KMP", "core/search/KMP.java",
                    "18-kmp", true, IN_TEXT_PATTERN, "text: ababcababd\npattern: ababd",
                    List.of("singly-linked-list"), "kmp"),

            /* ---------------- 动态规划 ---------------- */
            f("dp-climb-stairs", "爬楼梯", "动态规划",
                    "入门 DP：斐波那契递推。",
                    "com.javadslab.core.dp.DynamicProgramming", "core/dp/DynamicProgramming.java",
                    "22-dp-15", true, IN_DP_N, "n: 8",
                    List.of(), "dp-climb-stairs"),
            f("dp-coin-change", "零钱兑换", "动态规划",
                    "完全背包型：最少硬币数。",
                    "com.javadslab.core.dp.DynamicProgramming", "core/dp/DynamicProgramming.java",
                    "22-dp-15", true, IN_COINS, "coins: 1, 5, 6\namount: 11",
                    List.of("dp-climb-stairs"), "dp-coin-change"),
            f("dp-lcs", "最长公共子序列", "动态规划",
                    "二维 DP 表 + 回溯构造。",
                    "com.javadslab.core.dp.DynamicProgramming", "core/dp/DynamicProgramming.java",
                    "22-dp-15", true, IN_TWO_STRINGS, "s1: ABCBDAB\ns2: BDCABA",
                    List.of("dp-climb-stairs"), "dp-lcs"),
            f("dp-knapsack", "0/1 背包", "动态规划",
                    "容量维度 DP：价值最大化。",
                    "com.javadslab.core.dp.DynamicProgramming", "core/dp/DynamicProgramming.java",
                    "22-dp-15", true, IN_KNAPSACK, "weights: 2, 3, 4, 5\nvalues: 3, 4, 5, 6\ncapacity: 8",
                    List.of("dp-coin-change"), "dp-knapsack"));

    private static FeatureDef f(String id, String name, String category, String description,
                                String javaClass, String sourcePath, String noteId,
                                boolean traceSupported, String inputType, String sampleInput,
                                List<String> prerequisites, String algoName) {
        return new FeatureDef(id, name, category, description, javaClass, sourcePath,
                noteId, traceSupported, inputType, sampleInput, List.copyOf(prerequisites), algoName);
    }

    private static final Map<String, FeatureDef> BY_ID = byId();

    private static Map<String, FeatureDef> byId() {
        Map<String, FeatureDef> m = new LinkedHashMap<>();
        for (FeatureDef d : ALL) m.put(d.id(), d);
        return m;
    }

    public static List<FeatureDef> all() {
        return ALL;
    }

    public static int count() {
        return ALL.size();
    }

    public static Optional<FeatureDef> find(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** 某分类下的功能（保持目录顺序）。 */
    public static List<FeatureDef> byCategory(String category) {
        List<FeatureDef> out = new ArrayList<>();
        for (FeatureDef d : ALL) if (d.category().equals(category)) out.add(d);
        return out;
    }

    /** 某篇笔记覆盖的全部功能。 */
    public static List<FeatureDef> byNote(String noteId) {
        List<FeatureDef> out = new ArrayList<>();
        for (FeatureDef d : ALL) if (d.noteId().equals(noteId)) out.add(d);
        return out;
    }

    /** 引用了某功能作为前置的功能（复习地图反向边）。 */
    public static List<FeatureDef> dependents(String id) {
        List<FeatureDef> out = new ArrayList<>();
        for (FeatureDef d : ALL) if (d.prerequisites().contains(id)) out.add(d);
        return out;
    }

    /** 功能全集的浅拷贝（防止调用方修改目录）。 */
    public static Map<String, Object> toMap(FeatureDef d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.id());
        m.put("name", d.name());
        m.put("category", d.category());
        m.put("description", d.description());
        m.put("javaClass", d.javaClass());
        m.put("sourcePath", d.sourcePath());
        m.put("noteId", d.noteId());
        m.put("traceSupported", d.traceSupported());
        m.put("inputType", d.inputType());
        m.put("sampleInput", d.sampleInput());
        m.put("prerequisites", d.prerequisites());
        m.put("algoName", d.algoName());
        return m;
    }
}
