package com.javadslab.core.graph;

import com.javadslab.core.heap.MyHeap;
import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 最短路三件套：
 * Dijkstra   —— 非负权、堆优化 O((V+E)logV)，返回 dist/prev；
 * Bellman-Ford —— 可处理负权、检测负环 O(VE)；
 * Floyd      —— 全源最短路 O(V^3)（DP：允许中转点 0..k）。
 */
public class ShortestPath {

    public static final int INF = Integer.MAX_VALUE / 2; // 防溢出的“无穷”

    private Tracer tracer;

    public void attachTracer(Tracer t) { this.tracer = t; }

    /* ================= Dijkstra ================= */

    public record Result(int[] dist, int[] prev) {}

    /** 堆优化 Dijkstra；图不得含负权边（Graph 构造已拦截）。 */
    public Result dijkstra(Graph g, int src) {
        int n = g.n();
        checkNode(g, src);
        int[] dist = new int[n];
        int[] prev = new int[n];
        boolean[] settled = new boolean[n];
        java.util.Arrays.fill(dist, INF);
        java.util.Arrays.fill(prev, -1);
        dist[src] = 0;

        MyHeap<int[]> heap = new MyHeap<>((a, b) -> Integer.compare(a[1], b[1])); // {node, dist}
        heap.push(new int[]{src, 0});
        while (!heap.isEmpty()) {
            int[] top = heap.pop();
            int u = top[0];
            if (settled[u]) continue;           // 惰性删除：过期条目跳过
            settled[u] = true;
            if (tracer != null) {
                tracer.step("settle").arg("u", u).arg("dist", dist[u])
                        .before(snapshot(g, dist, settled, u, null))
                        .after(snapshot(g, dist, settled, u, null))
                        .hl("active", u).commit();
            }
            for (int[] vw : g.neighbors(u)) {
                int v = vw[0], w = vw[1];
                if (!settled[v] && dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                    prev[v] = u;
                    heap.push(new int[]{v, dist[v]});
                    if (tracer != null) {
                        tracer.step("relax").arg("u", u).arg("v", v).arg("newDist", dist[v])
                                .before(snapshot(g, dist, settled, u, v))
                                .after(snapshot(g, dist, settled, u, v))
                                .hl("active", u).hl("improved", v).commit();
                    }
                }
            }
        }
        return new Result(dist, prev);
    }

    /* ================= Bellman-Ford ================= */

    /** 返回 dist；第 n 轮仍可松弛则存在负环，抛 IllegalStateException。 */
    public int[] bellmanFord(int n, List<Graph.Edge> edges, int src) {
        if (src < 0 || src >= n) throw new IndexOutOfBoundsException("源点越界");
        int[] dist = new int[n];
        java.util.Arrays.fill(dist, INF);
        dist[src] = 0;
        boolean[][] settledSnap = new boolean[n][0]; // 占位，快照用 dist 着色
        for (int round = 1; round <= n; round++) {
            boolean changed = false;
            boolean lastRound = round == n;
            for (Graph.Edge e : edges) {
                if (dist[e.u()] < INF && dist[e.u()] + e.w() < dist[e.v()]) {
                    if (lastRound) {
                        throw new IllegalStateException("存在负环，最短路无定义");
                    }
                    dist[e.v()] = dist[e.u()] + e.w();
                    changed = true;
                    if (tracer != null) {
                        tracer.step("relax").arg("round", round).arg("u", e.u()).arg("v", e.v()).arg("newDist", dist[e.v()])
                                .before(snapshotWeighted(n, dist, null, e.u()))
                                .after(snapshotWeighted(n, dist, null, e.v()))
                                .hl("edge", List.of(e.u(), e.v())).commit();
                    }
                }
            }
            if (!changed && !lastRound) break; // 提前收敛
        }
        return dist;
    }

    /* ================= Floyd ================= */

    /** 全源最短路；返回 dist 矩阵（无路径为 INF）。 */
    public int[][] floyd(int n, List<Graph.Edge> edges, boolean directed) {
        int[][] d = new int[n][n];
        for (int[] row : d) java.util.Arrays.fill(row, INF);
        for (int i = 0; i < n; i++) d[i][i] = 0;
        for (Graph.Edge e : edges) {
            d[e.u()][e.v()] = Math.min(d[e.u()][e.v()], e.w());
            if (!directed) d[e.v()][e.u()] = Math.min(d[e.v()][e.u()], e.w());
        }
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                if (d[i][k] >= INF) continue;
                for (int j = 0; j < n; j++) {
                    if (d[k][j] < INF && d[i][k] + d[k][j] < d[i][j]) {
                        d[i][j] = d[i][k] + d[k][j];
                        if (tracer != null) {
                            tracer.step("update").arg("k", k).arg("i", i).arg("j", j).arg("newDist", d[i][j])
                                    .before(Snaps.matrix("Floyd(k=" + k + ")", d, "∞"))
                                    .after(Snaps.matrix("Floyd(k=" + k + ")", d, "∞"))
                                    .hl("via", k).commit();
                        }
                    }
                }
            }
            if (tracer != null && k < n - 1) {
                tracer.step("allow-via").arg("k", k)
                        .before(Snaps.matrix("Floyd(允许中转 0.." + k + ")", d, "∞"))
                        .after(Snaps.matrix("Floyd(允许中转 0.." + k + ")", d, "∞"))
                        .commit();
            }
        }
        return d;
    }

    /* ---------------- 快照 ---------------- */

    private Object snapshot(Graph g, int[] dist, boolean[] settled, Integer active, Integer other) {
        Map<Integer, String> marks = new HashMap<>();
        if (settled != null) for (int i = 0; i < settled.length; i++) if (settled[i]) marks.put(i, "settled");
        if (active != null) marks.put(active, "active");
        if (other != null) marks.put(other, "improving");
        List<Object> distList = new ArrayList<>();
        for (int d : dist) distList.add(d >= INF ? "∞" : d);
        Map<String, Object> overlay = new HashMap<>();
        overlay.put("dist", distList);
        int[][] ea = g.edgeList().stream().map(e -> new int[]{e.u(), e.v(), e.w()}).toArray(int[][]::new);
        return Snaps.graph(g.n(), g.isDirected(), ea, marks, overlay);
    }

    private Object snapshotWeighted(int n, int[] dist, Integer active, Integer other) {
        Map<Integer, String> marks = new HashMap<>();
        if (active != null) marks.put(active, "active");
        if (other != null) marks.put(other, "improving");
        List<Object> distList = new ArrayList<>();
        for (int d : dist) distList.add(d >= INF ? "∞" : d);
        Map<String, Object> overlay = new HashMap<>();
        overlay.put("dist", distList);
        int[][] ea = new int[0][];
        return Snaps.graph(n, true, ea, marks, overlay);
    }

    private void checkNode(Graph g, int u) {
        if (u < 0 || u >= g.n()) throw new IndexOutOfBoundsException("节点越界: " + u);
    }
}
