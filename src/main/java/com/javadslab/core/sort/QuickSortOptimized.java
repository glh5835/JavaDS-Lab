package com.javadslab.core.sort;

/**
 * 快速排序分区优化集：
 * ①三数取中（median-of-three）选基准，防有序输入退化；
 * ②小区间（<=16）切换插入排序，减少递归开销；
 * ③三路分区（Dutch Flag），重复元素多的数组从 O(n^2) 降为 O(n log k)；
 * ④尾递归消除：较大的一侧用循环，控制栈深 O(log n)。
 */
public final class QuickSortOptimized {

    private static final int INSERTION_THRESHOLD = 16;

    private QuickSortOptimized() {}

    public static void sort(int[] a) {
        quickRec(a, 0, a.length - 1);
    }

    private static void quickRec(int[] a, int lo, int hi) {
        while (lo < hi) {
            if (hi - lo + 1 <= INSERTION_THRESHOLD) {
                SortingAlgorithms.insertionSort(a, lo, hi);
                return;
            }
            // 三路分区：[lo, lt-1] < pivot, [lt, gt] == pivot, [gt+1, hi] > pivot
            int pivot = medianOfThree(a, lo, lo + (hi - lo) / 2, hi);
            int lt = lo, gt = hi, i = lo;
            while (i <= gt) {
                if (a[i] < pivot) SortingAlgorithms.swap(a, i++, lt++);
                else if (a[i] > pivot) SortingAlgorithms.swap(a, i, gt--);
                else i++;
            }
            // 尾递归消除：先递归较小一侧
            if (lt - lo < hi - gt) {
                quickRec(a, lo, lt - 1);
                lo = gt + 1;
            } else {
                quickRec(a, gt + 1, hi);
                hi = lt - 1;
            }
        }
    }

    /** 三数取中：取 lo/mid/hi 的中位数“值”作为基准。 */
    static int medianOfThree(int[] a, int i, int j, int k) {
        int x = a[i], y = a[j], z = a[k];
        if ((x >= y && x <= z) || (x <= y && x >= z)) return x;
        if ((y >= x && y <= z) || (y <= x && y >= z)) return y;
        return z;
    }
}
