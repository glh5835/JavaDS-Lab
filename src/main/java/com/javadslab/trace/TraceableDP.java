package com.javadslab.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 动态规划表格填充的 trace 演示（LCS 与 0/1 背包）。 */
public final class TraceableDP {

    private TraceableDP() {}

    /** LCS：逐格填充 DP 表（kind=matrix，活跃格高亮）。 */
    public static Tracer lcs(String s1, String s2) {
        Tracer t = new Tracer("matrix", "LCS “" + s1 + "” vs “" + s2 + "”");
        t.meta("algorithm", "lcs");
        int m = s1.length(), n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        t.step("init").before(matrixSnapshot(dp, -1, -1)).after(matrixSnapshot(dp, -1, -1)).commit();
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    t.step("match").arg("i", i).arg("j", j).arg("ch", String.valueOf(s1.charAt(i - 1)))
                            .arg("value", dp[i][j]).arg("from", "左上+1")
                            .before(matrixSnapshot(dp, i, j)).after(matrixSnapshot(dp, i, j))
                            .hl("cell", List.of(i, j)).commit();
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                    t.step("skip").arg("i", i).arg("j", j).arg("value", dp[i][j]).arg("from", "max(上,左)")
                            .before(matrixSnapshot(dp, i, j)).after(matrixSnapshot(dp, i, j))
                            .hl("cell", List.of(i, j)).commit();
                }
            }
        }
        t.step("done").arg("lcsLength", dp[m][n])
                .before(matrixSnapshot(dp, m, n)).after(matrixSnapshot(dp, m, n)).commit();
        return t;
    }

    /** 0/1 背包：逐行填充一维滚动数组（kind=array）。 */
    public static Tracer knapsack01(int[] weights, int[] values, int cap) {
        Tracer t = new Tracer("array", "0/1 背包 cap=" + cap);
        t.meta("algorithm", "knapsack01");
        int[] dp = new int[cap + 1];
        for (int i = 0; i < weights.length; i++) {
            for (int c = cap; c >= weights[i]; c--) {
                int before = dp[c];
                dp[c] = Math.max(dp[c], dp[c - weights[i]] + values[i]);
                Map<Integer, String> marks = new java.util.LinkedHashMap<>();
                marks.put(c, "cmp");
                t.step("consider").arg("item", i).arg("weight", weights[i]).arg("value", values[i])
                        .arg("capacity", c).arg("before", before).arg("after", dp[c])
                        .before(Snaps.arrayMarked("dp 数组", dp, marks))
                        .after(Snaps.arrayMarked("dp 数组", dp, marks))
                        .hl("cell", c).commit();
            }
            t.step("item-done").arg("item", i)
                    .before(Snaps.arrayPlain("dp 数组", dp))
                    .after(Snaps.arrayPlain("dp 数组", dp)).commit();
        }
        t.step("done").arg("best", dp[cap])
                .before(Snaps.arrayPlain("dp 数组", dp))
                .after(Snaps.arrayPlain("dp 数组", dp)).commit();
        return t;
    }

    private static Object matrixSnapshot(int[][] dp, int ai, int aj) {
        List<List<Object>> rows = new ArrayList<>();
        for (int i = 0; i < dp.length; i++) {
            List<Object> row = new ArrayList<>();
            for (int j = 0; j < dp[i].length; j++) {
                String mark = (i == ai && j == aj) ? "active" : null;
                row.add(Snaps.cell(dp[i][j], mark));
            }
            rows.add(row);
        }
        return Snaps.obj("kind", "matrix", "label", "LCS DP 表", "rows", rows);
    }

    /** 爬楼梯：一维 dp 数组逐格填充。 */
    public static Tracer climbStairs(int n) {
        Tracer t = new Tracer("array", "爬楼梯 dp（n=" + n + "）");
        t.meta("algorithm", "climb-stairs");
        long[] dp = new long[Math.max(n + 1, 2)];
        dp[0] = 1;
        dp[1] = 1;
        int[] show = new int[Math.max(n + 1, 2)];
        show[0] = 1;
        show[1] = 1;
        t.step("init").arg("dp0", 1).arg("dp1", 1)
                .before(Snaps.arrayPlain("dp 数组", show))
                .after(Snaps.arrayPlain("dp 数组", show)).commit();
        for (int i = 2; i <= n; i++) {
            dp[i] = dp[i - 1] + dp[i - 2];
            show[i] = (int) dp[i];
            Map<Integer, String> marks = new java.util.LinkedHashMap<>();
            marks.put(i, "active");
            marks.put(i - 1, "cmp");
            marks.put(i - 2, "cmp");
            t.step("fill").arg("i", i).arg("value", dp[i]).arg("from", "dp[i-1]+dp[i-2]")
                    .before(Snaps.arrayMarked("dp 数组", show, marks))
                    .after(Snaps.arrayMarked("dp 数组", show, marks))
                    .hl("cell", i).commit();
        }
        t.step("done").arg("ways", dp[n])
                .before(Snaps.arrayPlain("dp 数组", show))
                .after(Snaps.arrayPlain("dp 数组", show)).commit();
        return t;
    }

    /** 零钱兑换（最少硬币）：一维 dp 逐格填充。 */
    public static Tracer coinChange(int[] coins, int amount) {
        Tracer t = new Tracer("array", "零钱兑换 coins=" + java.util.Arrays.toString(coins) + " amount=" + amount);
        t.meta("algorithm", "coin-change");
        int INF = Integer.MAX_VALUE - 1;
        int[] dp = new int[amount + 1];
        java.util.Arrays.fill(dp, INF);
        dp[0] = 0;
        for (int c : coins) {
            for (int x = c; x <= amount; x++) {
                if (dp[x - c] + 1 < dp[x]) {
                    dp[x] = dp[x - c] + 1;
                    Map<Integer, String> marks = new java.util.LinkedHashMap<>();
                    marks.put(x, "active");
                    marks.put(x - c, "cmp");
                    t.step("relax").arg("coin", c).arg("amount", x).arg("newDp", dp[x])
                            .before(Snaps.arrayMarked("dp 数组", dp, marks))
                            .after(Snaps.arrayMarked("dp 数组", dp, marks))
                            .hl("cell", x).commit();
                }
            }
            t.step("coin-done").arg("coin", c)
                    .before(Snaps.arrayPlain("dp 数组", dp))
                    .after(Snaps.arrayPlain("dp 数组", dp)).commit();
        }
        t.step("done").arg("minCoins", dp[amount])
                .before(Snaps.arrayPlain("dp 数组", dp))
                .after(Snaps.arrayPlain("dp 数组", dp)).commit();
        return t;
    }
}
