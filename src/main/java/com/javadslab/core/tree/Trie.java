package com.javadslab.core.tree;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.ArrayList;
import java.util.List;

/**
 * 手写 Trie（前缀树/字典树）：小写字母 a-z。
 * insert / search / startsWith / delete / 前缀统计，均为 O(L)。
 * step-mode：insert/delete 记录 trace（快照为整棵树）。
 */
public class Trie {

    public static final int ALPHABET = 26;

    public static final class Node {
        public Node[] children = new Node[ALPHABET];
        public boolean isEnd;
        public int passCount; // 经过该节点的单词数（含以它结尾），支持删除与前缀统计
    }

    private final Node root = new Node();
    private int size;
    private Tracer tracer;

    /* ---------------- step-mode ---------------- */

    public void attachTracer(Tracer t) { this.tracer = t; }

    public Object snapshot() {
        return Snaps.tree(nodeSnapshot(root), "Trie(单词数=" + size + ")");
    }

    /** 递归生成树快照节点。 */
    private Object nodeSnapshot(Node n) {
        List<Object> children = new ArrayList<>();
        for (int c = 0; c < ALPHABET; c++) {
            if (n.children[c] != null) {
                Object child = nodeSnapshot(n.children[c]);
                com.javadslab.trace.Json.obj(child).put("ch", String.valueOf((char) ('a' + c)));
                children.add(child);
            }
        }
        return Snaps.treeNode(List.of(String.valueOf(n.passCount)), children, n.isEnd ? "end" : null, 0);
    }

    private Tracer.Step begin(String op) {
        if (tracer == null) return Tracer.inert();
        return tracer.step(op).before(snapshot());
    }

    private void commit(Tracer.Step s, String key, Object val) {
        if (tracer == null) return;
        if (key != null) s.hl(key, val);
        s.after(snapshot()).commit();
    }

    /* ---------------- 核心操作 ---------------- */

    public int size() { return size; }

    /** 插入单词（小写字母），集合语义：重复插入为无操作。O(L)。 */
    public void insert(String word) {
        validate(word);
        Node exist = walk(word);
        if (exist != null && exist.isEnd) return; // 已存在
        Tracer.Step s = begin("insert").arg("word", word);
        Node cur = root;
        cur.passCount++;
        for (char ch : word.toCharArray()) {
            int c = ch - 'a';
            if (cur.children[c] == null) cur.children[c] = new Node();
            cur = cur.children[c];
            cur.passCount++;
        }
        cur.isEnd = true;
        size++;
        commit(s, "word", word);
    }

    /** 完整单词是否存在。 */
    public boolean search(String word) {
        validate(word);
        Node n = walk(word);
        return n != null && n.isEnd;
    }

    /** 是否存在以 prefix 开头的单词。 */
    public boolean startsWith(String prefix) {
        validate(prefix);
        return walk(prefix) != null;
    }

    /** 以 prefix 开头的单词个数（passCount 统计）。 */
    public int countWithPrefix(String prefix) {
        validate(prefix);
        Node n = walk(prefix);
        return n == null ? 0 : n.passCount;
    }

    /** 删除单词（存在才删）；passCount 归零的节点被回收。 */
    public boolean delete(String word) {
        validate(word);
        if (!search(word)) return false;
        Tracer.Step s = begin("delete").arg("word", word);
        Node cur = root;
        cur.passCount--;
        for (char ch : word.toCharArray()) {
            int c = ch - 'a';
            Node child = cur.children[c];
            child.passCount--;
            if (child.passCount == 0) {
                cur.children[c] = null; // 整棵子树没有单词经过，直接剪掉
                // 后续节点已随子树脱离，无需继续
                size--;
                commit(s, "word", word);
                return true;
            }
            cur = child;
        }
        cur.isEnd = false;
        size--;
        commit(s, "word", word);
        return true;
    }

    private Node walk(String s) {
        Node cur = root;
        for (char ch : s.toCharArray()) {
            if (ch < 'a' || ch > 'z') return null;
            cur = cur.children[ch - 'a'];
            if (cur == null) return null;
        }
        return cur;
    }

    private void validate(String word) {
        if (word == null || word.isEmpty()) throw new IllegalArgumentException("单词不能为空");
        for (char ch : word.toCharArray()) {
            if (ch < 'a' || ch > 'z')
                throw new IllegalArgumentException("仅支持小写字母 a-z，非法字符: '" + ch + "'");
        }
    }

    /** 收集所有单词（字典序），用于测试与演示。 */
    public List<String> words() {
        List<String> out = new ArrayList<>();
        collect(root, new StringBuilder(), out);
        return out;
    }

    private void collect(Node n, StringBuilder path, List<String> out) {
        if (n.isEnd) out.add(path.toString());
        for (int c = 0; c < ALPHABET; c++) {
            if (n.children[c] != null) {
                path.append((char) ('a' + c));
                collect(n.children[c], path, out);
                path.deleteCharAt(path.length() - 1);
            }
        }
    }
}
