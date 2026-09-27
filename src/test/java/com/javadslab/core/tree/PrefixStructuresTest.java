package com.javadslab.core.tree;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Trie / SegmentTree / FenwickTree 测试。 */
class PrefixStructuresTest {

    /* ================= Trie ================= */

    @Test
    void trieInsertSearchPrefix() {
        Trie t = new Trie();
        t.insert("apple");
        assertTrue(t.search("apple"));
        assertFalse(t.search("app"));
        assertTrue(t.startsWith("app"));
        t.insert("app");
        assertTrue(t.search("app"));
        assertEquals(2, t.countWithPrefix("ap"));
        assertEquals(1, t.countWithPrefix("appl"));
        assertEquals(0, t.countWithPrefix("b"));
    }

    @Test
    void trieDelete() {
        Trie t = new Trie();
        t.insert("ab"); t.insert("abc");
        assertEquals(2, t.size());
        assertTrue(t.delete("ab"));
        assertFalse(t.search("ab"));
        assertTrue(t.search("abc"));       // 共享前缀不受影响
        assertTrue(t.delete("abc"));
        assertEquals(0, t.size());
        assertTrue(t.words().isEmpty());
        assertFalse(t.delete("xyz"));      // 不存在返回 false
    }

    @Test
    void trieDictionaryOrderAndDuplicateInsert() {
        Trie t = new Trie();
        t.insert("banana"); t.insert("apple"); t.insert("cherry"); t.insert("apple");
        assertEquals(3, t.size()); // 集合语义：重复插入不计数
        assertEquals(List.of("apple", "banana", "cherry"), t.words()); // 字典序且去重
        t.delete("apple");
        assertFalse(t.search("apple"));
        assertEquals(2, t.size());
    }

    @Test
    void trieValidation() {
        Trie t = new Trie();
        assertThrows(IllegalArgumentException.class, () -> t.insert("ABC")); // 大写
        assertThrows(IllegalArgumentException.class, () -> t.insert(""));
        assertThrows(IllegalArgumentException.class, () -> t.insert(null));
        assertThrows(IllegalArgumentException.class, () -> t.insert("ap1e"));
        assertThrows(IllegalArgumentException.class, () -> t.search("App"));
    }

    @Test
    void trieCrossCheckWithSet() {
        Random rnd = new Random(17L);
        Trie mine = new Trie();
        java.util.Set<String> ref = new java.util.HashSet<>(); // Trie 是集合语义，oracle 用 Set
        for (int iter = 0; iter < 5_000; iter++) {
            int len = 1 + rnd.nextInt(6);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) sb.append((char) ('a' + rnd.nextInt(26)));
            String w = sb.toString();
            int op = rnd.nextInt(3);
            if (op == 0) {
                mine.insert(w);
                ref.add(w);
            } else if (op == 1) {
                assertEquals(ref.contains(w), mine.search(w), "word=" + w);
            } else {
                if (ref.contains(w)) {
                    assertTrue(mine.delete(w));
                    ref.remove(w);
                }
            }
            assertEquals(ref.size(), mine.size(), "iter " + iter);
            assertEquals(ref.contains(w), mine.search(w), "iter " + iter);
        }
    }

    /* ================= SegmentTree ================= */

    @Test
    void segmentTreeBuildUpdateQuery() {
        SegmentTree st = new SegmentTree(new int[]{1, 3, 5, 7, 9, 11});
        assertEquals(36, st.query(0, 5));
        assertEquals(15, st.query(1, 3));  // 0-based [1,3] = 3+5+7
        assertEquals(1, st.query(0, 0));
        st.update(2, 10);                   // 5 -> 10
        assertEquals(41, st.query(0, 5));
        assertEquals(20, st.query(1, 3));
        st.update(0, -5);
        assertEquals(-2, st.query(0, 1)); // -5+3
    }

    @Test
    void segmentTreeExceptions() {
        SegmentTree st = new SegmentTree(new int[]{1, 2, 3});
        assertThrows(IndexOutOfBoundsException.class, () -> st.query(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> st.query(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> st.update(3, 1));
        assertThrows(IllegalArgumentException.class, () -> st.query(2, 1));
        assertThrows(IllegalArgumentException.class, () -> new SegmentTree(new int[0]));
    }

    @Test
    void segmentTreeRandomCrossCheckWithBruteForce() {
        Random rnd = new Random(21L);
        int n = 200;
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = rnd.nextInt(1000) - 500;
        SegmentTree st = new SegmentTree(a);
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + a[i];
        for (int iter = 0; iter < 20_000; iter++) {
            if (rnd.nextBoolean()) {
                int i = rnd.nextInt(n);
                int v = rnd.nextInt(1000) - 500;
                st.update(i, v);
                a[i] = v;
                for (int j = i; j < n; j++) prefix[j + 1] = prefix[j] + a[j]; // 从 i 起向后重算
            } else {
                int l = rnd.nextInt(n), r = rnd.nextInt(n);
                if (l > r) { int t = l; l = r; r = t; }
                long want = prefix[r + 1] - prefix[l];
                assertEquals(want, st.query(l, r), "iter " + iter + " [" + l + "," + r + "]");
            }
        }
    }

    /* ================= FenwickTree ================= */

    @Test
    void fenwickBasic() {
        FenwickTree ft = new FenwickTree(new int[]{1, 2, 3, 4, 5});
        assertEquals(15, ft.prefixSum(5));
        assertEquals(6, ft.prefixSum(3));
        assertEquals(7, ft.rangeSum(3, 4)); // 1-based [3,4] = 3+4
    }

    @Test
    void fenwickAddAndPointValue() {
        FenwickTree ft = new FenwickTree(5);
        for (int i = 1; i <= 5; i++) ft.add(i, i * 10);
        assertEquals(10, ft.pointValue(1));
        assertEquals(30, ft.pointValue(3));
        ft.add(3, -10);
        assertEquals(20, ft.pointValue(3));
        assertEquals(140, ft.prefixSum(5)); // 10+20+20+40+50
        assertEquals(110, ft.rangeSum(3, 5)); // 20+40+50
    }

    @Test
    void fenwickExceptions() {
        FenwickTree ft = new FenwickTree(3);
        assertThrows(IndexOutOfBoundsException.class, () -> ft.prefixSum(0));
        assertThrows(IndexOutOfBoundsException.class, () -> ft.add(4, 1));
        assertThrows(IllegalArgumentException.class, () -> ft.rangeSum(2, 1));
        assertThrows(IllegalArgumentException.class, () -> new FenwickTree(0));
    }

    @Test
    void fenwickRandomCrossCheckWithBruteForce() {
        Random rnd = new Random(23L);
        int n = 150;
        FenwickTree ft = new FenwickTree(n);
        int[] a = new int[n + 1]; // 1-based
        for (int iter = 0; iter < 20_000; iter++) {
            int i = 1 + rnd.nextInt(n);
            if (rnd.nextBoolean()) {
                int d = rnd.nextInt(100) - 50;
                ft.add(i, d);
                a[i] += d;
            } else {
                int want = 0;
                for (int k = 1; k <= i; k++) want += a[k];
                assertEquals(want, ft.prefixSum(i), "iter " + iter);
            }
        }
    }

    /* ================= step-mode ================= */

    @Test
    void tracersProduceValidSnapshots() {
        var tt = new com.javadslab.trace.Tracer("tree", "Trie 演示");
        Trie trie = new Trie();
        trie.attachTracer(tt);
        trie.insert("ab"); trie.insert("abc"); trie.delete("ab");
        assertTrue(tt.stepCount() >= 3);

        var ts = new com.javadslab.trace.Tracer("tree", "线段树演示");
        SegmentTree st = new SegmentTree(new int[]{1, 2, 3, 4});
        st.attachTracer(ts);
        st.query(1, 3); st.update(2, 9);
        assertTrue(ts.stepCount() >= 2);

        var tf = new com.javadslab.trace.Tracer("array", "树状数组演示");
        FenwickTree ft = new FenwickTree(6);
        ft.attachTracer(tf);
        ft.add(3, 5); ft.prefixSum(6);
        assertTrue(tf.stepCount() >= 2);

        for (var t : List.of(tt, ts, tf)) {
            Object parsed = com.javadslab.trace.Json.parse(com.javadslab.trace.Json.write(t.toTrace()));
            for (Object so : com.javadslab.trace.Json.arr(com.javadslab.trace.Json.obj(parsed).get("steps"))) {
                var stp = com.javadslab.trace.Json.obj(so);
                assertNotNull(stp.get("before"));
                assertNotNull(stp.get("after"));
            }
        }
    }
}
