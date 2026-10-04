# FEATURE_MATRIX — 功能覆盖表（Phase 0 产出）

> 依据计划 §37：项目开发前必须生成功能覆盖表，确认**现有功能没有在新工作台中被遗漏**。
> 统计口径：功能总数 **44**（与 `trace/manifest.json` 完全一致）；笔记 **22** 篇；练习题 **110**（22 篇 × 5 题，嵌于笔记内）。
> UI 中显示的数量必须来源于本表/功能目录，禁止硬编码。

## 图例

- Java实现：核心类位于 `src/main/java/com/javadslab/core/`（真实可执行）
- Trace：`trace/<id>.json` 已有（44/44，全部可播放）
- 笔记：`docs/notes/` 关联篇目
- 练习：关联笔记的 5 道练习题（附答案）——工作台需结构化后入库
- 实验入口：工作台实验场必须提供的运行入口（Result 模式全部支持；Trace 模式见 Trace 列）
- 源码入口：播放器/详情页"查看 Java 源码"必须指向的文件

## 覆盖矩阵（44 项）

### 线性表（3）

| 功能 id | 名称 | Java实现 | Trace | 笔记 | 练习 | 实验入口 | 源码入口 |
|---|---|---|---|---|---|---|---|
| arraylist | 动态数组 | MyArrayList | ✓(7步) | 01 | ✓5 | ✓ | core/linear/MyArrayList.java |
| singly-linked-list | 单链表 | SinglyLinkedList | ✓(6步) | 01 | ✓5 | ✓ | core/linear/SinglyLinkedList.java |
| doubly-linked-list | 双向链表 | DoublyLinkedList | ✓(5步) | 01 | ✓5 | ✓ | core/linear/DoublyLinkedList.java |

### 栈与队列（4）

| 功能 id | 名称 | Java实现 | Trace | 笔记 | 练习 | 实验入口 | 源码入口 |
|---|---|---|---|---|---|---|---|
| stack | 顺序栈 | ArrayStack | ✓(10步) | 02 | ✓5 | ✓ | core/stackqueue/ArrayStack.java |
| queue | 队列 | ArrayQueue | ✓(6步) | 02 | ✓5 | ✓ | core/stackqueue/ArrayQueue.java |
| circular-queue | 循环队列 | CircularQueue | ✓(7步) | 02 | ✓5 | ✓ | core/stackqueue/CircularQueue.java |
| monotonic-stack | 单调栈 | MonotonicStack | ✓(11步) | 02 | ✓5 | ✓ | core/stackqueue/MonotonicStack.java |

### 哈希（3）

| 功能 id | 名称 | Java实现 | Trace | 笔记 | 练习 | 实验入口 | 源码入口 |
|---|---|---|---|---|---|---|---|
| hash-chaining | 拉链法哈希表 | ChainingHashMap | ✓(9步) | 03 | ✓5 | ✓ | core/hash/ChainingHashMap.java |
| hash-open-addressing | 开放寻址哈希表 | OpenAddressingHashMap | ✓(5步) | 03 | ✓5 | ✓ | core/hash/OpenAddressingHashMap.java |
| union-find | 并查集 | UnionFind | ✓(6步) | 04 | ✓5 | ✓ | core/unionfind/UnionFind.java |

### 树（9）

| 功能 id | 名称 | Java实现 | Trace | 笔记 | 练习 | 实验入口 | 源码入口 |
|---|---|---|---|---|---|---|---|
| heap | 二叉堆 | MyHeap | ✓(13步) | 05 | ✓5 | ✓ | core/heap/MyHeap.java |
| trie | 前缀树 | Trie | ✓(5步) | 06 | ✓5 | ✓ | core/tree/Trie.java |
| segment-tree | 线段树 | SegmentTree | ✓(11步) | 07 | ✓5 | ✓ | core/tree/SegmentTree.java |
| fenwick-tree | 树状数组 | FenwickTree | ✓(14步) | 08 | ✓5 | ✓ | core/tree/FenwickTree.java |
| tree-traversal | 二叉树遍历 | BinaryTrees | ✓(33步) | 09 | ✓5 | ✓ | core/tree/BinaryTrees.java |
| bst | 二叉搜索树 | BST | ✓(8步) | 10 | ✓5 | ✓ | core/tree/BST.java |
| avl | AVL 平衡树 | AVLTree | ✓(10步) | 11 | ✓5 | ✓ | core/tree/AVLTree.java |
| red-black-tree | 红黑树 | RedBlackTree | ✓(8步) | 12 | ✓5 | ✓ | core/tree/RedBlackTree.java |
| btree | B 树 | BTree | ✓(12步) | 13 | ✓5 | ✓ | core/tree/BTree.java |

### 图（8）

| 功能 id | 名称 | Java实现 | Trace | 笔记 | 练习 | 实验入口 | 源码入口 |
|---|---|---|---|---|---|---|---|
| bfs | 广度优先搜索 | GraphSearch | ✓(12步) | 14 | ✓5 | ✓ | core/graph/GraphSearch.java |
| dfs | 深度优先搜索 | GraphSearch | ✓(6步) | 14 | ✓5 | ✓ | core/graph/GraphSearch.java |
| dijkstra | Dijkstra 最短路 | ShortestPath | ✓(13步) | 15 | ✓5 | ✓ | core/graph/ShortestPath.java |
| bellman-ford | Bellman-Ford 负权最短路 | ShortestPath | ✓(4步) | 15 | ✓5 | ✓ | core/graph/ShortestPath.java |
| floyd | Floyd 全源最短路 | ShortestPath | ✓(29步) | 15 | ✓5 | ✓ | core/graph/ShortestPath.java |
| prim | Prim 最小生成树 | MinimumSpanningTree | ✓(6步) | 16 | ✓5 | ✓ | core/graph/MinimumSpanningTree.java |
| kruskal | Kruskal 最小生成树 | MinimumSpanningTree | ✓(5步) | 16 | ✓5 | ✓ | core/graph/MinimumSpanningTree.java |
| topological-sort | 拓扑排序 | TopologicalSorter | ✓(13步) | 17 | ✓5 | ✓ | core/graph/TopologicalSorter.java |

### 排序（11）

| 功能 id | 名称 | Java实现 | Trace | 笔记 | 练习 | 实验入口 | 源码入口 |
|---|---|---|---|---|---|---|---|
| sort-bubble | 冒泡排序 | SortingAlgorithms | ✓(26步) | 19 | ✓5 | ✓ | core/sort/SortingAlgorithms.java |
| sort-insertion | 插入排序 | SortingAlgorithms | ✓(14步) | 19 | ✓5 | ✓ | core/sort/SortingAlgorithms.java |
| sort-selection | 选择排序 | SortingAlgorithms | ✓(14步) | 19 | ✓5 | ✓ | core/sort/SortingAlgorithms.java |
| sort-shell | 希尔排序 | SortingAlgorithms | ✓(25步) | 19 | ✓5 | ✓ | core/sort/SortingAlgorithms.java |
| sort-merge | 归并排序 | MergeSort | ✓(16步) | 19 | ✓5 | ✓ | core/sort/MergeSort.java |
| sort-quick | 快速排序（Lomuto） | TraceableSorts→QuickSort | ✓(26步) | 19/20 | ✓10 | ✓ | core/sort/SortingAlgorithms.java |
| sort-heap | 堆排序 | SortingAlgorithms(基于 MyHeap) | ✓(10步) | 05/19 | ✓10 | ✓ | core/sort/SortingAlgorithms.java |
| sort-counting | 计数排序 | SortingAlgorithms | ✓(12步) | 19 | ✓5 | ✓ | core/sort/SortingAlgorithms.java |
| sort-radix | 基数排序 | SortingAlgorithms | ✓(4步) | 19 | ✓5 | ✓ | core/sort/SortingAlgorithms.java |
| sort-bucket | 桶排序 | SortingAlgorithms | ✓(16步) | 19 | ✓5 | ✓ | core/sort/SortingAlgorithms.java |
| quick-sort-3way | 三路快排 | QuickSortOptimized | ✓(17步) | 20 | ✓5 | ✓ | core/sort/QuickSortOptimized.java |

### 查找与字符串（2）

| 功能 id | 名称 | Java实现 | Trace | 笔记 | 练习 | 实验入口 | 源码入口 |
|---|---|---|---|---|---|---|---|
| binary-search | 二分查找 | BinarySearch | ✓(2步) | 21 | ✓5 | ✓ | core/search/BinarySearch.java |
| kmp | KMP 字符串匹配 | KMP | ✓(14步) | 18 | ✓5 | ✓ | core/search/KMP.java |

### 动态规划（4）

| 功能 id | 名称 | Java实现 | Trace | 笔记 | 练习 | 实验入口 | 源码入口 |
|---|---|---|---|---|---|---|---|
| dp-climb-stairs | 爬楼梯 | DynamicProgramming | ✓(9步) | 22 | ✓5 | ✓ | core/dp/DynamicProgramming.java |
| dp-coin-change | 零钱兑换 | DynamicProgramming | ✓(27步) | 22 | ✓5 | ✓ | core/dp/DynamicProgramming.java |
| dp-lcs | 最长公共子序列 | DynamicProgramming | ✓(44步) | 22 | ✓5 | ✓ | core/dp/DynamicProgramming.java |
| dp-knapsack | 0/1 背包 | DynamicProgramming | ✓(27步) | 22 | ✓5 | ✓ | core/dp/DynamicProgramming.java |

## 汇总统计

| 维度 | 数量 | 说明 |
|---|---|---|
| 功能总数 | **44** | UI 显示"共 N 个算法"必须读取功能目录实时数量 |
| 有 Java 实现 | 44/44 | 全部真实手写实现 |
| 有 Trace | 44/44 | 全部可进 Trace 模式；Result 模式亦全部支持 |
| 有笔记 | 44/44 | 由 22 篇笔记覆盖（多篇功能共享一篇笔记属正常） |
| 有练习 | 44/44 | 由笔记练习题承载（sort-quick / sort-heap 双笔记共 10 题） |
| Trace kind 分布 | array×20 / tree×9 / graph×7 / linked×3 / matrix×3 / hash×2 / uf / heap / monostack 各1 | 播放器已支持全部 9 种渲染 |

## 已知命名对齐问题（功能目录必须解决）

现有三处来源的 id 命名不完全一致，工作台功能目录（单一事实来源）以本表 44 个 trace-id 为准：

1. `SeedData.ALGORITHMS`（lab.db algorithms 表，34 条）：使用 `quick-sort`（≠ trace `sort-quick`）、`bubble-sort`（≠ `sort-bubble`）等；缺 10 个后补 trace 功能 → **迁移时以 trace-id 建立映射，不删旧数据**。
2. `trace/manifest.json`：44 条，与本表一致 ✅（基准）。
3. 笔记文件名：编号式（01~22），功能目录中用 `noteId` 字段关联。

## 非算法类资产（不在 44 项内，但工作台必须承载）

| 资产 | 承载页面 |
|---|---|
| 期末复习图谱 docs/期末复习图谱.md | 复习地图（#/review）内容底稿 |
| 10 条复杂查询 Queries.java | 数据库实验页（#/database） |
| JDBC 基准报告 reports/jdbc-report.md | 项目验证/数据库实验（历史报告） |
| 166 个 Maven 测试 | 项目验证页（#/verify）后台任务 |
| QueryBenchmark 造数流程 | 项目验证页（⚠️ 会重建 lab.db，工作台不得直接调用；数据库实验改用 experiment.db） |
| MistakeBookCli | CLI 保留；网页错题本共用 data/lab.db |
