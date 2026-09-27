package com.javadslab.core.unionfind;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** UnionFind：正常 / 边界 / 异常 / 随机对照（与暴力连通性对照）。 */
class UnionFindTest {

    @Test
    void basicUnionAndFind() {
        UnionFind uf = new UnionFind(6);
        assertEquals(6, uf.components());
        assertTrue(uf.union(0, 1));
        assertTrue(uf.union(1, 2));
        assertTrue(uf.union(3, 4));
        assertEquals(3, uf.components());
        assertTrue(uf.connected(0, 2));
        assertTrue(uf.connected(3, 4));
        assertFalse(uf.connected(2, 3));
        assertEquals(3, uf.sizeOf(0)); // {0,1,2}
        assertEquals(2, uf.sizeOf(4)); // {3,4}
    }

    @Test
    void repeatedUnionIsIdempotent() {
        UnionFind uf = new UnionFind(4);
        assertTrue(uf.union(0, 1));
        assertFalse(uf.union(0, 1)); // 已同集合
        assertFalse(uf.union(1, 0));
        assertEquals(3, uf.components());
    }

    @Test
    void selfRootFind() {
        UnionFind uf = new UnionFind(3);
        assertEquals(0, uf.find(0));
        assertEquals(2, uf.find(2));
    }

    @Test
    void chainUnionThenCompress() {
        // 链式合并 0-1-2-...-9，find 路径压缩后所有 find 都是根
        UnionFind uf = new UnionFind(10);
        for (int i = 0; i < 9; i++) uf.union(i, i + 1);
        assertEquals(1, uf.components());
        int root = uf.find(0);
        for (int i = 0; i < 10; i++) assertEquals(root, uf.find(i));
        assertEquals(10, uf.sizeOf(5));
    }

    @Test
    void emptyUnionFind() {
        UnionFind uf = new UnionFind(0);
        assertEquals(0, uf.components());
    }

    @Test
    void outOfBoundsThrows() {
        UnionFind uf = new UnionFind(3);
        assertThrows(IndexOutOfBoundsException.class, () -> uf.find(3));
        assertThrows(IndexOutOfBoundsException.class, () -> uf.find(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> uf.union(0, 9));
    }

    @Test
    void crossCheckWithBruteForceConnectivity() {
        // 随机 union/查询，与「重新扫描合并集合」的暴力实现对照
        Random rnd = new Random(99L);
        int n = 100;
        UnionFind uf = new UnionFind(n);
        int[] brute = new int[n]; // brute[i] = 组号
        for (int i = 0; i < n; i++) brute[i] = i;
        for (int iter = 0; iter < 5_000; iter++) {
            int a = rnd.nextInt(n), b = rnd.nextInt(n);
            if (rnd.nextBoolean()) {
                boolean merged = uf.union(a, b);
                boolean bruteMerged = brute[a] != brute[b];
                assertEquals(bruteMerged, merged);
                if (bruteMerged) {
                    int ga = brute[a], gb = brute[b];
                    for (int i = 0; i < n; i++) if (brute[i] == gb) brute[i] = ga;
                }
            } else {
                assertEquals(brute[a] == brute[b], uf.connected(a, b), "a=" + a + " b=" + b);
            }
        }
        assertEquals(countGroups(brute), uf.components());
    }

    private int countGroups(int[] labels) {
        java.util.Set<Integer> s = new java.util.HashSet<>();
        for (int v : labels) s.add(v);
        return s.size();
    }
}
