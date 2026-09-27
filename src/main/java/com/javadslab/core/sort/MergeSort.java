package com.javadslab.core.sort;

import java.util.Comparator;

/**
 * 手写归并排序（泛型 + int[] 两套）。
 * 稳定排序：相等元素保持相对次序。时间 O(n log n) 恒定，空间 O(n)。
 * 供 Kruskal 等模块复用（替代 java.util 的排序）。
 */
public final class MergeSort {

    private MergeSort() {}

    /** 泛型归并排序（稳定）。 */
    public static <T> void sort(T[] a, Comparator<? super T> cmp) {
        if (a == null || a.length < 2) return;
        @SuppressWarnings("unchecked")
        T[] buf = (T[]) new Object[a.length];
        sortRec(a, buf, 0, a.length - 1, cmp);
    }

    private static <T> void sortRec(T[] a, T[] buf, int lo, int hi, Comparator<? super T> cmp) {
        if (lo >= hi) return;
        int mid = (lo + hi) >>> 1;
        sortRec(a, buf, lo, mid, cmp);
        sortRec(a, buf, mid + 1, hi, cmp);
        merge(a, buf, lo, mid, hi, cmp);
    }

    private static <T> void merge(T[] a, T[] buf, int lo, int mid, int hi, Comparator<? super T> cmp) {
        System.arraycopy(a, lo, buf, lo, hi - lo + 1);
        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) a[k] = buf[j++];
            else if (j > hi) a[k] = buf[i++];
            else if (cmp.compare(buf[i], buf[j]) <= 0) a[k] = buf[i++]; // <= 保证稳定
            else a[k] = buf[j++];
        }
    }

    /** int[] 归并排序（供排序教学 + 堆外场景）。 */
    public static int[] sort(int[] a) {
        int[] out = a.clone();
        if (out.length < 2) return out;
        int[] buf = new int[out.length];
        sortRec(out, buf, 0, out.length - 1);
        return out;
    }

    private static void sortRec(int[] a, int[] buf, int lo, int hi) {
        if (lo >= hi) return;
        int mid = (lo + hi) >>> 1;
        sortRec(a, buf, lo, mid);
        sortRec(a, buf, mid + 1, hi);
        merge(a, buf, lo, mid, hi);
    }

    private static void merge(int[] a, int[] buf, int lo, int mid, int hi) {
        System.arraycopy(a, lo, buf, lo, hi - lo + 1);
        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) a[k] = buf[j++];
            else if (j > hi) a[k] = buf[i++];
            else if (buf[i] <= buf[j]) a[k] = buf[i++];
            else a[k] = buf[j++];
        }
    }
}
