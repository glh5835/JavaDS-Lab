package com.javadslab.core.graph;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图的遍历：BFS（队列，最短路 for 无权图）+ DFS（递归 & 显式栈迭代）。
 * step-mode：每个节点「发现/访问」都记录 trace（图快照 + visited 标记）。
 */
public class GraphSearch {

    private Tracer tracer;

    public void attachTracer(Tracer t) { this.tracer = t; }

    /* ---------------- BFS ---------------- */

    /** 从 start 出发 BFS，返回访问顺序；dist 为跳数（-1 不可达）。 */
    public List<Integer> bfs(Graph g, int start) {
        int n = g.n();
        boolean[] visited = new boolean[n];
        int[] dist = new int[n];
        java.util.Arrays.fill(dist, -1);
        List<Integer> order = new ArrayList<>();
        int[][] edgesArr = edgeArrayForSnapshot(g);

        ArrayDeque<Integer> queue = new ArrayDeque<>();
        visited[start] = true;
        dist[start] = 0;
        queue.add(start);
        if (tracer != null) {
            tracer.step("start").arg("start", start)
                    .before(snapshot(g, edgesArr, visited, start, null))
                    .after(snapshot(g, edgesArr, visited, start, null))
                    .commit();
        }
        while (!queue.isEmpty()) {
            int u = queue.poll();
            order.add(u);
            if (tracer != null) {
                tracer.step("dequeue").arg("u", u).arg("dist", dist[u])
                        .before(snapshot(g, edgesArr, visited, u, null))
                        .after(snapshot(g, edgesArr, visited, u, null))
                        .hl("active", u).commit();
            }
            for (int[] vw : g.neighbors(u)) {
                int v = vw[0];
                if (!visited[v]) {
                    visited[v] = true;
                    dist[v] = dist[u] + 1;
                    queue.add(v);
                    if (tracer != null) {
                        tracer.step("discover").arg("from", u).arg("v", v).arg("dist", dist[v])
                                .before(snapshot(g, edgesArr, visited, u, v))
                                .after(snapshot(g, edgesArr, visited, u, v))
                                .hl("active", u).hl("neighbor", v).commit();
                    }
                }
            }
        }
        return order;
    }

    /** BFS 逐跳距离（配合 bfs 使用，供测试）。 */
    public int[] bfsDistances(Graph g, int start) {
        int n = g.n();
        int[] dist = new int[n];
        java.util.Arrays.fill(dist, -1);
        boolean[] visited = new boolean[n];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        visited[start] = true;
        dist[start] = 0;
        queue.add(start);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int[] vw : g.neighbors(u)) {
                if (!visited[vw[0]]) {
                    visited[vw[0]] = true;
                    dist[vw[0]] = dist[u] + 1;
                    queue.add(vw[0]);
                }
            }
        }
        return dist;
    }

    /* ---------------- DFS ---------------- */

    /** 递归 DFS。 */
    public List<Integer> dfsRecursive(Graph g, int start) {
        boolean[] visited = new boolean[g.n()];
        List<Integer> order = new ArrayList<>();
        dfsRec(g, start, visited, order);
        return order;
    }

    private void dfsRec(Graph g, int u, boolean[] visited, List<Integer> order) {
        visited[u] = true;
        order.add(u);
        if (tracer != null) {
            int[][] ea = edgeArrayForSnapshot(g);
            tracer.step("visit").arg("u", u)
                    .before(snapshot(g, ea, visited, u, null))
                    .after(snapshot(g, ea, visited, u, null))
                    .hl("active", u).commit();
        }
        for (int[] vw : g.neighbors(u)) {
            if (!visited[vw[0]]) dfsRec(g, vw[0], visited, order);
        }
    }

    /** 迭代 DFS（显式栈，逆序压邻居保持与递归一致的访问次序）。 */
    public List<Integer> dfsIterative(Graph g, int start) {
        boolean[] visited = new boolean[g.n()];
        List<Integer> order = new ArrayList<>();
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            int u = stack.pop();
            if (visited[u]) continue;
            visited[u] = true;
            order.add(u);
            List<int[]> nbrs = g.neighbors(u);
            for (int i = nbrs.size() - 1; i >= 0; i--) {
                if (!visited[nbrs.get(i)[0]]) stack.push(nbrs.get(i)[0]);
            }
        }
        return order;
    }

    /* ---------------- 快照 ---------------- */

    private int[][] edgeArrayForSnapshot(Graph g) {
        List<Graph.Edge> es = g.edgeList();
        int[][] arr = new int[es.size()][];
        for (int i = 0; i < es.size(); i++) {
            Graph.Edge e = es.get(i);
            arr[i] = new int[]{e.u(), e.v(), e.w()};
        }
        return arr;
    }

    private Object snapshot(Graph g, int[][] edges, boolean[] visited, Integer active, Integer neighbor) {
        Map<Integer, String> marks = new HashMap<>();
        for (int i = 0; i < visited.length; i++) if (visited[i]) marks.put(i, "visited");
        if (active != null) marks.put(active, "active");
        if (neighbor != null) marks.put(neighbor, "frontier");
        return Snaps.graph(g.n(), g.isDirected(), edges, marks, null);
    }
}
