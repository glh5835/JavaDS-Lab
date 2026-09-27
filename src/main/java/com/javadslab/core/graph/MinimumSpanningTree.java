package com.javadslab.core.graph;

import com.javadslab.core.heap.MyHeap;
import com.javadslab.core.sort.MergeSort;
import com.javadslab.core.unionfind.UnionFind;
import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 最小生成树两算法：
 * Prim   —— 堆优化，从源点“长树”，O((V+E)logV)；
 * Kruskal —— 边按权排序（手写归并排序，非 Collections.sort）+ 并查集判环，O(E logE)。
 */
public class MinimumSpanningTree {

    public record MstResult(long totalWeight, List<Graph.Edge> edges) {}

    private Tracer tracer;

    public void attachTracer(Tracer t) { this.tracer = t; }

    /* ---------------- Prim（堆优化） ---------------- */

    public MstResult prim(Graph g, int start) {
        if (g.isDirected()) throw new IllegalArgumentException("Prim 需要无向图");
        int n = g.n();
        boolean[] inTree = new boolean[n];
        List<Graph.Edge> treeEdges = new ArrayList<>();
        long total = 0;

        // 堆元素 {from, to, w}，按 w 比较
        MyHeap<int[]> heap = new MyHeap<>((a, b) -> Integer.compare(a[2], b[2]));
        inTree[start] = true;
        pushNeighbors(g, start, heap);
        if (tracer != null) {
            tracer.step("start").arg("start", start)
                    .before(snapshot(g, inTree, null, null)).after(snapshot(g, inTree, null, null)).commit();
        }
        while (!heap.isEmpty() && treeEdges.size() < n - 1) {
            int[] top = heap.pop();
            int u = top[0], v = top[1], w = top[2];
            if (inTree[v]) continue; // 已在树中：跳过过期条目
            inTree[v] = true;
            total += w;
            treeEdges.add(new Graph.Edge(u, v, w));
            if (tracer != null) {
                tracer.step("add-edge").arg("u", u).arg("v", v).arg("w", w).arg("total", total)
                        .before(snapshot(g, inTree, u, v)).after(snapshot(g, inTree, u, v))
                        .hl("edge", List.of(u, v)).commit();
            }
            pushNeighbors(g, v, heap);
        }
        if (treeEdges.size() != n - 1) {
            throw new IllegalStateException("图不连通，无法生成最小生成树");
        }
        return new MstResult(total, treeEdges);
    }

    private void pushNeighbors(Graph g, int u, MyHeap<int[]> heap) {
        for (int[] vw : g.neighbors(u)) {
            heap.push(new int[]{u, vw[0], vw[1]});
        }
    }

    /* ---------------- Kruskal ---------------- */

    public MstResult kruskal(Graph g) {
        if (g.isDirected()) throw new IllegalArgumentException("Kruskal 需要无向图");
        int n = g.n();
        List<Graph.Edge> edges = new ArrayList<>(g.edgeList());
        // 手写归并排序（不使用 java.util 排序充当核心逻辑）
        Graph.Edge[] arr = edges.toArray(new Graph.Edge[0]);
        MergeSort.sort(arr, (a, b) -> Integer.compare(a.w(), b.w()));

        UnionFind uf = new UnionFind(n);
        List<Graph.Edge> picked = new ArrayList<>();
        long total = 0;
        for (Graph.Edge e : arr) {
            if (uf.union(e.u(), e.v())) {
                picked.add(e);
                total += e.w();
                if (tracer != null) {
                    tracer.step("accept").arg("u", e.u()).arg("v", e.v()).arg("w", e.w()).arg("total", total)
                            .before(snapshot(g, null, e.u(), e.v())).after(snapshot(g, null, e.u(), e.v()))
                            .hl("edge", List.of(e.u(), e.v())).commit();
                }
                if (picked.size() == n - 1) break;
            } else if (tracer != null) {
                tracer.step("reject-cycle").arg("u", e.u()).arg("v", e.v()).arg("w", e.w())
                        .before(snapshot(g, null, e.u(), e.v())).after(snapshot(g, null, e.u(), e.v()))
                        .hl("edge", List.of(e.u(), e.v())).commit();
            }
        }
        if (picked.size() != n - 1) throw new IllegalStateException("图不连通，无法生成最小生成树");
        return new MstResult(total, picked);
    }

    /* ---------------- 快照 ---------------- */

    private Object snapshot(Graph g, boolean[] inTree, Integer active, Integer other) {
        Map<Integer, String> marks = new HashMap<>();
        if (inTree != null) for (int i = 0; i < inTree.length; i++) if (inTree[i]) marks.put(i, "inTree");
        if (active != null) marks.put(active, "active");
        if (other != null) marks.put(other, "joining");
        int[][] ea = g.edgeList().stream().map(e -> new int[]{e.u(), e.v(), e.w()}).toArray(int[][]::new);
        return Snaps.graph(g.n(), false, ea, marks, null);
    }
}
