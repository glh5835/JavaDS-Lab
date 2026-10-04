# API — 学习工作台 HTTP 接口说明

> 基址 `http://127.0.0.1:8642`，仅监听本机。所有请求/响应均为 UTF-8 JSON。
> 统一响应包络：成功 `{"success": true, "data": {...}}`；失败 `{"success": false, "error": {"code", "message", "field"}}`（field 可选，指出出错字段；不返回 Java 堆栈）。
> 所有响应带 `Cache-Control: no-store`。

错误码：`INVALID_INPUT`(400 参数不合法) / `NOT_FOUND`(404) / `FORBIDDEN`(403 路径越界) / `EXECUTION_ERROR`(500 执行失败) / `INTERNAL`(500)。

## 健康检查

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/health` | `{status, database, version, features, practices}`；启动脚本等待它成功后开浏览器 |

## 功能目录（单一事实来源）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/features?category=&q=` | 全部功能（44）+ 分类表；每项含 id/name/category/description/javaClass/sourcePath/noteId/traceSupported/inputType/sampleInput/prerequisites/algoName/learningState/practiceCount |
| GET | `/api/features/{id}` | 单个功能详情 + practiceIds + dependents（谁以它为前置）+ mistakeCount |

## 算法运行（真实 Java 执行）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/runs` | body `{featureId, input(字符串化 JSON), source}`。服务端校验后调用 core 实现；结果与 Trace 同事务写入 experiment_run。返回 `{runId, result, traceAvailable, elapsedNs}`。result 内含 `elapsedResultNs`（纯算法）与 `elapsedTraceNs`（Trace 记录），二者分列 |
| GET | `/api/runs?featureId=&limit=` | 运行历史（新→旧） |
| GET | `/api/runs/{runId}` | 单条（含 inputJson/resultJson） |
| GET | `/api/runs/{runId}/trace` | Trace JSON（统一格式 meta/steps{op,args,before,after,hl}）；无 Trace 返回 404 NO_TRACE |

输入校验要点（服务端为最终边界）：数组 ≤20000（Trace 另有步数上限 3000，超限自动降级仅结果模式并提示）；二分查找要求升序（明确报错，不自动排序）；Dijkstra 边权非负；图顶点 ≤300、边 ≤3000；结构操作 ≤500 条。

## 练习

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/practices?featureId=&q=` | 列表（**不含答案**） |
| GET | `/api/practices/{practiceId}` | 详情（**含 answer**；前端默认折叠，揭示行为记录 revealed） |
| POST | `/api/practice-attempts` | `{practiceId, featureId, answer, revealed, selfRating(unknown/fuzzy/mastered)}` |
| GET | `/api/practice-drafts` | 全部草稿 |
| PUT | `/api/practice-drafts/{practiceId}` | `{answer, revealed}` 自动保存草稿 |

## 学习状态（只由用户设置）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/learning` | 全部 `{featureId, status(unlearned/reviewing/mastered), updatedAt}` |
| PUT | `/api/learning/{featureId}` | `{status}`；非法值 400 |

## 错题（与 CLI 共用 data/lab.db 的 mistakes 表）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/mistakes?algoName=&q=&limit=` | 列表（含 redoCount/successCount/lastRedoAt） |
| GET | `/api/mistakes/{id}` | 详情 + 重做记录 reviews |
| POST | `/api/mistakes` | `{featureId 或 algoName, question, wrongAnswer, reason}` |
| PUT | `/api/mistakes/{id}` | 编辑题目/错误答案/原因 |
| DELETE | `/api/mistakes/{id}` | 删除 |
| POST | `/api/mistakes/{id}/reviews` | `{answer, result(fail/partial/success)}`；success 才累计 success_count |
| GET | `/api/weakness` | 薄弱项统计（只描述记录，不推断掌握状态） |

## 笔记 / 复习地图

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/notes` | 22 篇目录（noteId/title/featureIds） |
| GET | `/api/notes/{noteId}` | 笔记 Markdown 原文（如 `19-sorting`） |
| GET | `/api/review/map` | 全部知识点 + 前置关系 + 学习状态 + 练习/错题数量 |

## 数据库实验

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/database/queries` | 10 条固定查询目录（SQL 原文、是否有参数） |
| POST | `/api/database/query` | `{queryId(Q1~Q10), param}` → 真实执行 + 列/行/耗时/EXPLAIN（学习库只读） |
| GET | `/api/database/status` | 实验库状态（是否存在/已造数/runs 数/索引数） |
| GET | `/api/database/schema` | 表清单 + ER.md + schema.sql 原文 |
| POST | `/api/database/experiment` | `{op: seed/reset/create-index/drop-index/explain/sql, params}`；仅作用于 `data/experiment.db`；自由 SQL 单条且禁 ATTACH/DETACH/PRAGMA |

## 后台任务

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/tasks` | `{type: maven-test / db-experiment / full-verify}` → `{id, status}`；不阻塞 HTTP |
| GET | `/api/tasks` | 任务列表 |
| GET | `/api/tasks/{id}` | 状态(queued/running/success/failed) + 完整日志 + summary |

## 其他

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/source?path=` | 查看 Java 源码；仅允许 `src/main/java/com/javadslab/**.java`（白名单，防穿越） |
| GET | `/api/report/jdbc` | 历史 JDBC 基准报告（响应内明确标注"历史报告"） |
| GET | `/api/context` / PUT | 继续学习上下文的读取/保存（存 app_kv 表） |
| GET | `/api/home` | 首页聚合（状态汇总/最近运行/薄弱项/继续学习） |

## 安全边界（服务端强制）

- 仅监听 `127.0.0.1`；
- 静态资源仅从 `web/workbench/` 读取，路径规范化防 `..` 穿越；
- 源码查看白名单目录；SQL 全部参数绑定（学习库无自由 SQL 入口）；
- 规模上限防止错误输入拖垮服务；错误响应不含堆栈，完整异常写 `logs/workbench.log`。
