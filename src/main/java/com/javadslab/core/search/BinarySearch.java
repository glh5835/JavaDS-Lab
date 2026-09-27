package com.javadslab.core.search;

/**
 * 二分查找变体集（数组均须升序，除旋转题外）。
 * 循环不变量是核心：每个变体注释里写清 [lo, hi] 的含义。
 */
public final class BinarySearch {

    private BinarySearch() {}

    /** 标准二分：返回命中下标，不存在返回 -1。O(log n)。 */
    public static int search(int[] a, int key) {
        int lo = 0, hi = a.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] == key) return mid;
            if (a[mid] < key) lo = mid + 1;
            else hi = mid - 1;
        }
        return -1;
    }

    /** lowerBound：第一个 >= key 的下标（不存在返回 a.length）。 */
    public static int lowerBound(int[] a, int key) {
        int lo = 0, hi = a.length; // 不变量：[0,lo) < key <= [hi, n)
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] < key) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    /** upperBound：第一个 > key 的下标（不存在返回 a.length）。 */
    public static int upperBound(int[] a, int key) {
        int lo = 0, hi = a.length; // 不变量：[0,lo) <= key < [hi, n)
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] <= key) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    /** 重复元素的第一次出现下标（不存在 -1）。 */
    public static int firstOccurrence(int[] a, int key) {
        int i = lowerBound(a, key);
        return i < a.length && a[i] == key ? i : -1;
    }

    /** 重复元素的最后一次出现下标（不存在 -1）。 */
    public static int lastOccurrence(int[] a, int key) {
        int i = upperBound(a, key) - 1;
        return i >= 0 && a[i] == key ? i : -1;
    }

    /** 旋转有序数组查找（无重复元素）。O(log n)。 */
    public static int searchRotated(int[] a, int key) {
        int lo = 0, hi = a.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] == key) return mid;
            if (a[lo] <= a[mid]) {          // 左半有序
                if (a[lo] <= key && key < a[mid]) hi = mid - 1;
                else lo = mid + 1;
            } else {                        // 右半有序
                if (a[mid] < key && key <= a[hi]) lo = mid + 1;
                else hi = mid - 1;
            }
        }
        return -1;
    }

    /** 旋转数组中的最小值。O(log n)。 */
    public static int findMinRotated(int[] a) {
        if (a.length == 0) throw new IllegalArgumentException("空数组");
        int lo = 0, hi = a.length - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] > a[hi]) lo = mid + 1; // 最小值在右半
            else hi = mid;                     // 最小值在左半（含 mid）
        }
        return a[lo];
    }

    /** 二分求整数平方根（x 非负），返回 floor(sqrt(x))。 */
    public static long sqrtInt(long x) {
        if (x < 0) throw new IllegalArgumentException("x 不能为负: " + x);
        long lo = 0, hi = x, ans = 0;
        while (lo <= hi) {
            long mid = lo + (hi - lo) / 2;
            // mid*mid 可能溢出，用除法比较；mid=0 时 0^2 恒 <= x
            if (mid != 0 && mid > x / mid) {
                hi = mid - 1;
            } else {
                ans = mid;
                lo = mid + 1;
            }
        }
        return ans;
    }
}
