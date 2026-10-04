# DATABASE — 学习工作台数据库说明

> 数据库引擎：SQLite（xerial sqlite-jdbc 3.46.1.0）。迁移策略：**只加不改**（CREATE TABLE IF NOT EXISTS / ADD COLUMN IF ABSENT），从不删除旧表旧数据，旧 CLI 与既有数据完全兼容。

## 1. 两个数据库文件

| 文件 | 角色 | 写入方 |
|---|---|---|
| `data/lab.db` | **学习库**：错题、学习状态、练习、实验历史、算法字典。CLI（错题本.bat）与网页版共用 | 服务端（Dao/WorkbenchDao，全部参数绑定+事务） |
| `data/experiment.db` | **实验库**：数据库性能实验沙盒，允许造数/建删索引/EXPLAIN/受限自由 SQL；可随时删除重建 | 仅数据库实验页/后台任务 |

学习库**绝不**用于危险造数实验；实验库操作（代码路径上）无法触达 lab.db（自由 SQL 禁 ATTACH/PRAGMA/`..`）。

## 2. 原有表（sql/schema.sql，CLI 共用）

| 表 | 字段 | 说明 |
|---|---|---|
| algorithms | id, name(UNIQUE), category(CHECK 9类), difficulty(1-5), description | 算法字典。工作台启动时把功能目录中缺失的 10 个功能以 trace-id 命名补入（INSERT-IF-ABSENT），旧命名（如 quick-sort）保持不动 |
| runs | id, algorithm_id FK, ran_at, duration_ms≥0, input_size≥0, passed(0/1), score | 运行/练习记录（CLI 时代 + 造数数据） |
| steps | id, run_id FK, step_no, op, detail | 旧步骤审计表（CLI 时代数据保留；网页 Trace 存于 experiment_run.trace_json） |
| mistakes | id, algorithm_id FK, question, wrong_answer, reason, redo_count, created_at, **last_redo_at**(迁移新增), **success_count**(迁移新增,默认0) | 错题本 |

## 3. 工作台新增表（兼容迁移自动创建）

```sql
-- 学习状态（§5.4：只由用户主动设置，禁止自动推断）
learning_state(feature_id TEXT PK, status CHECK IN('unlearned','reviewing','mastered'), updated_at)

-- 练习作答记录（§6）
practice_attempt(id PK, practice_id, feature_id, answer,
                 revealed_answer(0/1), self_rating CHECK IN('unknown','fuzzy','mastered'),
                 created_at, updated_at)

-- 未完成作答草稿（§6.6：刷新/跨页可续，自评后自动清除）
practice_draft(practice_id TEXT PK, answer, revealed(0/1), updated_at)

-- 实验场运行历史（§14/§21：结果与 trace 同事务写入，绝不出现"有运行没步骤"）
experiment_run(run_id TEXT PK, feature_id, input_json, result_json,
               trace_available(0/1), trace_json,   -- 同一事务，失败整体回滚
               elapsed_ns, source CHECK IN('user','sample','replay','experiment'), created_at)

-- 错题每次重做（§15.3：result 三态；success 才累计 mistakes.success_count）
mistake_review(id PK, mistake_id FK→mistakes ON DELETE CASCADE, answer,
               result CHECK IN('fail','partial','success'), created_at)

-- 后台任务（§24；内存为主，表结构预留）
task_run(id PK, type, status CHECK IN('queued','running','success','failed','cancelled'),
         started_at, ended_at, summary, logs)

-- 应用级 KV（§5.2 继续学习上下文）
app_kv(key TEXT PK, value, updated_at)
```

索引：`idx_practice_attempt_feature(feature_id, practice_id)`、`idx_experiment_run_time(created_at)`、`idx_experiment_run_feature(feature_id, created_at)`、`idx_mistake_review(mistake_id, created_at)`，加上 schema.sql 原有 6 个索引。

## 4. 数据一致性

- **一次算法运行** = experiment_run 行（含 result_json 与 trace_json）在**同一事务**写入；任何一步失败整体回滚，不存在"运行存在但 Trace 缺失"的中间态（Trace 缺失只有一种合法情况：`trace_available=0`，即运行时主动降级为仅结果模式）。
- 错题重做：mistake_review 插入 + mistakes.redo_count/success_count/last_redo_at 更新在同一事务。
- 学习状态、练习自评由用户显式操作产生，系统不做任何自动写入。

## 5. 数据生命周期验证要点

重启服务后以下数据必须仍在（已验证）：错题、学习状态、练习记录/草稿、实验历史。
实验库实验（造数 10 万行、建删索引、自由 SQL）之后 lab.db 的 runs/mistakes 行数不变（已验证：Q1 查询行数恒为 34 算法）。

## 6. 手工维护

```bash
# 重置实验库（不影响学习库）：删除文件即可
rm data/experiment.db

# 备份学习数据
cp data/lab.db data/lab.backup.db
```
