package com.javadslab.trace;

/**
 * 带 trace 的排序演示（覆盖代表性算法；完整实现与测试见 core.sort 包）。
 * 每次比较/交换/写回记录一步，快照为高亮数组。
 */
public final class TraceableSorts {

    private TraceableSorts() {}

    public static Tracer bubbleSort(int[] a) {
        Tracer t = new Tracer("array", "冒泡排序");
        t.meta("algorithm", "bubble").meta("input", a);
        bubbleRec(a, t);
        return t;
    }

    private static void bubbleRec(int[] a, Tracer t) {
        for (int i = 0; i < a.length - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < a.length - 1 - i; j++) {
                t.step("compare").arg("i", j).arg("j", j + 1)
                        .before(Snaps.arrayMarked("数组", a, mapOf(j, "cmp", j + 1, "cmp")))
                        .after(Snaps.arrayMarked("数组", a, mapOf(j, "cmp", j + 1, "cmp")))
                        .hl("compare", java.util.List.of(j, j + 1)).commit();
                if (a[j] > a[j + 1]) {
                    SortingHelper.swap(a, j, j + 1);
                    t.step("swap").arg("i", j).arg("j", j + 1)
                            .before(Snaps.arrayMarked("数组", a, mapOf(j, "swap", j + 1, "swap")))
                            .after(Snaps.arrayMarked("数组", a, mapOf(j, "swap", j + 1, "swap")))
                            .hl("swapped", java.util.List.of(j, j + 1)).commit();
                    swapped = true;
                }
            }
            t.step("mark-sorted").arg("index", a.length - 1 - i)
                    .before(Snaps.arrayMarked("数组", a, mapOf(a.length - 1 - i, "sorted")))
                    .after(Snaps.arrayMarked("数组", a, mapOf(a.length - 1 - i, "sorted")))
                    .commit();
            if (!swapped) break;
        }
    }

    public static Tracer insertionSort(int[] a) {
        Tracer t = new Tracer("array", "插入排序");
        t.meta("algorithm", "insertion").meta("input", a);
        for (int i = 1; i < a.length; i++) {
            int key = a[i];
            int j = i - 1;
            t.step("pick-key").arg("i", i).arg("key", key)
                    .before(Snaps.arrayMarked("数组", a, mapOf(i, "pivot")))
                    .after(Snaps.arrayMarked("数组", a, mapOf(i, "pivot")))
                    .hl("key", i).commit();
            while (j >= 0 && a[j] > key) {
                a[j + 1] = a[j];
                t.step("shift").arg("from", j).arg("to", j + 1)
                        .before(Snaps.arrayMarked("数组", a, mapOf(j, "cmp", j + 1, "swap")))
                        .after(Snaps.arrayMarked("数组", a, mapOf(j, "cmp", j + 1, "swap")))
                        .commit();
                j--;
            }
            a[j + 1] = key;
            t.step("place-key").arg("pos", j + 1)
                    .before(Snaps.arrayMarked("数组", a, mapOf(j + 1, "swap")))
                    .after(Snaps.arrayMarked("数组", a, mapOf(j + 1, "sorted")))
                    .commit();
        }
        return t;
    }

    public static Tracer quickSort(int[] a) {
        Tracer t = new Tracer("array", "快速排序（Lomuto 分区）");
        t.meta("algorithm", "quick").meta("input", a);
        quickRec(a, 0, a.length - 1, t);
        return t;
    }

    private static void quickRec(int[] a, int lo, int hi, Tracer t) {
        if (lo >= hi) return;
        int pivot = a[hi];
        t.step("choose-pivot").arg("pivot", pivot).arg("range", lo + ".." + hi)
                .before(Snaps.arrayMarked("数组", a, mapOf(hi, "pivot")))
                .after(Snaps.arrayMarked("数组", a, mapOf(hi, "pivot")))
                .hl("pivot", hi).commit();
        int i = lo;
        for (int j = lo; j < hi; j++) {
            t.step("compare").arg("j", j).arg("pivot", pivot)
                    .before(Snaps.arrayMarked("数组", a, mapOf(j, "cmp", hi, "pivot")))
                    .after(Snaps.arrayMarked("数组", a, mapOf(j, "cmp", hi, "pivot")))
                    .hl("compare", java.util.List.of(j, hi)).commit();
            if (a[j] < pivot) {
                SortingHelper.swap(a, i, j);
                t.step("swap-left").arg("i", i).arg("j", j)
                        .before(Snaps.arrayMarked("数组", a, mapOf(i, "swap", j, "swap", hi, "pivot")))
                        .after(Snaps.arrayMarked("数组", a, mapOf(i, "swap", j, "swap", hi, "pivot")))
                        .commit();
                i++;
            }
        }
        SortingHelper.swap(a, i, hi);
        t.step("pivot-settle").arg("pos", i).arg("pivot", pivot)
                .before(Snaps.arrayMarked("数组", a, mapOf(i, "swap", hi, "pivot")))
                .after(Snaps.arrayMarked("数组", a, mapOf(i, "sorted")))
                .commit();
        quickRec(a, lo, i - 1, t);
        quickRec(a, i + 1, hi, t);
    }

    public static Tracer selectionSort(int[] a) {
        Tracer t = new Tracer("array", "选择排序");
        t.meta("algorithm", "selection").meta("input", a);
        for (int i = 0; i < a.length - 1; i++) {
            int min = i;
            for (int j = i + 1; j < a.length; j++) {
                t.step("compare").arg("min", min).arg("j", j)
                        .before(Snaps.arrayMarked("数组", a, mapOf(min, "cmp", j, "cmp")))
                        .after(Snaps.arrayMarked("数组", a, mapOf(min, "cmp", j, "cmp")))
                        .commit();
                if (a[j] < a[min]) min = j;
            }
            if (min != i) SortingHelper.swap(a, i, min);
            t.step("settle").arg("pos", i)
                    .before(Snaps.arrayMarked("数组", a, mapOf(i, "swap", min, "cmp")))
                    .after(Snaps.arrayMarked("数组", a, mapOf(i, "sorted")))
                    .commit();
        }
        return t;
    }

    public static Tracer heapSort(int[] a) {
        Tracer t = new Tracer("heap", "堆排序");
        t.meta("algorithm", "heap").meta("input", a);
        int n = a.length;
        for (int i = n / 2 - 1; i >= 0; i--) siftDown(a, i, n, t);
        for (int end = n - 1; end > 0; end--) {
            SortingHelper.swap(a, 0, end);
            t.step("move-max").arg("from", 0).arg("to", end)
                    .before(Snaps.heap(a, n, mapOf(0, "swap", end, "sorted")))
                    .after(Snaps.heap(a, n, mapOf(0, "swap", end, "sorted")))
                    .commit();
            siftDown(a, 0, end, t);
        }
        t.step("done").before(Snaps.heap(a, n, fullMap(n, "sorted"))).after(Snaps.heap(a, n, fullMap(n, "sorted"))).commit();
        return t;
    }

    private static void siftDown(int[] a, int i, int size, Tracer t) {
        while (true) {
            int l = 2 * i + 1, r = 2 * i + 2, largest = i;
            if (l < size && a[l] > a[largest]) largest = l;
            if (r < size && a[r] > a[largest]) largest = r;
            if (largest == i) return;
            SortingHelper.swap(a, i, largest);
            t.step("sift-down").arg("from", i).arg("to", largest)
                    .before(Snaps.heap(a, size, mapOf(i, "swap", largest, "cmp")))
                    .after(Snaps.heap(a, size, mapOf(i, "swap", largest, "cmp")))
                    .hl("swapped", java.util.List.of(i, largest)).commit();
            i = largest;
        }
    }

    /* ---------------- 工具 ---------------- */

    private static java.util.Map<Integer, String> mapOf(Object... kv) {
        java.util.Map<Integer, String> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((Integer) kv[i], (String) kv[i + 1]);
        return m;
    }

    private static java.util.Map<Integer, String> fullMap(int n, String mark) {
        java.util.Map<Integer, String> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i < n; i++) m.put(i, mark);
        return m;
    }

    /** 独立 swap（避免依赖核心包）。 */
    static final class SortingHelper {
        static void swap(int[] a, int i, int j) {
            int tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }
}
