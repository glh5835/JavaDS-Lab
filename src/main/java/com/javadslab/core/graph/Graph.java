package com.javadslab.core.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * 手写图：同时提供邻接矩阵与邻接表两种存储视图。
 * 构造时指定节点数与是否有向；addEdge 统一登记边表，matrix()/adjacencyList() 按需给出视图。
 * 权重默认 1（无权图用法）；无边的矩阵位置为 NO_EDGE。
 */
public class Graph {

    public static final int NO_EDGE = Integer.MAX_VALUE;

    /** 有权边（无权图 weight=1）。 */
    public record Edge(int u, int v, int w) {}

    private final int n;
    private final boolean directed;
    private final List<Edge> edges = new ArrayList<>();
    /** 邻接表：adj[u] 中为 {v, w} 对（无向图双向登记）。 */
    private final List<List<int[]>> adj;

    public Graph(int n, boolean directed) {
        if (n < 0) throw new IllegalArgumentException("节点数不能为负: " + n);
        this.n = n;
        this.directed = directed;
        this.adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    }

    public int n() { return n; }

    public boolean isDirected() { return directed; }

    /** 加边；节点越界或负权重抛异常（Dijkstra 的前置约束）。 */
    public void addEdge(int u, int v, int w) {
        checkNode(u);
        checkNode(v);
        if (w < 0) throw new IllegalArgumentException("Graph 不接受负权边（请用专门的负权算法图）: " + u + "->" + v + " w=" + w);
        edges.add(new Edge(u, v, w));
        adj.get(u).add(new int[]{v, w});
        if (!directed && u != v) adj.get(v).add(new int[]{u, w});
    }

    public void addEdge(int u, int v) {
        addEdge(u, v, 1);
    }

    /** 邻接矩阵视图：matrix[u][v] = w，无边为 NO_EDGE。O(n^2) 构建。 */
    public int[][] matrix() {
        int[][] m = new int[n][n];
        for (int[] row : m) java.util.Arrays.fill(row, NO_EDGE);
        for (int i = 0; i < n; i++) m[i][i] = 0;
        for (Edge e : edges) {
            m[e.u()][e.v()] = Math.min(m[e.u()][e.v()], e.w());
            if (!directed) m[e.v()][e.u()] = Math.min(m[e.v()][e.u()], e.w());
        }
        return m;
    }

    /** 邻接表视图（内部结构的只读代理）。 */
    public List<List<int[]>> adjacencyList() {
        List<List<int[]>> copy = new ArrayList<>(n);
        for (List<int[]> l : adj) copy.add(List.copyOf(l));
        return copy;
    }

    public List<Edge> edgeList() {
        return List.copyOf(edges);
    }

    /** 节点 u 的邻居 {v,w} 列表。 */
    public List<int[]> neighbors(int u) {
        checkNode(u);
        return adj.get(u);
    }

    /** 度（有向图为出度）。 */
    public int degree(int u) {
        checkNode(u);
        return adj.get(u).size();
    }

    private void checkNode(int u) {
        if (u < 0 || u >= n) throw new IndexOutOfBoundsException("节点越界: " + u + ", n=" + n);
    }
}
