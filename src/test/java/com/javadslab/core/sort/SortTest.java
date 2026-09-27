package com.javadslab.core.sort;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** 十种排序 + 快排优化：与手写归并排序（MergeSort）随机对照。 */
class SortTest {

    /** 支持任意 int 的排序器（counting/radix 仅支持非负，单独测）。 */
    static final java.util.List<java.util.function.Consumer<int[]>> ALL_SORTERS = java.util.List.of(
            SortingAlgorithms::bubbleSort,
            SortingAlgorithms::selectionSort,
            SortingAlgorithms::insertionSort,
            SortingAlgorithms::shellSort,
            SortingAlgorithms::quickSort,
            SortingAlgorithms::heapSort,
            SortingAlgorithms::bucketSort,
            (int[] a) -> { int[] r = SortingAlgorithms.mergeSort(a); System.arraycopy(r, 0, a, 0, a.length); },
            QuickSortOptimized::sort
    );

    @Test
    void allSortersOnCommonCases() {
        int[][] cases = {
                {},
                {1},
                {2, 1},
                {1, 2},
                {3, 3, 3, 3},
                {5, 4, 3, 2, 1},
                {1, 2, 3, 4, 5},
                {-3, 7, 0, -3, 9, 2},
        };
        for (int[] c : cases) {
            int[] want = MergeSort.sort(c);
            for (java.util.function.Consumer<int[]> sorter : ALL_SORTERS) {
                int[] a = c.clone();
                sorter.accept(a);
                assertArrayEquals(want, a, "输入 " + Arrays.toString(c) + " 排序器 #" + sorter);
            }
        }
    }

    @Test
    void allSortersLargeRandomAgainstMergeSort() {
        Random rnd = new Random(51L);
        for (int trial = 0; trial < 30; trial++) {
            int n = rnd.nextInt(800);
            int[] src = new int[n];
            for (int i = 0; i < n; i++) src[i] = rnd.nextInt(1000) - 500;
            int[] want = MergeSort.sort(src);
            for (java.util.function.Consumer<int[]> sorter : ALL_SORTERS) {
                int[] a = src.clone();
                sorter.accept(a);
                assertArrayEquals(want, a, "排序器 " + sorter + " trial " + trial);
            }
        }
    }

    @Test
    void countingAndRadixRejectNegativeSemantics() {
        // countingSort/radixSort 设计上只支持非负整数（教学实现约定）
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> SortingAlgorithms.countingSort(new int[]{-1, 2}));
        assertDoesNotThrow(() -> SortingAlgorithms.radixSort(new int[]{170, 45, 75, 90, 802, 24, 2, 66}));
        int[] a = {170, 45, 75, 90, 802, 24, 2, 66};
        SortingAlgorithms.radixSort(a);
        assertArrayEquals(new int[]{2, 24, 45, 66, 75, 90, 170, 802}, a);
    }

    @Test
    void quickSortDegenerateInputs() {
        // 已有序 / 全相同 / 逆序：基础快排用随机基准也应正确
        int[] sorted = new int[5_000];
        for (int i = 0; i < sorted.length; i++) sorted[i] = i;
        int[] a1 = sorted.clone();
        SortingAlgorithms.quickSort(a1);
        assertArrayEquals(sorted, a1);

        int[] same = new int[5_000];
        Arrays.fill(same, 7);
        int[] a2 = same.clone();
        SortingAlgorithms.quickSort(a2);
        assertArrayEquals(same, a2);

        int[] rev = new int[5_000];
        for (int i = 0; i < rev.length; i++) rev[i] = rev.length - 1 - i; // 4999..0，逆序
        int[] a3 = rev.clone();
        SortingAlgorithms.quickSort(a3);
        assertArrayEquals(sorted, a3);
    }

    @Test
    void quickSortOptimizedHandlesDuplicatesFast() {
        // 全相同的 10 万元素：三路分区应线性完成（若退化 O(n^2) 会超时暴露）
        int[] a = new int[100_000];
        Arrays.fill(a, 42);
        long start = System.nanoTime();
        QuickSortOptimized.sort(a);
        long ms = (System.nanoTime() - start) / 1_000_000;
        for (int v : a) assertEquals(42, v);
        assertTrue(ms < 2000, "三路分区处理全相同数组应远快于 O(n^2)，实际 " + ms + "ms");

        Random rnd = new Random(52L);
        for (int trial = 0; trial < 20; trial++) {
            int[] b = new int[rnd.nextInt(5_000)];
            for (int i = 0; i < b.length; i++) b[i] = rnd.nextInt(5); // 大量重复
            int[] want = MergeSort.sort(b);
            QuickSortOptimized.sort(b);
            assertArrayEquals(want, b, "trial " + trial);
        }
    }

    @Test
    void medianOfThree() {
        assertEquals(2, QuickSortOptimized.medianOfThree(new int[]{1, 2, 3}, 0, 1, 2));
        assertEquals(2, QuickSortOptimized.medianOfThree(new int[]{3, 2, 1}, 0, 1, 2));
        assertEquals(5, QuickSortOptimized.medianOfThree(new int[]{5, 5, 5}, 0, 1, 2));
        assertEquals(7, QuickSortOptimized.medianOfThree(new int[]{3, 9, 7}, 0, 1, 2));
    }

    @Test
    void countingAndRadixOnNonNegative() {
        // 计数/基数排序（教学实现约定：仅非负整数）
        int[] a = {5, 3, 8, 3, 1, 0, 9};
        SortingAlgorithms.countingSort(a);
        assertArrayEquals(new int[]{0, 1, 3, 3, 5, 8, 9}, a);
        int[] b = {170, 45, 75, 90, 802, 24, 2, 66};
        SortingAlgorithms.radixSort(b);
        assertArrayEquals(new int[]{2, 24, 45, 66, 75, 90, 170, 802}, b);
        // 全同元素 / 空数组
        int[] c = {4, 4, 4};
        SortingAlgorithms.countingSort(c);
        SortingAlgorithms.radixSort(new int[0]);
        assertArrayEquals(new int[]{4, 4, 4}, c);
        // 负数按约定抛异常
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> SortingAlgorithms.countingSort(new int[]{-1, 2}));
    }

    @Test
    void stabilityNotGuaranteedForIntSorts() {
        // 整型排序无稳定性可观测，仅验证 mergeSort 输出与插入排序一致
        Random rnd = new Random(53L);
        for (int trial = 0; trial < 20; trial++) {
            int[] a = new int[rnd.nextInt(300)];
            for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt(50);
            int[] byMerge = MergeSort.sort(a);
            int[] byIns = a.clone();
            SortingAlgorithms.insertionSort(byIns);
            assertArrayEquals(byMerge, byIns);
        }
    }
}
