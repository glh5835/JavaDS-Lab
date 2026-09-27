package com.javadslab.core.stackqueue;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.ArrayList;
import java.util.List;

/**
 * 单调栈：解决「下一个更大元素」一类问题。
 * 栈内保存下标，对应值从栈底到栈顶严格递减；遇到更大值即弹出并结算答案。整体 O(n)。
 * step-mode：visit / pop-settle / push / finish 均记录 trace（用于动画演示）。
 * 结果数组约定：-2=未结算，-1=右侧无更大元素，>=0=下一个更大元素的下标。
 */
public class MonotonicStack {

    public static final int UNKNOWN = -2;
    public static final int NONE = -1;

    private Tracer tracer;

    public void attachTracer(Tracer t) { this.tracer = t; }

    /** 返回 res[i] = a 中 i 右侧第一个比 a[i] 大的元素下标，不存在为 -1。O(n)。 */
    public int[] nextGreaterIndex(int[] a) {
        int n = a.length;
        int[] res = new int[n];
        java.util.Arrays.fill(res, UNKNOWN);
        int[] stack = new int[n]; // 手写 int 栈，存下标
        int top = 0;
        if (tracer != null) tracer.meta("input", a);

        for (int i = 0; i < n; i++) {
            Tracer.Step s = tracer == null ? null
                    : tracer.step("visit").arg("i", i).arg("value", a[i])
                            .before(snapshot(a, stack, top, res)).hl("active", i);
            // 弹出所有比当前值小的下标并结算
            while (top > 0 && a[stack[top - 1]] < a[i]) {
                int j = stack[--top];
                res[j] = i;
                if (tracer != null) {
                    tracer.step("pop-settle").arg("popped", j).arg("settledTo", i)
                            .before(snapshot(a, stack, top, res))
                            .after(snapshot(a, stack, top, res))
                            .hl("active", i).hl("settled", j).commit();
                }
            }
            stack[top++] = i; // 入栈
            if (tracer != null) {
                if (s != null) s.after(snapshot(a, stack, top, res)).commit();
                else tracer.step("push").after(snapshot(a, stack, top, res)).hl("active", i).commit();
            }
        }
        while (top > 0) res[stack[--top]] = NONE;
        if (tracer != null) {
            tracer.step("finish").before(snapshot(a, new int[0], 0, res)).after(snapshot(a, new int[0], 0, res)).commit();
        }
        return res;
    }

    /** 便捷版本：返回值本身（无更大元素为 -1）。 */
    public int[] nextGreaterValue(int[] a) {
        int[] idx = nextGreaterIndex(a);
        int[] vals = new int[idx.length];
        for (int i = 0; i < idx.length; i++) vals[i] = idx[i] < 0 ? -1 : a[idx[i]];
        return vals;
    }

    /** 快照：原数组 + 结果数组 + 栈内容（下标:值）。 */
    private Object snapshot(int[] a, int[] stack, int top, int[] res) {
        List<Object> resCells = new ArrayList<>(a.length);
        for (int r : res) resCells.add(Snaps.cell(r == UNKNOWN ? "?" : r, r == UNKNOWN ? "unknown" : null));
        List<Object> stackCells = new ArrayList<>();
        for (int k = 0; k < top; k++) stackCells.add(Snaps.cell(stack[k] + ":" + a[stack[k]], "inStack"));
        return Snaps.obj("kind", "monostack",
                "array", Snaps.arrayPlain("数组", a),
                "res", Snaps.array("res（下一个更大元素下标）", resCells),
                "stack", stackCells);
    }
}
