# JavaDS-Lab 使用指南

一个 Java 数据结构与算法教学项目：**手写实现 + 操作追踪 + 可视化回放 + SQLite 持久化 + 学习笔记**。

> 📖 **完整操作手册见 [docs/使用说明.md](docs/使用说明.md)**（环境准备、跑测试、播放器操作、数据库实验、常见问题）。
> 快速开始：双击 `启动播放器.bat` 看动画；双击 `运行测试.bat` 跑全部 161 个测试。

## 快速开始

```bash
# 环境要求：JDK 17 + Maven（tools/ 内含便携版，可用 tools/env.sh 激活）
source tools/env.sh
mvn -s tools/maven-settings.xml test      # 全量测试（161 个）
```

## 模块结构

| 目录/包 | 内容 |
|---|---|
| `src/main/java/com/javadslab/core/` | 模块一：全部手写数据结构与算法（禁止 java.util 树/堆/排序充当核心逻辑） |
| `src/main/java/com/javadslab/trace/` | 模块二：step-mode 追踪器、迷你 JSON 库、可追踪排序/二分/DP |
| `src/main/java/com/javadslab/persist/` | 模块四：SQLite 连接池 + DAO + 造数 + 10 条复杂查询 |
| `web/player.html` | 模块三：单文件 SVG 播放器（单步/回退/自动播放/变速） |
| `trace/` | 34 个算法各至少 1 个 trace JSON |
| `sql/` | schema.sql + ER 说明 |
| `reports/jdbc-report.md` | 10 万条数据 + EXPLAIN 索引对比实测报告 |
| `docs/notes/` | 22 篇算法笔记（图解+代码+复杂度推导+易错点+5 题） |
| `docs/期末复习图谱.md` | 按依赖分层 + 期中高频标注 |

## 可视化播放器

方式一（推荐，直接双击打开 `web/player.html`）：
- file:// 协议下自动加载 `web/traces-bundle.js` 内嵌的全部 34 个 trace。

方式二（加载 trace/ 目录的独立 JSON）：
```bash
python -m http.server 8765   # 在项目根目录
# 打开 http://127.0.0.1:8765/web/player.html，下拉框选择任意 trace
# 或播放器头部“从本地 JSON 加载”任选 trace/*.json
```

操作：`◀ 上一步` / `下一步 ▶` 单步与回退，`▶ 播放` 自动播放，速度 0.5×~4×，
滑杆任意跳步；键盘 ←/→ 单步、空格播放暂停。支持的渲染类型：
array / linked / hash / heap / tree / graph / matrix / uf / monostack。

## 重新生成 trace

```bash
java -cp target/classes com.javadslab.app.TraceGenerator .
```

## 重新生成 JDBC 基准报告（真实耗时）

```bash
SQLITE_JAR=~/.m2/repository/org/xerial/sqlite-jdbc/3.46.1.0/sqlite-jdbc-3.46.1.0.jar
SLF4J_JAR=~/.m2/repository/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar
java -cp "target/classes;$(cygpath -w $SQLITE_JAR);$(cygpath -w $SLF4J_JAR)" \
     com.javadslab.app.QueryBenchmark .
# 输出 reports/jdbc-report.md
```

## 测试覆盖说明

- 正常路径 / 边界（空、单元素、回绕）/ 异常（越界、空弹、非法参数）
- **大规模随机对照**：所有容器与 JDK 参照实现对拍 2~5 万次随机操作；
  算法类与暴力解对拍；树结构带不变量校验器（AVL 平衡、RB 五性质、B 树键数范围）
- 集成测试：SQLite 建库→10 万条造数→10 条复杂查询→错题本全流程
