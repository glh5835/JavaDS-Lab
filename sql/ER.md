# ER 说明（SQLite）

## 实体关系图（ASCII）

```
┌──────────────────┐         ┌──────────────────────┐
│    algorithms    │1      * │        runs          │
├──────────────────├─────────├──────────────────────┤
│ id        PK     │         │ id             PK    │
│ name      UNIQUE │         │ algorithm_id   FK ───┼──→ algorithms.id
│ category  CHECK  │         │ ran_at               │
│ difficulty CHECK │         │ duration_ms CHECK>=0 │
│ description      │         │ input_size  CHECK>=0 │
└────────┬─────────┘         │ passed       IN(0,1) │
         │ 1                 │ score         >=0    │
         │                   └──────────┬───────────┘
         │                              │ 1
         │                              │
         │ *                            │ *
┌────────┴─────────┐         ┌──────────┴───────────┐
│     mistakes     │         │        steps         │
├──────────────────┤         ├──────────────────────┤
│ id        PK     │         │ id             PK    │
│ algorithm_id FK ─┼──→      │ run_id         FK ───┼──→ runs.id
│ question         │ algs.id │ step_no      CHECK   │
│ wrong_answer     │         │ op                   │
│ reason           │         │ detail               │
│ redo_count CHECK │         └──────────────────────┘
│ created_at       │
└──────────────────┘
```

## 关系与约束

| 关系 | 基数 | 外键 | 删除行为 |
|---|---|---|---|
| algorithms → runs | 1 : N | runs.algorithm_id | ON DELETE CASCADE |
| runs → steps | 1 : N | steps.run_id | ON DELETE CASCADE |
| algorithms → mistakes | 1 : N | mistakes.algorithm_id | ON DELETE CASCADE |

- **algorithms**：算法字典表。`category` 用 CHECK 枚举 9 大类；`difficulty` 1~5。
- **runs**：每次运行/练习一条。CHECK 保证时长与规模非负、passed 只能 0/1。
- **steps**：运行的操作步骤（回放审计用）。`(run_id, step_no)` 复合索引覆盖
  “取某次运行的全部步骤并按序号排序”这一最常见查询。
- **mistakes**：错题本。`redo_count` 记录重做次数，`reason` 记录错误原因归类。

## 索引设计

| 索引 | 服务场景 |
|---|---|
| idx_runs_algo | 按算法聚合运行次数/平均耗时 |
| idx_runs_time | 按时间过滤（最近 N 天练习） |
| idx_runs_algo_time | 算法+时间联合：某算法的耗时趋势 |
| idx_runs_passed | 通过/未通过筛选 |
| idx_steps_run | 步骤按运行回放（覆盖排序） |
| idx_mistakes_algo / idx_mistakes_redo | 错题按知识点统计、按重做次数排序 |

全部索引在 `sql/schema.sql` 内与建表语句一起维护，基准对比中删除/重建以测量收益。
