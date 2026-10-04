# JavaDS-Lab 学习工作台

一个 Java 数据结构与算法教学项目：**手写实现 + 操作追踪 + 可视化回放 + SQLite 持久化 + 学习笔记**，
现已整合为**本地优先、单用户、无需联网的学习工作台**——围绕每个知识点形成完整闭环：
**理解 → 练习 → 验证 → 纠错 → 复习**。

## 🚀 快速开始（唯一入口）

**双击 `启动工作台.bat`** —— 自动检查环境 → 启动本地服务 → 打开浏览器进入学习中心。
无需手动运行 Maven 或 Java 命令；运行期完全离线。

```
启动后地址：http://127.0.0.1:8642/
```

| 页面 | 用途 |
|---|---|
| 学习中心 | 进度汇总 / 继续学习 / 推荐路线 / 薄弱项 |
| 自测练习 | 110 道练习题（主动回忆 → 对照答案 → 自评） |
| 数据实验场 | 运行 44 个真实 Java 算法 + Trace 回放播放器 |
| 错题本 | 错题管理 / 重做 / 薄弱统计（与 CLI 共用数据） |
| 学习笔记 · 复习地图 · 数据库实验 · 项目验证 | 辅助模块 |

旧入口：`启动播放器.bat` 已转为转发到工作台；`错题本.bat`（CLI）与 `运行测试.bat` 保留。

## 📚 文档

| 文档 | 内容 |
|---|---|
| [docs/WORKBENCH_GUIDE.md](docs/WORKBENCH_GUIDE.md) | 工作台使用说明（启动、闭环流程、常见问题） |
| [docs/API.md](docs/API.md) | HTTP 接口说明 |
| [docs/DATABASE.md](docs/DATABASE.md) | 数据库 Schema 与迁移策略 |
| [docs/PROJECT_INVENTORY.md](docs/PROJECT_INVENTORY.md) | 项目盘点（改造前基线） |
| [docs/FEATURE_MATRIX.md](docs/FEATURE_MATRIX.md) | 功能覆盖表（44 功能 × 笔记/练习/Trace/源码） |
| [docs/TRACE_VALIDATION_REPORT.md](docs/TRACE_VALIDATION_REPORT.md) | 44 个 Trace 校验报告（可在"项目验证"页重新生成） |
| [docs/TEST_REPORT.md](docs/TEST_REPORT.md) | 测试记录（Java/API/浏览器四流程/UI 尺寸） |
| [docs/使用说明.md](docs/使用说明.md) | 原 CLI 时代操作手册（仍然有效） |

## 🧪 开发者

```bash
# 环境要求：JDK 17 + Maven（tools/ 内含便携版，可用 tools/env.sh 激活）
source tools/env.sh
mvn -s tools/maven-settings.xml test      # 全量 181 个测试（166 原有 + 15 API 集成测试）

# 命令行启动工作台（同 BAT 效果）
java -cp "target/classes;<sqlite-jdbc.jar>;<slf4j.jar>" com.javadslab.workbench.WorkbenchServer 8642 .

# 重新生成 trace（44 个 JSON + 播放器 bundle + manifest）
java -cp target/classes com.javadslab.app.TraceGenerator .
```

## 模块结构

| 目录/包 | 内容 |
|---|---|
| `src/main/java/com/javadslab/core/` | 手写数据结构与算法（禁止 java.util 树/堆/排序充当核心逻辑） |
| `src/main/java/com/javadslab/trace/` | step-mode 追踪器、迷你 JSON 库、可追踪排序/二分/DP |
| `src/main/java/com/javadslab/persist/` | SQLite 连接池 + DAO + 造数 + 10 条复杂查询 |
| `src/main/java/com/javadslab/workbench/` | **学习工作台服务端**：功能目录 / 实验场 Runner / 学习闭环 DAO / 后台任务 / HTTP 服务 |
| `web/workbench/` | **工作台前端**：Hash Router 单页应用（导航/主题/8 个页面/播放器） |
| `web/player.html` | 旧独立播放器（已由工作台整合，保留作离线备份） |
| `trace/` | 44 个 trace JSON + manifest |
| `sql/` | schema.sql + ER 说明 |
| `reports/jdbc-report.md` | 10 万条数据 + EXPLAIN 索引对比实测报告（历史） |
| `docs/notes/` | 22 篇算法笔记（图解+代码+复杂度推导+易错点+5 题） |
| `docs/期末复习图谱.md` | 按依赖分层 + 期中高频标注 |

## 测试覆盖说明

- 正常路径 / 边界（空、单元素、回绕）/ 异常（越界、空弹、非法参数）
- **大规模随机对照**：所有容器与 JDK 参照实现对拍 2~5 万次随机操作；算法类与暴力解对拍；树结构带不变量校验器
- 集成测试：SQLite 建库→10 万条造数→10 条复杂查询→错题本全流程
- **工作台 API 集成测试**：随机端口启动真实服务，覆盖正常/空参/非法/边界/规模限制/数据库异常/安全白名单
