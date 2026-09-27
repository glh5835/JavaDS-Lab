package com.javadslab.trace;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 快速排序优化版（三数取中 + 三路分区）的 trace 演示。
 * 核心实现见 core.sort.QuickSortOptimized；本类为可视化逐步记录分区扫描。
 */
public final class TraceableQuickSort3Way {

    private TraceableQuickSort3Way() {}

    public static Tracer sort(int[] a) {
        Tracer t = new Tracer("array", "快排优化版（三数取中 + 三路分区）");
        t.meta("algorithm", "quick-3way").meta("input", a);
        quickRec(a, 0, a.length - 1, t);
        t.step("done").before(Snaps.arrayPlain("数组", a)).after(Snaps.arrayPlain("数组", a)).commit();
        return t;
    }

    private static void quickRec(int[] a, int lo, int hi, Tracer t) {
        if (lo >= hi) return;
        int pivot = medianOfThree(a, lo, lo + (hi - lo) / 2, hi);
        t.step("median-of-three").arg("range", lo + ".." + hi).arg("pivot", pivot)
                .before(Snaps.arrayMarked("数组", a, mapOf(lo, "cmp", lo + (hi - lo) / 2, "cmp", hi, "pivot")))
                .after(Snaps.arrayMarked("数组", a, mapOf(lo, "cmp", lo + (hi - lo) / 2, "cmp", hi, "pivot")))
                .commit();
        int lt = lo, gt = hi, i = lo;
        while (i <= gt) {
            String mark = a[i] == pivot ? "pivot" : (a[i] < pivot ? "swap" : "cmp");
            Map<Integer, String> marks = mapOf(lt, "cmp", gt, "cmp", i, mark);
            t.step("scan").arg("i", i).arg("a[i]", a[i]).arg("pivot", pivot)
                    .arg("zone", a[i] < pivot ? "<" : (a[i] > pivot ? ">" : "=="))
                    .before(Snaps.arrayMarked("数组", a, marks))
                    .after(Snaps.arrayMarked("数组", a, marks))
                    .hl("active", i).commit();
            if (a[i] < pivot) {
                swap(a, i++, lt++);
            } else if (a[i] > pivot) {
                swap(a, i, gt--);
            } else {
                i++;
            }
        }
        t.step("partition-done").arg("lt", lt).arg("gt", gt).arg("pivot", pivot)
                .before(Snaps.arrayMarked("数组", a, rangeMap(lt, gt)))
                .after(Snaps.arrayMarked("数组", a, rangeMap(lt, gt)))
                .commit();
        quickRec(a, lo, lt - 1, t);
        quickRec(a, gt + 1, hi, t);
    }

    static int medianOfThree(int[] a, int i, int j, int k) {
        int x = a[i], y = a[j], z = a[k];
        if ((x >= y && x <= z) || (x <= y && x >= z)) return x;
        if ((y >= x && y <= z) || (y <= x && y >= z)) return y;
        return z;
    }

    private static void swap(int[] a, int i, int j) {
        int tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    private static Map<Integer, String> mapOf(Object... kv) {
        Map<Integer, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((Integer) kv[i], (String) kv[i + 1]);
        return m;
    }

    private static Map<Integer, String> rangeMap(int lt, int gt) {
        Map<Integer, String> m = new LinkedHashMap<>();
        for (int k = lt; k <= gt; k++) m.put(k, "pivot");
        return m;
    }
}
