package com.javadslab.core.search;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** 二分变体 + KMP 测试。 */
class SearchTest {

    private static final int[] A = {1, 2, 4, 4, 4, 6, 8};

    /* ================= 二分变体 ================= */

    @Test
    void standardSearch() {
        assertEquals(0, BinarySearch.search(A, 1));
        assertEquals(6, BinarySearch.search(A, 8));
        assertEquals(-1, BinarySearch.search(A, 5));
        assertEquals(-1, BinarySearch.search(A, 0));
        assertEquals(-1, BinarySearch.search(A, 9));
        assertEquals(-1, BinarySearch.search(new int[0], 3));
        assertEquals(0, BinarySearch.search(new int[]{9}, 9));
    }

    @Test
    void lowerAndUpperBound() {
        assertEquals(2, BinarySearch.lowerBound(A, 4)); // 第一个 4
        assertEquals(5, BinarySearch.upperBound(A, 4)); // 4 之后
        assertEquals(0, BinarySearch.lowerBound(A, 0));
        assertEquals(A.length, BinarySearch.lowerBound(A, 100));
        assertEquals(2, BinarySearch.lowerBound(A, 3)); // 插入位置
        assertEquals(5, BinarySearch.upperBound(A, 5));
        assertEquals(0, BinarySearch.upperBound(A, -1));
    }

    @Test
    void firstAndLastOccurrence() {
        assertEquals(2, BinarySearch.firstOccurrence(A, 4));
        assertEquals(4, BinarySearch.lastOccurrence(A, 4));
        assertEquals(-1, BinarySearch.firstOccurrence(A, 5));
        assertEquals(-1, BinarySearch.lastOccurrence(A, 5));
        int[] all = {7, 7, 7, 7};
        assertEquals(0, BinarySearch.firstOccurrence(all, 7));
        assertEquals(3, BinarySearch.lastOccurrence(all, 7));
    }

    @Test
    void rotatedSearchAndMin() {
        int[] r = {4, 5, 6, 7, 0, 1, 2};
        assertEquals(4, BinarySearch.searchRotated(r, 0));
        assertEquals(0, BinarySearch.searchRotated(r, 4));
        assertEquals(3, BinarySearch.searchRotated(r, 7));
        assertEquals(-1, BinarySearch.searchRotated(r, 3));
        assertEquals(0, BinarySearch.findMinRotated(r)); // 最小值 0
        assertEquals(1, BinarySearch.findMinRotated(new int[]{1, 2, 3}));
        assertEquals(1, BinarySearch.findMinRotated(new int[]{2, 1}));
        assertEquals(-1, BinarySearch.searchRotated(new int[]{1}, 5));
    }

    @Test
    void sqrtInt() {
        assertEquals(0, BinarySearch.sqrtInt(0));
        assertEquals(1, BinarySearch.sqrtInt(1));
        assertEquals(1, BinarySearch.sqrtInt(3));
        assertEquals(3, BinarySearch.sqrtInt(9));
        assertEquals(3, BinarySearch.sqrtInt(15));
        assertEquals(10, BinarySearch.sqrtInt(100));
        assertEquals(31, BinarySearch.sqrtInt(999));
        assertEquals(46340, BinarySearch.sqrtInt(Integer.MAX_VALUE));
        assertEquals(1_000_000, BinarySearch.sqrtInt(1_000_000_000_000L));
        assertThrows(IllegalArgumentException.class, () -> BinarySearch.sqrtInt(-1));
    }

    @Test
    void binarySearchRandomCrossCheckWithBruteForce() {
        Random rnd = new Random(61L);
        for (int trial = 0; trial < 300; trial++) {
            int n = rnd.nextInt(40);
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = rnd.nextInt(20);
            java.util.Arrays.sort(a);
            int key = rnd.nextInt(25) - 2;
            // 暴力参照
            int first = -1, last = -1, expectedLower = n, expectedUpper = n;
            for (int i = 0; i < n; i++) {
                if (a[i] == key) {
                    if (first < 0) first = i;
                    last = i;
                }
                if (expectedLower == n && a[i] >= key) expectedLower = i;
                if (expectedUpper == n && a[i] > key) expectedUpper = i;
            }
            assertEquals(expectedLower, BinarySearch.lowerBound(a, key));
            assertEquals(expectedUpper, BinarySearch.upperBound(a, key));
            assertEquals(first, BinarySearch.firstOccurrence(a, key));
            assertEquals(last, BinarySearch.lastOccurrence(a, key));
            if (key >= 0) assertEquals((long) Math.sqrt(key), BinarySearch.sqrtInt(key));
        }
    }

    /* ================= KMP ================= */

    @Test
    void kmpNextArray() {
        assertArrayEquals(new int[]{0, 0, 1, 2}, KMP.buildNext("abab"));
        assertArrayEquals(new int[]{0, 1, 2, 3, 4, 0, 1, 2, 3}, KMP.buildNext("aaaaabaaa"));
        assertArrayEquals(new int[]{0, 0, 0, 0, 0}, KMP.buildNext("abcde"));
        assertArrayEquals(new int[]{0, 1, 2, 3, 4}, KMP.buildNext("aaaaa"));
        assertArrayEquals(new int[]{0, 0, 1, 1, 2, 3, 2}, KMP.buildNext("abaabab"));
    }

    @Test
    void kmpSearchAll() {
        assertEquals(java.util.List.of(0, 3, 6, 9), KMP.searchAll("abcabcabcabc", "abc"));
        assertEquals(java.util.List.of(1), KMP.searchAll("aabb", "abb"));
        assertEquals(java.util.List.of(0, 1, 2), KMP.searchAll("aaaa", "aa")); // 允许重叠
        assertTrue(KMP.searchAll("abcdef", "xyz").isEmpty());
        assertTrue(KMP.searchAll("", "a").isEmpty());
        assertEquals(java.util.List.of(0), KMP.searchAll("abc", "abc"));
        assertTrue(KMP.searchAll("ab", "abc").isEmpty()); // 模式比主串长
    }

    @Test
    void kmpIndexOf() {
        assertEquals(3, KMP.indexOf("hello world", "lo w"));
        assertEquals(-1, KMP.indexOf("hello", "loo"));
    }

    @Test
    void kmpValidation() {
        assertThrows(IllegalArgumentException.class, () -> KMP.buildNext(""));
        assertThrows(IllegalArgumentException.class, () -> KMP.buildNext(null));
        assertThrows(IllegalArgumentException.class, () -> KMP.searchAll("abc", ""));
    }

    @Test
    void kmpCrossCheckWithBruteForce() {
        Random rnd = new Random(62L);
        for (int trial = 0; trial < 500; trial++) {
            StringBuilder t = new StringBuilder();
            StringBuilder p = new StringBuilder();
            int tl = rnd.nextInt(30), pl = 1 + rnd.nextInt(4);
            for (int i = 0; i < tl; i++) t.append((char) ('a' + rnd.nextInt(3)));
            for (int i = 0; i < pl; i++) p.append((char) ('a' + rnd.nextInt(3)));
            // 暴力
            java.util.List<Integer> want = new java.util.ArrayList<>();
            for (int i = 0; i + p.length() <= t.length(); i++) {
                if (t.substring(i, i + p.length()).equals(p.toString())) want.add(i);
            }
            assertEquals(want, KMP.searchAll(t.toString(), p.toString()),
                    "text=" + t + " pattern=" + p);
        }
    }
}
