package com.javadslab.core.tree;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.List;

/**
 * 手写线段树（区间和版本，递归实现，4n 存储）。
 * build O(n)；单点赋值 update O(log n)；区间和 query O(log n)。
 * step-mode：build/update/query 记录 trace（快照为「线段覆盖树」，节点标注 [l,r] 与区间和）。
 */
public class SegmentTree {

    private final int n;
    private final int[] sum;   // 每个节点维护区间的和
    private Tracer tracer;

    public SegmentTree(int[] values) {
        if (values == null || values.length == 0) throw new IllegalArgumentException("数据不能为空");
        this.n = values.length;
        this.sum = new int[4 * n];
        build(1, 0, n - 1, values);
    }

    /* ---------------- step-mode ---------------- */

    public void attachTracer(Tracer t) { this.tracer = t; }

    /** 线段树快照：key="[l,r]"，value=区间和， hl 通过单独参数在 step 上给。 */
    public Object snapshot(int highlightNode) {
        return Snaps.tree(nodeSnap(1, highlightNode), "SegmentTree(n=" + n + ")");
    }

    public Object snapshot() { return snapshot(0); }

    private Object nodeSnap(int node, int hl) {
        return nodeSnapRec(node, 0, n - 1, hl);
    }

    private Object nodeSnapRec(int node, int lo, int hi, int hl) {
        List<Object> children = List.of();
        if (lo != hi) {
            int mid = (lo + hi) >>> 1;
            children = List.of(
                    nodeSnapRec(2 * node, lo, mid, hl),
                    nodeSnapRec(2 * node + 1, mid + 1, hi, hl));
        }
        return Snaps.treeNode(List.of("[" + lo + "," + hi + "]=" + sum[node]), children,
                null, node == hl ? 1 : 0);
    }

    private Tracer.Step begin(String op) {
        if (tracer == null) return Tracer.inert();
        return tracer.step(op).before(snapshot());
    }

    private void commit(Tracer.Step s, int hlNode, String note) {
        if (tracer == null) return;
        s.hl("node", hlNode);
        if (note != null) s.note(note);
        s.after(snapshot(hlNode)).commit();
    }

    /* ---------------- 核心操作 ---------------- */

    private void build(int node, int lo, int hi, int[] values) {
        if (lo == hi) {
            sum[node] = values[lo];
            return;
        }
        int mid = (lo + hi) >>> 1;
        build(2 * node, lo, mid, values);
        build(2 * node + 1, mid + 1, hi, values);
        pull(node);
    }

    private void pull(int node) {
        sum[node] = sum[2 * node] + sum[2 * node + 1];
    }

    /** 单点赋值：a[index] = value。O(log n)。 */
    public void update(int index, int value) {
        checkIndex(index);
        Tracer.Step s = begin("update").arg("index", index).arg("value", value);
        updateRec(1, 0, n - 1, index, value);
        commit(s, 1, "根节点区间和已更新");
    }

    private void updateRec(int node, int lo, int hi, int index, int value) {
        if (lo == hi) {
            sum[node] = value;
            if (tracer != null) {
                tracer.step("update-leaf").arg("index", index).arg("value", value)
                        .before(snapshot(node)).after(snapshot(node)).hl("node", node).commit();
            }
            return;
        }
        int mid = (lo + hi) >>> 1;
        if (index <= mid) updateRec(2 * node, lo, mid, index, value);
        else updateRec(2 * node + 1, mid + 1, hi, index, value);
        pull(node);
        if (tracer != null) {
            tracer.step("pull-up").arg("node", node).arg("lo", lo).arg("hi", hi).arg("sum", sum[node])
                    .before(snapshot(node)).after(snapshot(node)).hl("node", node).commit();
        }
    }

    /** 闭区间 [l, r] 的区间和。O(log n)。 */
    public int query(int l, int r) {
        checkIndex(l);
        checkIndex(r);
        if (l > r) throw new IllegalArgumentException("l > r: " + l + " > " + r);
        Tracer.Step s = begin("query").arg("l", l).arg("r", r);
        int result = queryRec(1, 0, n - 1, l, r);
        commit(s, 1, "查询结果=" + result);
        return result;
    }

    private int queryRec(int node, int lo, int hi, int l, int r) {
        if (l <= lo && hi <= r) {
            if (tracer != null) {
                tracer.step("query-hit").arg("node", node).arg("seg", "[" + lo + "," + hi + "]").arg("sum", sum[node])
                        .before(snapshot(node)).after(snapshot(node)).hl("node", node).commit();
            }
            return sum[node];
        }
        int mid = (lo + hi) >>> 1;
        int total = 0;
        if (l <= mid) total += queryRec(2 * node, lo, mid, l, r);
        if (r > mid) total += queryRec(2 * node + 1, mid + 1, hi, l, r);
        return total;
    }

    private void checkIndex(int i) {
        if (i < 0 || i >= n) throw new IndexOutOfBoundsException("下标越界: " + i + ", n=" + n);
    }

    /** 原数组快照（数组视图），便于播放器同时展示。 */
    public Object arraySnapshot() {
        int[] arr = new int[n];
        fillArray(1, 0, n - 1, arr);
        return Snaps.arrayPlain("原数组", arr);
    }

    private void fillArray(int node, int lo, int hi, int[] out) {
        if (lo == hi) {
            out[lo] = sum[node];
            return;
        }
        int mid = (lo + hi) >>> 1;
        fillArray(2 * node, lo, mid, out);
        fillArray(2 * node + 1, mid + 1, hi, out);
    }
}
