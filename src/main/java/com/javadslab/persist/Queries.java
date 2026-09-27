package com.javadslab.persist;

import java.util.List;

/**
 * 10 条复杂查询（多表 JOIN / 聚合 / 窗口函数 / 子查询）。
 * 全部为常量 SQL，参数经 PreparedStatement 绑定，绝不拼接。
 */
public final class Queries {

    private Queries() {}

    public record Query(String name, String sql, boolean hasParam) {}

    public static final List<Query> ALL = List.of(

            /* Q1 多表 JOIN：每个算法的运行次数、通过率、平均耗时（聚合+JOIN） */
            new Query("Q1_各算法运行统计",
                    """
                    SELECT a.name, a.category, COUNT(r.id) AS run_cnt,
                           ROUND(100.0 * SUM(r.passed) / COUNT(r.id), 2) AS pass_rate,
                           ROUND(AVG(r.duration_ms), 1) AS avg_ms
                    FROM algorithms a JOIN runs r ON r.algorithm_id = a.id
                    GROUP BY a.id
                    HAVING COUNT(r.id) >= ?
                    ORDER BY run_cnt DESC, a.name
                    """, true),

            /* Q2 窗口函数：每个算法内按耗时排名的前 3 次运行（PERCENTILE 思路用窗口实现） */
            new Query("Q2_每算法最快3次运行",
                    """
                    SELECT name, ran_at, duration_ms, rk
                    FROM (
                        SELECT a.name, r.ran_at, r.duration_ms,
                               ROW_NUMBER() OVER (PARTITION BY a.id ORDER BY r.duration_ms) AS rk
                        FROM algorithms a JOIN runs r ON r.algorithm_id = a.id
                    ) WHERE rk <= 3 ORDER BY name, rk
                    """, false),

            /* Q3 窗口函数：各算法按日期的累计运行次数（移动总量） */
            new Query("Q3_按日累计运行量",
                    """
                    SELECT d, name, day_cnt,
                           SUM(day_cnt) OVER (PARTITION BY name ORDER BY d
                                              ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cum_cnt
                    FROM (
                        SELECT substr(r.ran_at, 1, 10) AS d, a.name, COUNT(*) AS day_cnt
                        FROM runs r JOIN algorithms a ON a.id = r.algorithm_id
                        GROUP BY d, a.id
                    ) ORDER BY d DESC, name LIMIT 40
                    """, false),

            /* Q4 子查询（派生表）：耗时高于该算法平均值的“慢运行” */
            new Query("Q4_慢于自身均值的运行",
                    """
                    SELECT a.name, r.duration_ms, ROUND(s.avg_ms, 1) AS algo_avg
                    FROM runs r
                    JOIN algorithms a ON a.id = r.algorithm_id
                    JOIN (SELECT algorithm_id, AVG(duration_ms) AS avg_ms FROM runs GROUP BY algorithm_id) s
                         ON s.algorithm_id = r.algorithm_id
                    WHERE r.duration_ms > s.avg_ms AND r.duration_ms <= ?
                    ORDER BY r.duration_ms DESC LIMIT 30
                    """, true),

            /* Q5 难度交叉分析：各难度档位的通过率与平均分（聚合 + GROUP BY 表达式） */
            new Query("Q5_难度维度通过率",
                    """
                    SELECT a.difficulty,
                           CASE WHEN a.difficulty <= 2 THEN '入门' WHEN a.difficulty = 3 THEN '进阶' ELSE '挑战' END AS tier,
                           COUNT(DISTINCT a.id) AS alg_cnt,
                           SUM(r.passed) * 100.0 / COUNT(r.id) AS pass_pct
                    FROM algorithms a JOIN runs r ON r.algorithm_id = a.id
                    GROUP BY a.difficulty ORDER BY a.difficulty
                    """, false),

            /* Q6 EXISTS：做过但从未失败过的算法（全绿算法） */
            new Query("Q6_从未失败的算法",
                    """
                    SELECT a.name, COUNT(r.id) AS runs
                    FROM algorithms a JOIN runs r ON r.algorithm_id = a.id
                    WHERE NOT EXISTS (SELECT 1 FROM runs bad WHERE bad.algorithm_id = a.id AND bad.passed = 0)
                    GROUP BY a.id ORDER BY runs DESC
                    """, false),

            /* Q7 窗口函数对比：各算法平均耗时在全部算法中的排名（RANK）与占比 */
            new Query("Q7_算法耗时排名",
                    """
                    SELECT name, avg_ms,
                           RANK() OVER (ORDER BY avg_ms) AS rank_fastest_first,
                           ROUND(100.0 * avg_ms / SUM(avg_ms) OVER (), 2) AS pct_of_total
                    FROM (
                        SELECT a.name, AVG(r.duration_ms) AS avg_ms
                        FROM algorithms a JOIN runs r ON r.algorithm_id = a.id GROUP BY a.id
                    )
                    """, false),

            /* Q8 错题本交叉：错题数最多的知识点 vs 运行失败率（双聚合 JOIN） */
            new Query("Q8_知识点失败率与错题",
                    """
                    SELECT s.name, s.category, s.fail_pct, COALESCE(m.m_cnt, 0) AS mistake_cnt
                    FROM (
                        SELECT a.id, a.name, a.category,
                               100.0 * SUM(1 - r.passed) / COUNT(r.id) AS fail_pct
                        FROM algorithms a JOIN runs r ON r.algorithm_id = a.id
                        GROUP BY a.id
                    ) s LEFT JOIN (
                        SELECT algorithm_id, COUNT(*) AS m_cnt FROM mistakes GROUP BY algorithm_id
                    ) m ON m.algorithm_id = s.id
                    ORDER BY s.fail_pct DESC, mistake_cnt DESC LIMIT 20
                    """, false),

            /* Q9 分组 Top-N：每个 category 里平均耗时最低的 2 个算法（窗口 + 过滤） */
            new Query("Q9_每类最快2算法",
                    """
                    SELECT name, category, avg_ms FROM (
                        SELECT a.name, a.category, AVG(r.duration_ms) AS avg_ms,
                               ROW_NUMBER() OVER (PARTITION BY a.category ORDER BY AVG(r.duration_ms)) AS rk
                        FROM algorithms a JOIN runs r ON r.algorithm_id = a.id
                        GROUP BY a.id
                    ) WHERE rk <= 2 ORDER BY category, avg_ms
                    """, false),

            /* Q10 时间趋势：最近 days 天内每天的运行量与失败量（日期聚合 + HAVING） */
            new Query("Q10_每日练习趋势",
                    """
                    SELECT substr(ran_at, 1, 10) AS d,
                           COUNT(*) AS total,
                           SUM(1 - passed) AS fails,
                           ROUND(AVG(duration_ms), 1) AS avg_ms
                    FROM runs
                    WHERE ran_at >= datetime('now', '-' || ? || ' days', 'localtime')
                    GROUP BY d HAVING total >= 1 ORDER BY d
                    """, true)
    );
}
