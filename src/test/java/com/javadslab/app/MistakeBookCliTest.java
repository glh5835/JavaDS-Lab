package com.javadslab.app;

import com.javadslab.persist.Dao;
import com.javadslab.persist.Db;
import com.javadslab.persist.SeedData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 错题本 CLI 集成测试：脚本化输入驱动完整会话，输出与数据库状态双重断言。 */
class MistakeBookCliTest {

    @TempDir
    Path tmp;

    private Db db;
    private Dao dao;

    @BeforeEach
    void setUp() throws Exception {
        db = new Db(tmp.resolve("cli-" + System.nanoTime() + ".db").toString(), 2);
        dao = new Dao(db);
        SeedData.applySchema(db, Path.of("sql/schema.sql"));
        dao.insertAlgorithm("heap", "tree", 3, "二叉堆");
        dao.insertAlgorithm("avl", "tree", 4, "AVL");
        dao.insertAlgorithm("red-black-tree", "tree", 5, "红黑树");
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    /** 跑一段脚本会话，返回控制台输出。 */
    private String runSession(String script) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        MistakeBookCli cli = new MistakeBookCli(dao,
                new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)), out);
        assertDoesNotThrow(cli::runLoop);
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void fullSessionAddListRedoWeakDel() {
        String out = runSession(String.join("\n",
                "algo",
                "add", "heap", "手写堆第 3 题", "WA3 越界", "下沉漏了右孩子",
                "add", "red-black-tree", "红黑树删除", "RE", "兄弟为空漏判",
                "list",
                "redo 1", "redo 1", "redo 2",
                "weak",
                "del 2",
                "list",
                "quit", ""));

        // add 的回显
        assertTrue(out.contains("已记录错题 #1（算法：heap）"), out);
        assertTrue(out.contains("已记录错题 #2"), out);
        // list 展示两条
        assertTrue(out.contains("手写堆第 3 题"), out);
        assertTrue(out.contains("红黑树删除"), out);
        // redo 回显
        assertTrue(out.contains("重做成功 +1"), out);
        // weak 统计：heap 1 错 2 次重做，红黑树 1 错 1 次
        assertTrue(out.contains("薄弱知识点"), out);
        // del 回显 + list 只剩一条（“红黑树删除”只应出现在 del 之前的第一次 list 里）
        assertTrue(out.contains("已删除错题 #2"), out);
        assertEquals(1, out.split("红黑树删除", -1).length - 1, "del 后不应再列出已删错题: " + out);

        // 数据库状态断言（与输出交叉验证）
        List<Dao.MistakeRow> rows = dao.listAllMistakes();
        assertEquals(1, rows.size());
        assertEquals("heap", rows.get(0).algorithm());
        assertEquals(2, rows.get(0).redoCount()); // redo 1 两次
        List<Dao.Weakness> weak = dao.weaknessReport();
        assertEquals(1, weak.size());
        assertEquals(2.0, weak.get(0).avgRedo());
    }

    @Test
    void invalidInputsAreFriendlyAndLeaveNoDirtyData() {
        String out = runSession(String.join("\n",
                "nonsense",
                "redo abc",
                "redo 99",
                "del xyz",
                "del 99",
                "list",
                "add", "", "quit", ""));

        assertTrue(out.contains("未知命令: nonsense"), out);
        assertTrue(out.contains("用法示例：redo 3 / del 3"), out);
        assertEquals(2, out.split("未找到错题 #99").length - 1, "redo 99 与 del 99 各提示一次");
        assertTrue(out.contains("还没有错题记录"), out);
        assertTrue(out.contains("再见。"), out);
        assertEquals(0, dao.listAllMistakes().size(), "非法输入不得产生脏数据");
    }

    @Test
    void addValidationCancelsAfterBadAlgorithm() {
        String out = runSession(String.join("\n",
                "add", "no-such-algo", "still-nope", "nope3",
                "list",
                "quit", ""));
        assertTrue(out.contains("找不到算法"), out);
        assertTrue(out.contains("连续 3 次无效，已取消 add"), out);
        assertEquals(0, dao.listAllMistakes().size());
    }

    @Test
    void addByAlgorithmIdAndEmptyInputReprompt() {
        // 用 id=2（avl）添加；中间夹一个空输入触发重试
        String out = runSession(String.join("\n",
                "add", "2", "", "AVL 第 1 题", "WA1", "旋转方向写反",
                "list",
                "quit", ""));
        assertTrue(out.contains("已记录错题 #1（算法：avl）"), out);
        assertTrue(out.contains("AVL 第 1 题"), out);
        assertEquals(1, dao.listAllMistakes().size());
    }

    @Test
    void helpAndEmptyLinesDoNotCrash() {
        String out = runSession(String.join("\n",
                "", "   ", "help", "?", "quit", ""));
        assertTrue(out.contains("命令："), out);
        assertTrue(out.contains("再见。"), out);
    }
}
