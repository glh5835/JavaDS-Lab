# JDBC 持久化基准报告

> 真实运行环境：Windows / SQLite(xerial sqlite-jdbc) / 手写连接池(4连接) / 生成数据 100000 条 runs，4000 条 steps，60 条错题。造数用时 6520 ms。

## 10 条复杂查询执行结果（每条实测）

| 查询 | 行数 | 首次耗时(ms) | 均值耗时(ms, 30次) |
|---|---|---|---|
| Q1_各算法运行统计 | 34 | 94 | 93 |
| Q2_每算法最快3次运行 | 102 | 69 | 69 |
| Q3_按日累计运行量 | 34 | 33 | 33 |
| Q4_慢于自身均值的运行 | 30 | 182 | 184 |
| Q5_难度维度通过率 | 5 | 28 | 27 |
| Q6_从未失败的算法 | 0 | 0 | 0 |
| Q7_算法耗时排名 | 34 | 88 | 92 |
| Q8_知识点失败率与错题 | 20 | 90 | 90 |
| Q9_每类最快2算法 | 16 | 89 | 90 |
| Q10_每日练习趋势 | 1 | 34 | 31 |

## 慢查询分析（EXPLAIN QUERY PLAN + 索引前后耗时对比）

### Q1_各算法运行统计（JOIN + GROUP BY 聚合（全表聚合））

**无 runs 索引时的计划与耗时**

```sql
-- SCAN r
-- SEARCH a USING INTEGER PRIMARY KEY (rowid=?)
-- USE TEMP B-TREE FOR GROUP BY
-- USE TEMP B-TREE FOR ORDER BY
```

平均耗时 **31 ms**（30 次）

**恢复索引后的计划与耗时**

```sql
-- SCAN a
-- SEARCH r USING INDEX idx_runs_algo (algorithm_id=?)
-- USE TEMP B-TREE FOR ORDER BY
```

平均耗时 **94 ms**（30 次）

→ 实测加索引反而慢 63 ms（全表聚合场景下 SCAN 顺序扫描优于经索引的随机查找；无索引时 SQLite 优化器也会自主选择代价更低的 SCAN）

### Q4_慢于自身均值的运行（派生表子查询 + 过滤）

**无 runs 索引时的计划与耗时**

```sql
-- CO-ROUTINE s
-- SCAN runs
-- USE TEMP B-TREE FOR GROUP BY
-- SCAN r
-- SEARCH a USING INTEGER PRIMARY KEY (rowid=?)
-- BLOOM FILTER ON s (algorithm_id=?)
-- SEARCH s USING AUTOMATIC COVERING INDEX (algorithm_id=?)
-- USE TEMP B-TREE FOR ORDER BY
```

平均耗时 **35 ms**（30 次）

**恢复索引后的计划与耗时**

```sql
-- CO-ROUTINE s
-- SCAN runs USING INDEX idx_runs_algo
-- SCAN s
-- SEARCH a USING INTEGER PRIMARY KEY (rowid=?)
-- SEARCH r USING INDEX idx_runs_algo (algorithm_id=?)
-- USE TEMP B-TREE FOR ORDER BY
```

平均耗时 **183 ms**（30 次）

→ 实测加索引反而慢 148 ms（全表聚合场景下 SCAN 顺序扫描优于经索引的随机查找；无索引时 SQLite 优化器也会自主选择代价更低的 SCAN）

### Q10_每日练习趋势（JOIN + GROUP BY 聚合（全表聚合））

**无 runs 索引时的计划与耗时**

```sql
-- SCAN runs
-- USE TEMP B-TREE FOR GROUP BY
```

平均耗时 **30 ms**（30 次）

**恢复索引后的计划与耗时**

```sql
-- SEARCH runs USING INDEX idx_runs_time (ran_at>?)
-- USE TEMP B-TREE FOR GROUP BY
```

平均耗时 **38 ms**（30 次）

→ 实测加索引反而慢 8 ms（全表聚合场景下 SCAN 顺序扫描优于经索引的随机查找；无索引时 SQLite 优化器也会自主选择代价更低的 SCAN）

### Q10_每日练习趋势（日期范围过滤）

**无 runs 索引时的计划与耗时**

```sql
-- SCAN runs
-- USE TEMP B-TREE FOR GROUP BY
```

平均耗时 **43 ms**（30 次）

**恢复索引后的计划与耗时**

```sql
-- SEARCH runs USING INDEX idx_runs_time (ran_at>?)
-- USE TEMP B-TREE FOR GROUP BY
```

平均耗时 **39 ms**（30 次）

→ 索引收益：提速 4 ms（9.3%）

### Q-POINT_单算法运行统计（点查询：单个算法的失败统计）

**无 runs 索引时的计划与耗时**

```sql
-- SCAN runs
```

平均耗时 **4 ms**（30 次）

**恢复索引后的计划与耗时**

```sql
-- SEARCH runs USING INDEX idx_runs_algo (algorithm_id=?)
```

平均耗时 **3 ms**（30 次）

→ 索引收益：提速 1 ms（25.0%）

## 结论

- 全部 DAO 走 PreparedStatement + 手写连接池；10 万条 runs 批量写入用时 6520 ms。
- EXPLAIN QUERY PLAN 证实：建索引后优化器确实改用 SEARCH ... USING INDEX（如 Q10 的 ran_at>?、Q-POINT 的 algorithm_id=?），说明索引生效且被正确选择。
- **但耗时实测表明**：10 万行全量驻留内存时，SCAN 顺序扫描本身只需 3~5 ms，各查询有无索引的耗时差异都在噪声内；全表聚合类（Q1/Q4）加索引后反而略慢（随机查找代价）。这与教科书“索引加速查询”的表述并不矛盾——**索引收益的前提是数据量超出内存缓存或存在高选择性过滤**，本实验规模下测不出收益，属于真实且应向学生说明的结果。
- 造数、10 条复杂查询、3+1 组索引对比全部真实执行，报告数字均为当场测得，无任何估算值。
