package com.javadslab.core.dp;

/**
 * 经典动态规划 15 题（每题一个静态方法，题号对应 docs/notes 里的笔记）。
 * 统一约定：结果用 int/long；非法入参抛 IllegalArgumentException。
 */
public final class DynamicProgramming {

    private DynamicProgramming() {}

    /* 1. 爬楼梯：f(n)=f(n-1)+f(n-2)。O(n)/O(1)。 */
    public static long climbStairs(int n) {
        if (n < 0) throw new IllegalArgumentException("n 不能为负");
        long a = 1, b = 1; // f(0)=1, f(1)=1
        for (int i = 2; i <= n; i++) {
            long c = a + b;
            a = b;
            b = c;
        }
        return b;
    }

    /* 2. 打家劫舍（不相邻最大和）。O(n)/O(1)。 */
    public static int houseRobber(int[] nums) {
        requireNonEmpty(nums);
        int rob = 0, skip = 0;
        for (int v : nums) {
            int newRob = skip + v;
            skip = Math.max(skip, rob);
            rob = newRob;
        }
        return Math.max(rob, skip);
    }

    /* 3. 最大子数组和（Kadane）。O(n)/O(1)。 */
    public static int maxSubArray(int[] nums) {
        requireNonEmpty(nums);
        int best = nums[0], cur = nums[0];
        for (int i = 1; i < nums.length; i++) {
            cur = Math.max(nums[i], cur + nums[i]);
            best = Math.max(best, cur);
        }
        return best;
    }

    /* 4. 买卖股票最佳时机 I（一次交易）。O(n)/O(1)。 */
    public static int maxProfit(int[] prices) {
        requireNonEmpty(prices);
        int minPrice = prices[0], profit = 0;
        for (int p : prices) {
            profit = Math.max(profit, p - minPrice);
            minPrice = Math.min(minPrice, p);
        }
        return profit;
    }

    /* 5. 零钱兑换（凑出 amount 的最少硬币数，无解 -1）。O(amount*n)。 */
    public static int coinChange(int[] coins, int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount 不能为负");
        int[] dp = new int[amount + 1];
        java.util.Arrays.fill(dp, Integer.MAX_VALUE - 1);
        dp[0] = 0;
        for (int c : coins) {
            for (int x = c; x <= amount; x++) { // 完全背包正序
                if (dp[x - c] + 1 < dp[x]) dp[x] = dp[x - c] + 1;
            }
        }
        return dp[amount] >= Integer.MAX_VALUE - 1 ? -1 : dp[amount];
    }

    /* 6. 零钱兑换 II（组合数，coins 顺序无关）。O(amount*n)。 */
    public static long coinChangeWays(int[] coins, int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount 不能为负");
        long[] dp = new long[amount + 1];
        dp[0] = 1;
        for (int c : coins) {          // 先物品后金额 → 组合数（不重复计顺序）
            for (int x = c; x <= amount; x++) {
                dp[x] += dp[x - c];
            }
        }
        return dp[amount];
    }

    /* 7. 最长递增子序列 LIS（O(n^2) DP 版；贪心+二分见 BinarySearch 应用）。 */
    public static int lengthOfLIS(int[] nums) {
        requireNonEmpty(nums);
        int[] dp = new int[nums.length]; // dp[i]=以 i 结尾的 LIS 长度
        int best = 1;
        for (int i = 0; i < nums.length; i++) {
            dp[i] = 1;
            for (int j = 0; j < i; j++) {
                if (nums[j] < nums[i]) dp[i] = Math.max(dp[i], dp[j] + 1);
            }
            best = Math.max(best, dp[i]);
        }
        return best;
    }

    /* 8. 最长公共子序列 LCS。O(mn)。 */
    public static int longestCommonSubsequence(String s1, String s2) {
        if (s1 == null || s2 == null) throw new IllegalArgumentException("入参不能为 null");
        int m = s1.length(), n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) dp[i][j] = dp[i - 1][j - 1] + 1;
                else dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        return dp[m][n];
    }

    /* 9. 编辑距离（插入/删除/替换）。O(mn)。 */
    public static int editDistance(String s1, String s2) {
        if (s1 == null || s2 == null) throw new IllegalArgumentException("入参不能为 null");
        int m = s1.length(), n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[m][n];
    }

    /* 10. 最长回文子串（区间 DP，返回长度）。O(n^2)。 */
    public static int longestPalindromeLength(String s) {
        if (s == null) throw new IllegalArgumentException("入参不能为 null");
        int n = s.length();
        if (n == 0) return 0;
        boolean[][] dp = new boolean[n][n]; // dp[i][j]=s[i..j] 是否回文
        int best = 1;
        for (int i = 0; i < n; i++) dp[i][i] = true;
        for (int len = 2; len <= n; len++) {
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                dp[i][j] = s.charAt(i) == s.charAt(j) && (len == 2 || dp[i + 1][j - 1]);
                if (dp[i][j]) best = len;
            }
        }
        return best;
    }

    /* 11. 0/1 背包（恰好容量不超过 cap 的最大价值）。O(n*cap)。 */
    public static int knapsack01(int[] weights, int[] values, int cap) {
        checkKnapsack(weights, values, cap);
        int[] dp = new int[cap + 1];
        for (int i = 0; i < weights.length; i++) {
            for (int c = cap; c >= weights[i]; c--) { // 逆序：每件最多选一次
                dp[c] = Math.max(dp[c], dp[c - weights[i]] + values[i]);
            }
        }
        return dp[cap];
    }

    /* 12. 分割等和子集（能否划分成两个等和子集 = 子集和 sum/2）。O(n*sum)。 */
    public static boolean canPartition(int[] nums) {
        requireNonEmpty(nums);
        int sum = 0;
        for (int v : nums) {
            if (v < 0) throw new IllegalArgumentException("元素须非负");
            sum += v;
        }
        if (sum % 2 != 0) return false;
        int target = sum / 2;
        boolean[] dp = new boolean[target + 1];
        dp[0] = true;
        for (int v : nums) {
            for (int c = target; c >= v; c--) dp[c] = dp[c] || dp[c - v];
        }
        return dp[target];
    }

    /* 13. 目标和（加减号分配方案数 = 子集和 (target+sum)/2 的组合数）。O(n*sum)。 */
    public static long findTargetSumWays(int[] nums, int target) {
        requireNonEmpty(nums);
        int sum = 0;
        for (int v : nums) sum += v;
        long diff = (long) target + sum;
        if (diff < 0 || diff % 2 != 0) return 0;
        int cap = (int) (diff / 2);
        long[] dp = new long[cap + 1];
        dp[0] = 1;
        for (int v : nums) {
            for (int c = cap; c >= v; c--) dp[c] += dp[c - v];
        }
        return dp[cap];
    }

    /* 14. 不同路径（m*n 网格只能右/下）。O(mn)。 */
    public static long uniquePaths(int m, int n) {
        if (m <= 0 || n <= 0) throw new IllegalArgumentException("网格尺寸必须为正");
        long[] dp = new long[n];
        java.util.Arrays.fill(dp, 1);
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) dp[j] += dp[j - 1];
        }
        return dp[n - 1];
    }

    /* 15. 最小路径和（网格左上到右下，只能右/下）。O(mn)。 */
    public static int minPathSum(int[][] grid) {
        if (grid == null || grid.length == 0 || grid[0].length == 0)
            throw new IllegalArgumentException("网格不能为空");
        int m = grid.length, n = grid[0].length;
        int[] dp = new int[n];
        dp[0] = grid[0][0];
        for (int j = 1; j < n; j++) dp[j] = dp[j - 1] + grid[0][j];
        for (int i = 1; i < m; i++) {
            dp[0] += grid[i][0];
            for (int j = 1; j < n; j++) {
                dp[j] = Math.min(dp[j], dp[j - 1]) + grid[i][j];
            }
        }
        return dp[n - 1];
    }

    /* ---------------- 工具 ---------------- */

    private static void requireNonEmpty(int[] a) {
        if (a == null || a.length == 0) throw new IllegalArgumentException("数组不能为空");
    }

    private static void checkKnapsack(int[] weights, int[] values, int cap) {
        if (weights == null || values == null || weights.length != values.length)
            throw new IllegalArgumentException("重量与价值数组须等长且非 null");
        if (cap < 0) throw new IllegalArgumentException("容量不能为负");
    }
}
