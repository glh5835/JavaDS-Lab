package com.javadslab.core.unionfind;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 手写并查集（Disjoint Set Union）：
 * 路径压缩 + 按大小合并，find/union 均 O(α(n)) 近似 O(1)。
 * step-mode：find/union 记录 trace（parent 数组快照）。
 */
public class UnionFind {

    private final int[] parent;
    private final int[] size;
    private int components;
    private Tracer tracer;

    public UnionFind(int n) {
        if (n < 0) throw new IllegalArgumentException("n 不能为负: " + n);
        parent = new int[n];
        size = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i] = 1;
        }
        components = n;
    }

    /* ---------------- step-mode ---------------- */

    public void attachTracer(Tracer t) { this.tracer = t; }

    public Object snapshot() {
        return Snaps.unionFind(parent, size, true);
    }

    /* ---------------- 核心操作 ---------------- */

    public int components() { return components; }

    public int sizeOf(int x) {
        return size[find(x)];
    }

    /** 查找根 + 路径压缩。 */
    public int find(int x) {
        check(x);
        int root = x;
        while (parent[root] != root) root = parent[root];
        // 路径压缩：沿途节点直接挂到根
        while (parent[x] != root) {
            int next = parent[x];
            parent[x] = root;
            x = next;
        }
        return root;
    }

    /** 合并两个集合；返回是否发生了合并（已同集返回 false）。 */
    public boolean union(int a, int b) {
        check(a);
        check(b);
        Tracer.Step s = null;
        if (tracer != null) s = tracer.step("union").arg("a", a).arg("b", b).before(snapshot());
        int ra = find(a), rb = find(b);
        if (ra == rb) {
            if (s != null) s.note("已在同一集合").after(snapshot()).commit();
            return false;
        }
        // 按大小合并：小树挂到大树
        if (size[ra] < size[rb]) {
            int t = ra;
            ra = rb;
            rb = t;
        }
        parent[rb] = ra;
        size[ra] += size[rb];
        components--;
        if (s != null) s.after(snapshot()).commit();
        return true;
    }

    public boolean connected(int a, int b) {
        return find(a) == find(b);
    }

    private void check(int x) {
        if (x < 0 || x >= parent.length)
            throw new IndexOutOfBoundsException("元素越界: " + x + ", n=" + parent.length);
    }

    /* ---------------- 教学辅助：导出森林结构 ---------------- */

    /** 返回 {root: [成员...]}，便于笔记与演示。 */
    public Map<Integer, java.util.List<Integer>> groups() {
        Map<Integer, List<Integer>> g = new HashMap<>();
        for (int i = 0; i < parent.length; i++) g.computeIfAbsent(find(i), k -> new ArrayList<>()).add(i);
        return g;
    }
}
