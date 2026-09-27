package com.javadslab.core.tree;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.ArrayList;
import java.util.List;

/**
 * 手写树状数组（Binary Indexed Tree / Fenwick Tree），1-based。
 * lowbit(i) = i & (-i)；update O(log n) 单点加，prefixSum O(log n) 前缀和，rangeSum 区间和。
 * step-mode：update/prefixSum 记录 trace（快照展示数组与当前跳跃下标）。
 */
public class FenwickTree {

    private final int n;
    private final int[] tree;
    private Tracer tracer;

    public FenwickTree(int n) {
        if (n <= 0) throw new IllegalArgumentException("n 必须为正: " + n);
        this.n = n;
        this.tree = new int[n + 1];
    }

    /** O(n) 建树：先逐点 add，或线性法 tree[i] += tree[i-1] 后上溯。这里用逐点（清晰）。 */
    public FenwickTree(int[] values) {
        this(values.length);
        for (int i = 0; i < values.length; i++) addRaw(i + 1, values[i]);
    }

    /* ---------------- step-mode ---------------- */

    public void attachTracer(Tracer t) { this.tracer = t; }

    public Object snapshot(Integer activeIdx) {
        List<Object> cells = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            String mark = null;
            if (activeIdx != null && activeIdx == i) mark = "active";
            cells.add(Snaps.cell(tree[i], mark));
        }
        return Snaps.array("Fenwick(n=" + n + ", 1-based)", cells);
    }

    public Object snapshot() { return snapshot(null); }

    private Tracer.Step begin(String op) {
        if (tracer == null) return Tracer.inert();
        return tracer.step(op).before(snapshot());
    }

    private void commit(Tracer.Step s, Integer activeIdx) {
        if (tracer == null) return;
        s.after(snapshot(activeIdx)).commit();
    }

    /* ---------------- 核心操作 ---------------- */

    private static int lowbit(int i) {
        return i & (-i);
    }

    /** 单点加 delta（含负值）。O(log n)。 */
    public void add(int index1, int delta) {
        checkIndex(index1);
        Tracer.Step s = begin("add").arg("index", index1).arg("delta", delta);
        int i = index1;
        while (i <= n) {
            tree[i] += delta;
            if (tracer != null) {
                tracer.step("add-hop").arg("at", i).arg("value", tree[i])
                        .before(snapshot(i)).after(snapshot(i)).hl("active", i).commit();
            }
            i += lowbit(i);
        }
        commit(s, index1);
    }

    /** 前缀和 prefix[1..index1]。O(log n)。 */
    public int prefixSum(int index1) {
        checkIndex(index1);
        Tracer.Step s = begin("prefixSum").arg("index", index1);
        int total = 0;
        int i = index1;
        while (i > 0) {
            total += tree[i];
            if (tracer != null) {
                tracer.step("sum-hop").arg("at", i).arg("grabbed", tree[i]).arg("total", total)
                        .before(snapshot(i)).after(snapshot(i)).hl("active", i).commit();
            }
            i -= lowbit(i);
        }
        commit(s, null);
        return total;
    }

    /** 区间和 [l, r]（1-based 闭区间）。 */
    public int rangeSum(int l, int r) {
        checkIndex(l);
        checkIndex(r);
        if (l > r) throw new IllegalArgumentException("l > r: " + l + " > " + r);
        int left = l == 1 ? 0 : prefixSum(l - 1);
        return prefixSum(r) - left;
    }

    /** 单点值 = prefixSum(i) - prefixSum(i-1)。 */
    public int pointValue(int index1) {
        checkIndex(index1);
        return prefixSum(index1) - (index1 == 1 ? 0 : prefixSum(index1 - 1));
    }

    private void addRaw(int index1, int delta) {
        while (index1 <= n) {
            tree[index1] += delta;
            index1 += lowbit(index1);
        }
    }

    private void checkIndex(int i) {
        if (i < 1 || i > n) throw new IndexOutOfBoundsException("1-based 下标越界: " + i + ", n=" + n);
    }
}
