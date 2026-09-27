package com.javadslab.trace;

import com.javadslab.core.sort.SortingAlgorithms;

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

    public static Tracer shellSort(int[] a) {
        Tracer t = new Tracer("array", "希尔排序");
        t.meta("algorithm", "shell").meta("input", a);
        for (int gap = a.length / 2; gap > 0; gap /= 2) {
            for (int i = gap; i < a.length; i++) {
                int key = a[i];
                int j = i - gap;
                t.step("pick").arg("gap", gap).arg("i", i).arg("key", key)
                        .before(Snaps.arrayMarked("数组", a, mapOf(i, "pivot")))
                        .after(Snaps.arrayMarked("数组", a, mapOf(i, "pivot")))
                        .commit();
                while (j >= 0 && a[j] > key) {
                    a[j + gap] = a[j];
                    t.step("shift").arg("gap", gap).arg("from", j).arg("to", j + gap)
                            .before(Snaps.arrayMarked("数组", a, mapOf(j, "cmp", j + gap, "swap")))
                            .after(Snaps.arrayMarked("数组", a, mapOf(j, "cmp", j + gap, "swap")))
                            .commit();
                    j -= gap;
                }
                a[j + gap] = key;
                t.step("place").arg("pos", j + gap)
                        .before(Snaps.arrayMarked("数组", a, mapOf(j + gap, "swap")))
                        .after(Snaps.arrayMarked("数组", a, mapOf(j + gap, "sorted")))
                        .commit();
            }
        }
        return t;
    }

    public static Tracer mergeSort(int[] a) {
        Tracer t = new Tracer("array", "归并排序");
        t.meta("algorithm", "merge").meta("input", a);
        mergeRec(a, new int[a.length], 0, a.length - 1, t);
        return t;
    }

    private static void mergeRec(int[] a, int[] buf, int lo, int hi, Tracer t) {
        if (lo >= hi) return;
        int mid = (lo + hi) >>> 1;
        t.step("split").arg("range", lo + ".." + hi).arg("mid", mid)
                .before(Snaps.arrayMarked("数组", a, mapOf(mid, "pivot")))
                .after(Snaps.arrayMarked("数组", a, mapOf(mid, "pivot")))
                .commit();
        mergeRec(a, buf, lo, mid, t);
        mergeRec(a, buf, mid + 1, hi, t);
        System.arraycopy(a, lo, buf, lo, hi - lo + 1);
        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            int from = (i > mid) ? j : (j > hi ? i : (buf[i] <= buf[j] ? i : j));
            a[k] = buf[from];
            if (from == i) i++;
            else j++;
            t.step("merge-write").arg("pos", k).arg("value", a[k])
                    .before(Snaps.arrayMarked("数组", a, mapOf(k, "swap")))
                    .after(Snaps.arrayMarked("数组", a, mapOf(k, "sorted")))
                    .commit();
        }
    }

    public static Tracer countingSort(int[] a) {
        Tracer t = new Tracer("array", "计数排序");
        t.meta("algorithm", "counting").meta("input", a);
        int max = 0;
        for (int v : a) max = Math.max(max, v);
        int[] cnt = new int[max + 1];
        for (int v : a) {
            cnt[v]++;
            t.step("count").arg("value", v).arg("count", cnt[v])
                    .before(Snaps.arrayPlain("数组", a))
                    .after(Snaps.arrayPlain("数组", a))
                    .commit();
        }
        int idx = 0;
        for (int v = 0; v <= max; v++) {
            while (cnt[v]-- > 0) {
                a[idx] = v;
                t.step("write-back").arg("pos", idx).arg("value", v)
                        .before(Snaps.arrayMarked("数组", a, mapOf(idx, "swap")))
                        .after(Snaps.arrayMarked("数组", a, mapOf(idx, "sorted")))
                        .commit();
                idx++;
            }
        }
        return t;
    }

    public static Tracer radixSort(int[] a) {
        Tracer t = new Tracer("array", "基数排序（LSD）");
        t.meta("algorithm", "radix").meta("input", a);
        int max = 0;
        for (int v : a) max = Math.max(max, v);
        int[] buf = new int[a.length];
        for (int exp = 1; max / exp > 0; exp *= 10) {
            int[] cnt = new int[10];
            for (int v : a) cnt[(v / exp) % 10]++;
            for (int d = 1; d < 10; d++) cnt[d] += cnt[d - 1];
            for (int i = a.length - 1; i >= 0; i--) {
                int d = (a[i] / exp) % 10;
                buf[--cnt[d]] = a[i];
            }
            System.arraycopy(buf, 0, a, 0, a.length);
            t.step("digit-done").arg("exp", exp).arg("digit", exp == 1 ? "个位" : (exp == 10 ? "十位" : "更高位"))
                    .before(Snaps.arrayPlain("数组", a))
                    .after(Snaps.arrayPlain("数组", a)).commit();
        }
        t.step("done").before(Snaps.arrayPlain("数组", a)).after(Snaps.arrayPlain("数组", a)).commit();
        return t;
    }

    public static Tracer bucketSort(int[] a) {
        Tracer t = new Tracer("array", "桶排序");
        t.meta("algorithm", "bucket").meta("input", a);
        int min = a.length == 0 ? 0 : a[0], max = min;
        for (int v : a) { min = Math.min(min, v); max = Math.max(max, v); }
        if (min == max) { t.step("done").before(Snaps.arrayPlain("数组", a)).after(Snaps.arrayPlain("数组", a)).commit(); return t; }
        int buckets = Math.max(1, a.length / 4);
        java.util.List<Integer>[] bs = new java.util.List[buckets];
        for (int v : a) {
            int b = (int) ((long) (v - min) * (buckets - 1) / (max - min));
            if (bs[b] == null) bs[b] = new java.util.ArrayList<>();
            bs[b].add(v);
            t.step("distribute").arg("value", v).arg("bucket", b)
                    .before(Snaps.arrayPlain("数组", a)).after(Snaps.arrayPlain("数组", a)).commit();
        }
        int idx = 0;
        for (java.util.List<Integer> b : bs) {
            if (b == null) continue;
            int[] arr = b.stream().mapToInt(Integer::intValue).toArray();
            SortingAlgorithms.insertionSort(arr);
            for (int v : arr) {
                a[idx] = v;
                t.step("collect").arg("pos", idx).arg("value", v)
                        .before(Snaps.arrayMarked("数组", a, mapOf(idx, "swap")))
                        .after(Snaps.arrayMarked("数组", a, mapOf(idx, "sorted")))
                        .commit();
                idx++;
            }
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
