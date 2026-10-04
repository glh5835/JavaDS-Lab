# TEST_REPORT — 学习工作台测试记录

> 测试时间：2026-10-05。环境：Windows / 便携 JDK 17.0.20.1 / Maven 3.9.16 / Chromium（应用内浏览器）。

## 1. Java 单元测试（Maven 全量）

```
mvn -s tools/maven-settings.xml test
Tests run: 181, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- 原有 166 个测试（16 个测试类：容器对拍 / 树不变量 / 图算法 / DP / 搜索 / 排序 / SQLite 集成）全部通过；
- 新增 `WorkbenchApiTest` 15 个 API 集成测试（随机端口启动真实服务），覆盖计划 §45 全部要求：

| 测试 | 覆盖点 |
|---|---|
| healthOk | 健康检查（§34）：status/database/version/features=44/practices=110 |
| featureCatalogComplete | 功能目录完整性 + 未知功能 404 |
| runSortNormalWithTrace | 正常参数：真实执行 + Trace 生成 + 结果正确 |
| runMissingFeatureId | 空参数：404 NOT_FOUND |
| runBinarySearchRequiresAscending | 非法参数：升序要求明确报错且不自动排序（§10） |
| runEmptyOpsRejected | 空操作序列 400 |
| runOversizeArrayRejected | 规模限制：20001 元素明确报错（§36） |
| runInvalidJsonRejected | 非法 JSON 400 |
| graphEdgeValidation | 边界值：越界边/负权边/Dijkstra 非负校验/Bellman-Ford 负权可用 |
| runHistoryAndTrace | 运行历史 + Trace 格式（steps/before/hl）+ 404 |
| learningStateRoundtrip | 学习状态设置/读取/非法值拒绝 |
| practicesListWithoutAnswerAndDetailWithAnswer | 列表不泄漏答案（§6.1）+ 详情含答案 + 草稿读写 |
| mistakeFullCycle | 错题全闭环：新增→重做失败→重做成功→success_count 只计成功→编辑→删除（§15.3） |
| databaseQueryAndGuards | Q1 真实执行+EXPLAIN；坏 queryId 400；ATTACH 越界 SQL 拒绝（§28） |
| sourcePathWhitelist | 源码白名单：合法 .java 可读；路径穿越/越界 403（§28/§29） |

## 2. 后台任务实测（项目验证页）

| 任务 | 结果 |
|---|---|
| full-verify | Maven 全量测试通过 + 44 个 Trace 全量校验 **44 通过 / 0 失败**，报告写入 docs/TRACE_VALIDATION_REPORT.md |
| db-experiment | 实验库重置→10 万行造数→无索引 vs 有索引查询对比，全程未触碰 lab.db |

## 3. 浏览器端测试（Chromium 实测，计划 §45 四条流程）

**流程一（练习闭环）**：首页 → 练习 → 选"05-heap 第 1 题"→ 作答 → 查看参考答案（揭示后按钮禁用）→ 自评"模糊" → 加入错题本（携带题目/答案/原因）→ 数据库确认新增 ✔

**流程二（实验场）**：实验场 → 快速排序 → 样例输入执行 → Result 面板（算法耗时 0.03ms / Trace 耗时 2.33ms 分列）→ Trace 播放器 26 步：单步 ✓ 回退 ✓ 播放/暂停 ✓ 缩略图跳步 ✓ → 查看 Java 源码 ✓ → 实验历史"重新打开"复现 ✓

**流程三（错题闭环）**：错题本 → 重新做 → 只显示题目+作答框 → 查看答案 → 标记"重做成功"→ redoCount=1、successCount=1、reviews=1 统计即时更新 ✔

**流程四（路由）**：`#/lab?feature=heap` → 刷新页面 → 页面与上下文（"当前知识点：二叉堆"）保持 → 前进/后退正常 → URL 可复制重开 ✔

## 4. UI 验证（计划 §46 尺寸 + §50 UI 标准）

| 尺寸 | 结果 |
|---|---|
| 1920×1080 | 双栏布局完整，截图 docs/screenshots/01-home-1920.png 等 |
| 1024×768 | 侧栏折叠为窄条，栅格降为单列，无溢出/重叠 |

检查项：按钮均可键盘聚焦（focus-visible 描边）；表单均有 Label；状态不只靠颜色（徽标带文字）；播放器快捷键在输入框聚焦时失效；执行失败保留用户输入并显示具体字段原因；空状态（无错题/无运行/无匹配搜索）均有引导文案；服务断开显示横幅"学习工作台服务已断开"并提供"重新连接"（实测）。

## 5. 数据持久化验证（计划 §47）

- 重启服务后：错题（61 条含历史）、学习状态、练习草稿、实验历史全部保留 ✔
- 数据库实验造数 10 万行后，学习库 runs/mistakes 计数不变；Q1 查询返回恒为 34 个算法 ✔

## 6. 已知边界（如实记录）

- 播放器 renderMatrix 在空 rows 时会渲染空白（现网不会出现：Trace 生成器保证 rows 非空）；
- 错题本"重做"的参考答案区只能展示当时记录的错误答案与提示（mistakes 表不存标准答案），界面已如实说明并引导去笔记对照；
- 数据库性能对比首跑含缓存效应，页面已标注"仅供教学参考"。
