# 计划书：播放器缩略图导航 + 错题本命令行界面

> 状态标记：☐ 未开始 / ☑ 已验收（附证据）。验收全部通过后推送到 GitHub（origin: glh5835/JavaDS-Lab）。
> **验收结果（全部通过，2026-09-28）**：mvn test 166/166 全绿；44 个 trace 缩略图断言 44/44 通过；CLI 五个集成测试 + 管道冒烟 + 跨进程持久化验证通过。

## 功能 A：播放器缩略图导航（web/player.html）

在时间轴下方增加一排缩略图，把整个 trace 的关键帧平铺出来，点击即跳。

### 验收标准

- [x] **A1** 加载任一 trace 后出现缩略图条：数量 `min(10, 总步数)`，按步数均匀采样且含首尾两帧。
  证据：dijkstra（13 步）采出 `#1 #2 #4 #5 #6 #8 #9 #10 #12 #13`，44/44 断言 `thumbCount===min(10,N)` 且首尾步号正确。
- [x] **A2** 每个缩略图是该步快照的**真实等比渲染**（复用主渲染器画在独立小 SVG 里，缩放自 viewBox 900×480），不是占位色块；左下角标注步号。
  证据：44/44 断言每个缩略图 SVG `childElementCount > 0`（渲染器复用：抽出 `renderInto(svg, snapshot, hl)`）。
- [x] **A3** 点击缩略图跳到对应步：主画面、步数标签（`k / N`）、底部信息栏三者同步更新。
  证据：44/44 断言 `click → current.idx === 目标步号`。
- [x] **A4** 当前步最接近的缩略图带高亮边框；按「下一步」或自动播放时高亮跟随移动。
  证据：44/44 断言 `activeThumbStep` 与当前步一致（dijkstra 实测 goto(6) → 高亮 `#6`）。
- [x] **A5** 浏览器自动化回放全部 44 个 trace：缩略图条非空、点击跳转正确、无 JS 错误。
  证据：浏览器自动化逐 trace 断言，44/44 passed, bad=[]。
- [x] **A6** 原有功能不回退：主渲染回放 44/44 仍然通过。
  证据：同一轮回放中 `svgChildren > 0` 全部成立（dijkstra 主画面 45 个 SVG 子节点）。

## 功能 B：错题本命令行界面（com.javadslab.app.MistakeBookCli）

基于现有 Dao（PreparedStatement + 事务 + 手写连接池）做一个可脚本化测试的交互式 CLI。

### 命令集

| 命令 | 作用 |
|---|---|
| `algo` | 列出全部算法（id、名称、分类、难度） |
| `list` / `list <算法名>` | 列出全部 / 指定算法的错题 |
| `add` | 交互式录入：算法 → 题目 → 错误答案 → 原因 |
| `redo <id>` | 标记重做成功（redo_count+1） |
| `del <id>` | 删除错题 |
| `weak` | 按知识点输出薄弱项统计（错题数、平均重做次数） |
| `help` / `quit` | 帮助 / 退出 |

### 验收标准

- [x] **B1** `java -cp target/classes;<sqlite等jar>` 启动进入 `mistake>` 交互循环，上表 8 条命令全部可用。
  证据：管道脚本实测 algo/add/list/redo/weak/del/未知命令/quit 全链路（首次使用自动初始化算法字典）。
- [x] **B2** 所有落库操作走现有 Dao，`add` 后数据真实持久化（退出重进仍可见）。
  证据：冒烟会话 add→redo 后退出，二次启动 `list` 仍显示该记录且 `redo_count=1`。
- [x] **B3** 非法输入健壮：不存在的 id、未知命令、空白输入、add 中途算法名无效——全部给出友好提示且不崩溃、不产生脏数据。
  证据：`MistakeBookCliTest.invalidInputsAreFriendlyAndLeaveNoDirtyData` + `addValidationCancelsAfterBadAlgorithm`。
- [x] **B4** CLI 逻辑可测试：构造函数注入 `InputStream/PrintStream`；集成测试用脚本输入驱动完整会话，断言 add→list 可见→redo 后 weak 计数变化→del 后消失，并与直接查库的结果一致。
  证据：`MistakeBookCliTest.fullSessionAddListRedoWeakDel`（输出断言 + `dao.listAllMistakes()`/`weaknessReport()` 交叉验证）。
- [x] **B5** `mvn test` 全绿（原 161 + 本功能新增 5 = 166）。
  证据：`Tests run: 166, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS`。
- [x] **B6** 双击项目根目录 `错题本.bat` 即可使用（自动拼 sqlite-jdbc classpath）。
  证据：`cmd //c 错题本.bat` 管道驱动实测 list/quit 正常（修复了 bat 的 UTF-8/LF/变量展开三个坑：内容纯 ASCII、CRLF、PATH 不引用同块内 set 的变量）。

## 交付与推送

- [x] **C1** 更新 `docs/使用说明.md` 与 `README.md`（新功能用法）。
- [x] **C2** 推送前清理：`data/lab.db`（生成的二进制库）移出版本控制并加入 .gitignore。
- [x] **C3** 验收证据齐全后 commit，并 `git push origin`，`git ls-remote` 确认远端 HEAD 与本地一致。
