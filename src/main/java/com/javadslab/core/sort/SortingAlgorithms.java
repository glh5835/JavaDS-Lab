package com.javadslab.core.sort;

/**
 * 十种排序（全部手写，整型数组，原地升序；counting/radix/bucket 需要辅助空间）。
 * 每种算法在类中独立成方法，复杂度与稳定性见各方法注释。
 */
public final class SortingAlgorithms {

    private SortingAlgorithms() {}

    /* 1. 冒泡：O(n^2) / O(1) / 稳定。提前退出优化。 */
    public static void bubbleSort(int[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < a.length - 1 - i; j++) {
                if (a[j] > a[j + 1]) {
                    swap(a, j, j + 1);
                    swapped = true;
                }
            }
            if (!swapped) break; // 已有序
        }
    }

    /* 2. 选择：O(n^2) / O(1) / 不稳定。 */
    public static void selectionSort(int[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            int min = i;
            for (int j = i + 1; j < a.length; j++) {
                if (a[j] < a[min]) min = j;
            }
            if (min != i) swap(a, i, min);
        }
    }

    /* 3. 插入：O(n^2) 最坏，O(n) 近乎有序 / O(1) / 稳定。 */
    public static void insertionSort(int[] a) {
        insertionSort(a, 0, a.length - 1);
    }

    /** 区间版插入排序（快排小区间优化复用）。 */
    public static void insertionSort(int[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            int key = a[i];
            int j = i - 1;
            while (j >= lo && a[j] > key) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = key;
        }
    }

    /* 4. 希尔：O(n^1.3) 平均（依赖增量序列）/ O(1) / 不稳定。 */
    public static void shellSort(int[] a) {
        int n = a.length;
        for (int gap = n / 2; gap > 0; gap /= 2) {
            for (int i = gap; i < n; i++) {
                int key = a[i];
                int j = i - gap;
                while (j >= 0 && a[j] > key) {
                    a[j + gap] = a[j];
                    j -= gap;
                }
                a[j + gap] = key;
            }
        }
    }

    /* 5. 归并：O(n log n) / O(n) / 稳定。 */
    public static int[] mergeSort(int[] a) {
        return MergeSort.sort(a);
    }

    /* 6. 快速（基础版：两端扫描分区 + 随机基准防退化）。 */
    public static void quickSort(int[] a) {
        quickRec(a, 0, a.length - 1);
    }

    private static void quickRec(int[] a, int lo, int hi) {
        if (lo >= hi) return;
        int p = partition(a, lo, hi);
        quickRec(a, lo, p - 1);
        quickRec(a, p + 1, hi);
    }

    /** Lomuto 分区（随机基准）：返回基准最终位置。 */
    static int partition(int[] a, int lo, int hi) {
        int pivotIdx = lo + java.util.concurrent.ThreadLocalRandom.current().nextInt(hi - lo + 1);
        swap(a, pivotIdx, hi);
        int pivot = a[hi];
        int i = lo; // 小于区的下一个位置
        for (int j = lo; j < hi; j++) {
            if (a[j] < pivot) swap(a, i++, j);
        }
        swap(a, i, hi);
        return i;
    }

    /* 7. 堆排：O(n log n) / O(1) / 不稳定。 */
    public static void heapSort(int[] a) {
        int n = a.length;
        for (int i = n / 2 - 1; i >= 0; i--) siftDown(a, i, n);   // 建大顶堆
        for (int end = n - 1; end > 0; end--) {
            swap(a, 0, end);
            siftDown(a, 0, end);
        }
    }

    private static void siftDown(int[] a, int i, int size) {
        while (true) {
            int l = 2 * i + 1, r = 2 * i + 2, largest = i;
            if (l < size && a[l] > a[largest]) largest = l;
            if (r < size && a[r] > a[largest]) largest = r;
            if (largest == i) return;
            swap(a, i, largest);
            i = largest;
        }
    }

    /* 8. 计数：O(n+k) / O(k) / 稳定（要求非负小范围）。 */
    public static void countingSort(int[] a) {
        if (a.length == 0) return;
        int max = a[0];
        for (int v : a) max = Math.max(max, v);
        int[] cnt = new int[max + 1];
        for (int v : a) cnt[v]++;
        int idx = 0;
        for (int v = 0; v <= max; v++) {
            while (cnt[v]-- > 0) a[idx++] = v;
        }
    }

    /* 9. 基数（LSD，非负整数）：O(d(n+10)) / O(n) / 稳定。 */
    public static void radixSort(int[] a) {
        if (a.length == 0) return;
        int max = a[0];
        for (int v : a) max = Math.max(max, v);
        int[] buf = new int[a.length];
        for (int exp = 1; max / exp > 0; exp *= 10) {
            int[] cnt = new int[10];
            for (int v : a) cnt[(v / exp) % 10]++;
            for (int d = 1; d < 10; d++) cnt[d] += cnt[d - 1]; // 前缀和定序
            for (int i = a.length - 1; i >= 0; i--) {          // 逆序保证稳定
                int d = (a[i] / exp) % 10;
                buf[--cnt[d]] = a[i];
            }
            System.arraycopy(buf, 0, a, 0, a.length);
        }
    }

    /* 10. 桶排（值域均匀时 O(n)）：分桶 + 各桶插入排序。 */
    public static void bucketSort(int[] a) {
        if (a.length == 0) return;
        int min = a[0], max = a[0];
        for (int v : a) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        if (min == max) return;
        int buckets = Math.max(1, a.length / 4);
        @SuppressWarnings("unchecked")
        java.util.List<Integer>[] bs = new java.util.List[buckets];
        for (int v : a) {
            int idx = (int) ((long) (v - min) * (buckets - 1) / (max - min));
            if (bs[idx] == null) bs[idx] = new java.util.ArrayList<>();
            bs[idx].add(v);
        }
        int idx = 0;
        for (java.util.List<Integer> b : bs) {
            if (b == null) continue;
            int[] arr = b.stream().mapToInt(Integer::intValue).toArray();
            insertionSort(arr);
            for (int v : arr) a[idx++] = v;
        }
    }

    /* ---------------- 工具 ---------------- */

    static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
