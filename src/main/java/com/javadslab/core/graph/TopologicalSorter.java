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
 * 拓扑排序（仅 DAG 有解）：
 * Kahn —— 入度表 + 队列，BFS 风格，可检测剩余入度>0（有环）；
 * DFS —— 逆后序（完成序倒置），用三色标记检测回边（有环）。
 */
public class TopologicalSorter {

    private Tracer tracer;

    public void attachTracer(Tracer t) { this.tracer = t; }

    /** Kahn：返回拓扑序；有环抛 IllegalStateException。 */
    public List<Integer> kahn(Graph g) {
        if (!g.isDirected()) throw new IllegalArgumentException("拓扑排序需要有向图");
        int n = g.n();
        int[] indegree = new int[n];
        for (int u = 0; u < n; u++) for (int[] vw : g.neighbors(u)) indegree[vw[0]]++;

        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < n; i++) if (indegree[i] == 0) queue.add(i);

        List<Integer> order = new ArrayList<>(n);
        int[][] ea = edgeArray(g);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            order.add(u);
            if (tracer != null) {
                tracer.step("take").arg("u", u).arg("order", order.size())
                        .before(snapshot(g, ea, indegree, u, null)).after(snapshot(g, ea, indegree, u, null))
                        .hl("active", u).commit();
            }
            for (int[] vw : g.neighbors(u)) {
                if (--indegree[vw[0]] == 0) {
                    queue.add(vw[0]);
                    if (tracer != null) {
                        tracer.step("release").arg("u", u).arg("v", vw[0])
                                .before(snapshot(g, ea, indegree, u, vw[0])).after(snapshot(g, ea, indegree, u, vw[0]))
                                .commit();
                    }
                }
            }
        }
        if (order.size() != n) throw new IllegalStateException("图中存在环，无法拓扑排序");
        return order;
    }

    /** DFS 逆后序：有环抛 IllegalStateException。 */
    public List<Integer> dfsOrder(Graph g) {
        if (!g.isDirected()) throw new IllegalArgumentException("拓扑排序需要有向图");
        int n = g.n();
        // 0=白(未访问) 1=灰(在栈中/递归中) 2=黑(完成)
        int[] color = new int[n];
        List<Integer> finishOrder = new ArrayList<>(n);
        int[][] ea = edgeArray(g);
        for (int s = 0; s < n; s++) {
            if (color[s] != 0) continue;
            // 迭代 DFS：栈元素 {node, 邻居游标}
            Deque<int[]> stack = new ArrayDeque<>();
            color[s] = 1;
            if (tracer != null) {
                tracer.step("enter").arg("u", s)
                        .before(snapshot(g, ea, indegreeSnapshot(color), s, null))
                        .after(snapshot(g, ea, indegreeSnapshot(color), s, null))
                        .hl("active", s).commit();
            }
            stack.push(new int[]{s, 0});
            while (!stack.isEmpty()) {
                int[] frame = stack.peek();
                int u = frame[0];
                if (frame[1] < g.neighbors(u).size()) {
                    int v = g.neighbors(u).get(frame[1]++)[0];
                    if (color[v] == 1) throw new IllegalStateException("图中存在环（检测到回边 " + u + "->" + v + "）");
                    if (color[v] == 0) {
                        color[v] = 1;
                        if (tracer != null) {
                            tracer.step("enter").arg("u", v).arg("from", u)
                                    .before(snapshot(g, ea, indegreeSnapshot(color), v, null))
                                    .after(snapshot(g, ea, indegreeSnapshot(color), v, null))
                                    .hl("active", v).commit();
                        }
                        stack.push(new int[]{v, 0});
                    }
                } else {
                    color[u] = 2;
                    finishOrder.add(u);
                    stack.pop();
                    if (tracer != null) {
                        tracer.step("finish").arg("u", u)
                                .before(snapshot(g, ea, indegreeSnapshot(color), null, u))
                                .after(snapshot(g, ea, indegreeSnapshot(color), null, u))
                                .hl("done", u).commit();
                    }
                }
            }
        }
        java.util.Collections.reverse(finishOrder);
        return finishOrder;
    }

    /* ---------------- 快照 ---------------- */

    private int[][] edgeArray(Graph g) {
        return g.edgeList().stream().map(e -> new int[]{e.u(), e.v(), e.w()}).toArray(int[][]::new);
    }

    private int[] indegreeSnapshot(int[] color) {
        return color.clone();
    }

    private Object snapshot(Graph g, int[][] edges, int[] colorOrIndegree, Integer active, Integer other) {
        Map<Integer, String> marks = new HashMap<>();
        for (int i = 0; i < colorOrIndegree.length; i++) {
            int v = colorOrIndegree[i];
            if (v == 0) marks.put(i, "todo");
            else if (v == 1) marks.put(i, "inProgress");
            else marks.put(i, "done");
        }
        if (active != null) marks.put(active, "active");
        if (other != null) marks.put(other, "released");
        return Snaps.graph(g.n(), true, edges, marks, null);
    }
}
