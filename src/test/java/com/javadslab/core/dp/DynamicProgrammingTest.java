package com.javadslab.core.dp;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** 经典 DP 15 题：已知答案 + 暴力对照。 */
class DynamicProgrammingTest {

    /* 1 爬楼梯 */
    @Test
    void climbStairs() {
        assertEquals(1, DynamicProgramming.climbStairs(0));
        assertEquals(1, DynamicProgramming.climbStairs(1));
        assertEquals(2, DynamicProgramming.climbStairs(2));
        assertEquals(3, DynamicProgramming.climbStairs(3));
        assertEquals(5, DynamicProgramming.climbStairs(4));
        assertEquals(89, DynamicProgramming.climbStairs(10));
        assertEquals(20365011074L, DynamicProgramming.climbStairs(50)); // f(50)，long 不溢出
        assertThrows(IllegalArgumentException.class, () -> DynamicProgramming.climbStairs(-1));
    }

    /* 2 打家劫舍 */
    @Test
    void houseRobber() {
        assertEquals(4, DynamicProgramming.houseRobber(new int[]{1, 2, 3, 1}));
        assertEquals(12, DynamicProgramming.houseRobber(new int[]{2, 7, 9, 3, 1}));
        assertEquals(3, DynamicProgramming.houseRobber(new int[]{3}));
        assertEquals(5, DynamicProgramming.houseRobber(new int[]{5, 1})); // 相邻取大
        assertThrows(IllegalArgumentException.class, () -> DynamicProgramming.houseRobber(new int[0]));
    }

    /* 3 最大子数组 */
    @Test
    void maxSubArray() {
        assertEquals(6, DynamicProgramming.maxSubArray(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}));
        assertEquals(-1, DynamicProgramming.maxSubArray(new int[]{-3, -1, -2})); // 全负取最大单个元素
        assertEquals(5, DynamicProgramming.maxSubArray(new int[]{5}));
        assertThrows(IllegalArgumentException.class, () -> DynamicProgramming.maxSubArray(new int[0]));
    }

    /* 4 股票 I */
    @Test
    void maxProfit() {
        assertEquals(5, DynamicProgramming.maxProfit(new int[]{7, 1, 5, 3, 6, 4}));
        assertEquals(0, DynamicProgramming.maxProfit(new int[]{7, 6, 4, 3, 1})); // 单调降不买
        assertEquals(0, DynamicProgramming.maxProfit(new int[]{3}));
        assertThrows(IllegalArgumentException.class, () -> DynamicProgramming.maxProfit(new int[0]));
    }

    /* 5 零钱兑换 */
    @Test
    void coinChange() {
        assertEquals(3, DynamicProgramming.coinChange(new int[]{1, 2, 5}, 11));
        assertEquals(-1, DynamicProgramming.coinChange(new int[]{2}, 3));
        assertEquals(0, DynamicProgramming.coinChange(new int[]{1}, 0));
        assertEquals(2, DynamicProgramming.coinChange(new int[]{1, 3, 4}, 6)); // 3+3，贪心会错
    }

    /* 6 零钱兑换 II */
    @Test
    void coinChangeWays() {
        assertEquals(4, DynamicProgramming.coinChangeWays(new int[]{1, 2, 5}, 5)); // 5, 2+2+1, 2+1+1+1, 1*5
        assertEquals(0, DynamicProgramming.coinChangeWays(new int[]{2}, 3));
        assertEquals(1, DynamicProgramming.coinChangeWays(new int[]{7}, 0));
        // 暴力对照：枚举每种面额使用数量
        assertEquals(bruteForceCoinWays(new int[]{1, 2, 3}, 7), DynamicProgramming.coinChangeWays(new int[]{1, 2, 3}, 7));
    }

    private long bruteForceCoinWays(int[] coins, int amount) {
        return bruteForceCoinRec(coins, 0, amount);
    }

    private long bruteForceCoinRec(int[] coins, int i, int rest) {
        if (rest == 0) return 1;
        if (i == coins.length || rest < 0) return 0;
        return bruteForceCoinRec(coins, i + 1, rest) + bruteForceCoinRec(coins, i, rest - coins[i]);
    }

    /* 7 LIS */
    @Test
    void lengthOfLIS() {
        assertEquals(4, DynamicProgramming.lengthOfLIS(new int[]{10, 9, 2, 5, 3, 7, 101, 18}));
        assertEquals(1, DynamicProgramming.lengthOfLIS(new int[]{7, 7, 7, 7}));
        assertEquals(6, DynamicProgramming.lengthOfLIS(new int[]{1, 2, 3, 4, 5, 6}));
        assertEquals(3, DynamicProgramming.lengthOfLIS(new int[]{4, 10, 4, 3, 8, 9})); // 3,8,9 或 4,8,9
        assertThrows(IllegalArgumentException.class, () -> DynamicProgramming.lengthOfLIS(new int[0]));
    }

    /* 8 LCS */
    @Test
    void lcs() {
        assertEquals(3, DynamicProgramming.longestCommonSubsequence("abcde", "ace")); // ace
        assertEquals(3, DynamicProgramming.longestCommonSubsequence("abc", "abc"));
        assertEquals(0, DynamicProgramming.longestCommonSubsequence("abc", "def"));
        assertEquals(0, DynamicProgramming.longestCommonSubsequence("", "abc"));
        assertEquals(4, DynamicProgramming.longestCommonSubsequence("AGGTAB", "GXTXAYB")); // GTAB
    }

    /* 9 编辑距离 */
    @Test
    void editDistance() {
        assertEquals(3, DynamicProgramming.editDistance("horse", "ros"));
        assertEquals(5, DynamicProgramming.editDistance("intention", "execution"));
        assertEquals(0, DynamicProgramming.editDistance("same", "same"));
        assertEquals(3, DynamicProgramming.editDistance("", "abc"));
        assertEquals(2, DynamicProgramming.editDistance("ab", "")); // 删 2 个
    }

    /* 10 最长回文子串 */
    @Test
    void longestPalindrome() {
        assertEquals(3, DynamicProgramming.longestPalindromeLength("babad")); // bab / aba
        assertEquals(2, DynamicProgramming.longestPalindromeLength("cbbd"));  // bb
        assertEquals(7, DynamicProgramming.longestPalindromeLength("racecar")); // 整串回文
        assertEquals(0, DynamicProgramming.longestPalindromeLength(""));
        assertEquals(1, DynamicProgramming.longestPalindromeLength("abc")); // 单字符
    }

    /* 11 0/1 背包 */
    @Test
    void knapsack01() {
        assertEquals(35, DynamicProgramming.knapsack01(new int[]{1, 3, 4}, new int[]{15, 20, 30}, 4)); // w1+w3=1+3, v=15+30
        assertEquals(0, DynamicProgramming.knapsack01(new int[]{5}, new int[]{10}, 3));
        assertEquals(10, DynamicProgramming.knapsack01(new int[]{5}, new int[]{10}, 5));
        assertThrows(IllegalArgumentException.class, () -> DynamicProgramming.knapsack01(new int[]{1}, new int[]{1, 2}, 5));
    }

    /* 12 分割等和子集 */
    @Test
    void canPartition() {
        assertTrue(DynamicProgramming.canPartition(new int[]{1, 5, 11, 5}));
        assertFalse(DynamicProgramming.canPartition(new int[]{1, 2, 3, 5}));
        assertTrue(DynamicProgramming.canPartition(new int[]{2, 2}));
        assertFalse(DynamicProgramming.canPartition(new int[]{1})); // 奇数和
    }

    /* 13 目标和 */
    @Test
    void findTargetSumWays() {
        assertEquals(5, DynamicProgramming.findTargetSumWays(new int[]{1, 1, 1, 1, 1}, 3));
        assertEquals(1, DynamicProgramming.findTargetSumWays(new int[]{1}, 1));
        assertEquals(0, DynamicProgramming.findTargetSumWays(new int[]{1, 2}, 10)); // 不可能
    }

    /* 14 不同路径 */
    @Test
    void uniquePaths() {
        assertEquals(1, DynamicProgramming.uniquePaths(1, 1));
        assertEquals(3, DynamicProgramming.uniquePaths(3, 2));
        assertEquals(28, DynamicProgramming.uniquePaths(3, 7));
        assertEquals(2, DynamicProgramming.uniquePaths(2, 2));
        assertThrows(IllegalArgumentException.class, () -> DynamicProgramming.uniquePaths(0, 5));
    }

    /* 15 最小路径和 */
    @Test
    void minPathSum() {
        assertEquals(7, DynamicProgramming.minPathSum(new int[][]{{1, 3, 1}, {1, 5, 1}, {4, 2, 1}}));
        assertEquals(3, DynamicProgramming.minPathSum(new int[][]{{1, 2}, {1, 1}}));
        assertEquals(1, DynamicProgramming.minPathSum(new int[][]{{1}}));
        assertThrows(IllegalArgumentException.class, () -> DynamicProgramming.minPathSum(new int[0][0]));
    }

    /* ---------------- 随机对照：DP vs 暴力 ---------------- */

    @Test
    void houseRobberBruteForceCrossCheck() {
        Random rnd = new Random(71L);
        for (int trial = 0; trial < 200; trial++) {
            int n = 1 + rnd.nextInt(12);
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = rnd.nextInt(50);
            assertEquals(bruteRob(a, 0, false), DynamicProgramming.houseRobber(a), "nums=" + java.util.Arrays.toString(a));
        }
    }

    private int bruteRob(int[] a, int i, boolean prevRobbed) {
        if (i == a.length) return 0;
        int skip = bruteRob(a, i + 1, false);
        int rob = prevRobbed ? Integer.MIN_VALUE : a[i] + bruteRob(a, i + 1, true);
        return Math.max(skip, rob);
    }

    @Test
    void lisBruteForceCrossCheck() {
        Random rnd = new Random(72L);
        for (int trial = 0; trial < 200; trial++) {
            int n = 1 + rnd.nextInt(12);
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = rnd.nextInt(30);
            // 暴力：枚举所有子序列取最长递增
            int best = bruteLis(a, 0, Integer.MIN_VALUE);
            assertEquals(best, DynamicProgramming.lengthOfLIS(a), "nums=" + java.util.Arrays.toString(a));
        }
    }

    private int bruteLis(int[] a, int i, int prev) {
        if (i == a.length) return 0;
        int skip = bruteLis(a, i + 1, prev);
        int take = a[i] > prev ? 1 + bruteLis(a, i + 1, a[i]) : Integer.MIN_VALUE / 2;
        return Math.max(skip, take);
    }
}
