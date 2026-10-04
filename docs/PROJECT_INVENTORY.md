# PROJECT_INVENTORY — JavaDS-Lab 项目盘点（Phase 0）

> 生成时间：2026-10-05 · 依据《JavaDS-Lab 学习工作台计划（修订版）》第 38 节要求，在改造前对现有资产做全量盘点。
> 本文档是后续工作台开发的"事实来源"基线：**所有复用、迁移、整合决策以本文记录为准，不重新发明。**

---

## 1. 项目概况

| 项 | 现状 |
|---|---|
| 技术栈 | JDK 17 + Maven 3.9.16（tools/ 内置便携版）+ SQLite (xerial 3.46.1.0) + 纯 HTML/CSS/JS 单页播放器 |
| 构建 | `mvn -s tools/maven-settings.xml test` —— **166 个测试全部通过（实测 2026-10-05）** |
| 入口现状 | 3 个 BAT：`启动播放器.bat`（直接开 file:// 播放器）、`错题本.bat`（CLI）、`运行测试.bat`（Maven）——**尚无统一 HTTP 服务入口** |
| 数据库 | `data/lab.db`（约 13 MB）：algorithms 34 / runs 100,000 / steps 4,000 / mistakes 60（来自 QueryBenchmark 造数） |
| 协议 | 无 HTTP 服务；播放器通过 file:// + `web/traces-bundle.js` 内嵌 44 个 trace 运行 |

---

## 2. Java 算法清单（核心实现，`src/main/java/com/javadslab/core/`）

手写实现，禁止使用 java.util 树/堆/排序充当核心逻辑。

| 包 | 类 | 覆盖功能（feature id） |
|---|---|---|
| linear | MyArrayList | arraylist |
| linear | SinglyLinkedList | singly-linked-list |
| linear | DoublyLinkedList | doubly-linked-list |
| stackqueue | ArrayStack | stack |
| stackqueue | ArrayQueue | queue |
| stackqueue | CircularQueue | circular-queue |
| stackqueue | MonotonicStack | monotonic-stack |
| hash | ChainingHashMap | hash-chaining |
| hash | OpenAddressingHashMap | hash-open-addressing |
| unionfind | UnionFind | union-find |
| heap | MyHeap | heap（+ sort-heap 复用） |
| tree | Trie | trie |
| tree | SegmentTree | segment-tree |
| tree | FenwickTree | fenwick-tree |
| tree | BinaryTrees | tree-traversal |
| tree | BST | bst |
| tree | AVLTree | avl |
| tree | RedBlackTree | red-black-tree |
| tree | BTree | btree |
| graph | Graph（结构） | bfs/dfs/dijkstra/floyd/bellman-ford/prim/kruskal/topological-sort 的输入结构 |
| graph | GraphSearch | bfs, dfs |
| graph | ShortestPath | dijkstra, bellman-ford, floyd |
| graph | MinimumSpanningTree | prim, kruskal |
| graph | TopologicalSorter | topological-sort |
| sort | SortingAlgorithms | sort-bubble/insertion/selection/shell/heap/counting/radix/bucket |
| sort | MergeSort | sort-merge |
| sort | QuickSortOptimized | quick-sort-3way |
| search | BinarySearch | binary-search |
| search | KMP | kmp |
| dp | DynamicProgramming | dp-lcs, dp-knapsack, dp-climb-stairs, dp-coin-change |

**Java 侧可执行算法共 44 个功能点（与 Trace 清单一一对应，见 §3）。**

---

## 3. Trace 清单（`trace/`，44 个 JSON + manifest.json）

Trace 统一格式：`meta {kind,title,algorithm,input}` + `steps[] {i, op, args, before, after, hl}`。
`meta.kind` 决定渲染类型，播放器支持 9 种：`array / linked / hash / heap / tree / graph / matrix / uf / monostack`。

| # | trace 文件 | kind | 步数 | 生成器（Traceable*） |
|---|---|---|---|---|
| 1 | arraylist.json | linked | 7 | Traceable* / Tracer |
| 2 | singly-linked-list.json | linked | 6 | 〃 |
| 3 | doubly-linked-list.json | linked | 5 | 〃 |
| 4 | stack.json | array | 10 | 〃 |
| 5 | queue.json | array | 6 | 〃 |
| 6 | circular-queue.json | array | 7 | 〃 |
| 7 | monotonic-stack.json | monostack | 11 | 〃 |
| 8 | hash-chaining.json | hash | 9 | 〃 |
| 9 | hash-open-addressing.json | hash | 5 | 〃 |
| 10 | union-find.json | uf | 6 | 〃 |
| 11 | heap.json | heap | 13 | 〃 |
| 12 | trie.json | tree | 5 | 〃 |
| 13 | segment-tree.json | tree | 11 | 〃 |
| 14 | fenwick-tree.json | tree | 14 | 〃 |
| 15 | bst.json | tree | 8 | TraceableTrees |
| 16 | avl.json | tree | 10 | TraceableTrees |
| 17 | red-black-tree.json | tree | 8 | TraceableTrees |
| 18 | btree.json | tree | 12 | TraceableTrees |
| 19 | tree-traversal.json | tree | 33 | TraceableTrees |
| 20 | bfs.json | graph | 12 | TraceableSearch |
| 21 | dfs.json | graph | 6 | TraceableSearch |
| 22 | dijkstra.json | graph | 13 | TraceableSearch |
| 23 | bellman-ford.json | graph | 4 | TraceableSearch |
| 24 | floyd.json | matrix | 29 | TraceableSearch |
| 25 | prim.json | graph | 6 | TraceableSearch |
| 26 | kruskal.json | graph | 5 | TraceableSearch |
| 27 | topological-sort.json | graph | 13 | TraceableSearch |
| 28 | sort-bubble.json | array | 26 | TraceableSorts |
| 29 | sort-insertion.json | array | 14 | TraceableSorts |
| 30 | sort-selection.json | array | 14 | TraceableSorts |
| 31 | sort-shell.json | array | 25 | TraceableSorts |
| 32 | sort-merge.json | array | 16 | TraceableSorts |
| 33 | sort-quick.json | array | 26 | TraceableSorts |
| 34 | sort-heap.json | array | 10 | TraceableSorts |
| 35 | sort-counting.json | array | 12 | TraceableSorts |
| 36 | sort-radix.json | array | 4 | TraceableSorts |
| 37 | sort-bucket.json | array | 16 | TraceableSorts |
| 38 | quick-sort-3way.json | array | 17 | TraceableQuickSort3Way |
| 39 | binary-search.json | array | 2 | TraceableSearch |
| 40 | kmp.json | array | 14 | TraceableSearch |
| 41 | dp-lcs.json | matrix | 44 | TraceableDP |
| 42 | dp-knapsack.json | matrix | 27 | TraceableDP |
| 43 | dp-climb-stairs.json | array | 9 | TraceableDP |
| 44 | dp-coin-change.json | array | 27 | TraceableDP |

Trace 基础设施：`trace/Json.java`（迷你 JSON 库）、`trace/Tracer.java`（step-mode 记录器）、`trace/Snaps.java`（快照构造）。
重新生成：`java -cp target/classes com.javadslab.app.TraceGenerator .`（同步产出 `web/traces-bundle.js` 与 `trace/manifest.json`）。

---

## 4. 学习笔记清单（`docs/notes/`，22 篇）

每篇固定结构：源码指引 → 原理图解 → 速查表 → 核心代码 → 复杂度推导 → **易错点×5** → **练习题×5（附答案）**。
**练习资源 = 22 篇 × 5 题 = 110 题（目前嵌在 Markdown 文本中，未结构化）**。

| 笔记 | 主题 | 关联 feature |
|---|---|---|
| 01-linear-lists.md | 线性表 | arraylist, singly-linked-list, doubly-linked-list |
| 02-stack-queue.md | 栈与队列 | stack, queue, circular-queue, monotonic-stack |
| 03-hash.md | 哈希表 | hash-chaining, hash-open-addressing |
| 04-union-find.md | 并查集 | union-find |
| 05-heap.md | 堆 | heap, sort-heap |
| 06-trie.md | 前缀树 | trie |
| 07-segment-tree.md | 线段树 | segment-tree |
| 08-fenwick-tree.md | 树状数组 | fenwick-tree |
| 09-binary-tree-traversal.md | 二叉树遍历 | tree-traversal |
| 10-bst.md | 二叉搜索树 | bst |
| 11-avl.md | AVL 树 | avl |
| 12-red-black-tree.md | 红黑树 | red-black-tree |
| 13-btree.md | B 树 | btree |
| 14-graph-bfs-dfs.md | 图遍历 | bfs, dfs |
| 15-shortest-path.md | 最短路 | dijkstra, bellman-ford, floyd |
| 16-mst.md | 最小生成树 | prim, kruskal |
| 17-topological-sort.md | 拓扑排序 | topological-sort |
| 18-kmp.md | 字符串匹配 | kmp |
| 19-sorting.md | 十种排序 | sort-bubble/insertion/selection/shell/merge/quick/heap/counting/radix/bucket |
| 20-quicksort-optimizations.md | 快排优化 | quick-sort-3way |
| 21-binary-search.md | 二分查找 | binary-search |
| 22-dp-15.md | 动态规划 15 讲 | dp-lcs, dp-knapsack, dp-climb-stairs, dp-coin-change |

另有：`docs/期末复习图谱.md`（按依赖分层，是"复习地图"页的内容底稿）、`docs/使用说明.md`。

---

## 5. SQLite Schema（`data/lab.db`，由 `sql/schema.sql` 定义）

现有 4 张业务表（外键级联删除 + CHECK 约束 + 全查询路径索引）：

| 表 | 字段 | 用途 |
|---|---|---|
| algorithms | id, name(UNIQUE), category(CHECK 9 类), difficulty(1-5), description | 算法字典（已种子 34 条，注意命名与 trace id 不完全一致，如 "quick-sort" vs trace "sort-quick"） |
| runs | id, algorithm_id FK, ran_at, duration_ms, input_size, passed(0/1), score | 运行/练习记录 |
| steps | id, run_id FK, step_no, op, detail | 运行步骤审计（**注意：此表存的 op/detail 与网页 Trace JSON 格式不同，非同一套数据**） |
| mistakes | id, algorithm_id FK, question, wrong_answer, reason, redo_count, created_at | 错题本（CLI 正在使用） |

`data/lab.db` 当前内容：algorithms 34 / runs 100,000 / steps 4,000 / mistakes 60（QueryBenchmark 造数产物）。
工作台需要**兼容迁移**新增表：learning_state、practice_attempt、practice_draft、experiment_run、mistake_review（计划 §20）。

---

## 6. CLI 功能（`MistakeBookCli`，入口 `com.javadslab.app.MistakeBookCli [db]`）

| 命令 | 功能 |
|---|---|
| algo | 列出算法字典 |
| list [算法名] | 列出错题（可按算法过滤） |
| add | 交互式添加错题（算法→题目→错误答案→原因） |
| redo \<id\> | 重做错题（累计 redo_count） |
| del \<id\> | 删除错题 |
| weak | 薄弱项（错题多/重做少的算法） |
| help / quit | 帮助 / 退出 |

CLI 走 `Dao`（PreparedStatement + 事务），网页版错题本**必须共用同一 Dao 与同一 `data/lab.db`**。

---

## 7. 数据库实验（`QueryBenchmark` + `persist/Queries.java`）

- **10 条复杂查询**：Q1 各算法运行统计 / Q2 每算法最快3次 / Q3 按日累计运行量 / Q4 慢于自身均值的运行 / Q5 难度维度通过率 / Q6 从未失败的算法 / Q7 算法耗时排名 / Q8 知识点失败率与错题 / Q9 每类最快2算法 / Q10 每日练习趋势（Q3、Q10 带参数）。
- `QueryBenchmark`：删除并重建 `data/lab.db` → 造 10 万 runs → EXPLAIN QUERY PLAN + 无索引/有索引对比 → 输出 `reports/jdbc-report.md`。
- ⚠️ **破坏性**：QueryBenchmark 会删除重建 lab.db —— 工作台的"数据库实验"页必须改用独立 `data/experiment.db`（计划 §22），绝不能在网页触发此流程对 lab.db 造数。
- 现有报告：`reports/jdbc-report.md`（历史实测报告，10 万条数据）。

---

## 8. 测试清单（`src/test/`，16 个测试类 / 166 个测试，全部通过）

| 测试类 | 数量 | 覆盖 |
|---|---|---|
| MistakeBookCliTest | 5 | CLI 全流程（脚本化注入 IO） |
| DynamicProgrammingTest | 17 | DP 各方法 |
| GraphAlgorithmsTest | 19 | bfs/dfs/最短路/MST/拓扑 |
| HashTableTest | 11 | 两种哈希表 |
| MyHeapTest | 7 | 堆 |
| DoublyLinkedListTest / MyArrayListTest / SinglyLinkedListTest / SmokeTest | 30 | 线性表 |
| SearchTest | 11 | 二分 + KMP |
| SortTest | 8 | 排序（含与手写归并对拍） |
| StackQueueTest | 13 | 栈队列 |
| PrefixStructuresTest / SearchTreesTest | 29 | Trie/线段树/树状数组 + BST/AVL/RB/B 树（带不变量校验） |
| UnionFindTest | 7 | 并查集 |
| PersistIntegrationTest | 9 | SQLite 建库→10 万造数→10 查询→错题全流程 |

特点：随机对拍（2~5 万次操作 vs JDK 参照）、树不变量校验、集成测试。

---

## 9. 现有播放器（`web/player.html`，514 行单文件）

- **已是功能完备的 SVG 播放器**：上一步/下一步/播放/暂停/回退/速度 0.5×~4×/进度滑杆跳步/缩略步骤导航/键盘 ←→ 空格/JSON 导入（从本地文件）。
- 数据来源：file:// 下自动加载 `web/traces-bundle.js`（内嵌全部 44 个 trace）；或 http.server 下按 manifest 下拉选择。
- 渲染 9 种 kind：array / linked / hash / heap / tree / graph / matrix / uf / monostack。
- ⚠️ 尚无：JSON 导出按钮、当前步骤"操作名+参数+解释"结构化说明区、与学习闭环的联动。
- 整合策略（计划 §13）：**复用播放器核心逻辑迁入工作台，作为实验场 Trace 模式的回放组件**；旧 `启动播放器.bat` 转为转发/迁移提示。

---

## 10. 启动脚本现状

| 脚本 | 行为 | 工作台处置 |
|---|---|---|
| 启动播放器.bat | 直接 start web/player.html（file://） | 保留但改为转发/提示（不得继续维护第二套入口） |
| 错题本.bat | java -cp 跑 CLI | 保留（CLI 与网页共用数据库） |
| 运行测试.bat | mvn test + pause | 保留 |
| **启动工作台.bat** | **（不存在）** | **Phase 1 新建：定位目录→查 JDK→查构建→启动 HTTP 服务→health→开浏览器** |

环境要点（来自 memory 与脚本实测）：便携 JDK17/Maven 在 tools/；Maven 需 `-s tools/maven-settings.xml`；SQLite 主类运行要拼 Windows classpath（sqlite-jdbc + slf4j-api 两个 jar，位于 `~/.m2/repository`）。

---

## 11. 缺口清单（工作台需要新建的能力）

1. **本地 HTTP 服务**（JDK 内置 com.sun.net.httpserver）：静态资源 + /api/*，仅监听 127.0.0.1。
2. **统一功能目录**（单一事实来源 FeatureDefinition：id/name/category/noteId/traceSupported/javaClass/sourcePath…）——现散落在 SeedData.ALGORITHMS（34 条，命名与 trace 不齐）、trace/manifest.json（44 条）、笔记文件名三处，需合并对齐为 44 个 feature。
3. **Algorithm Runner**：服务端解析输入→调用真实 Java 方法→返回 result/trace（含输入校验、规模上限）。
4. **新数据表迁移**：learning_state / practice_attempt / practice_draft / experiment_run / mistake_review。
5. **练习题结构化**：110 题目前是 Markdown 文本，需建立 practices 数据（可从笔记提取为结构化 JSON，来源标注笔记）。
6. **后台任务系统**：Maven 测试 / 数据库造数 / 完整验证不阻塞 HTTP。
7. **安全边界**：输入服务端校验、路径白名单（src/notes/trace/data/reports）、HTML 转义、SQL 参数绑定。
8. **健康检查 + 日志**：/api/health；logs/ 目录。
