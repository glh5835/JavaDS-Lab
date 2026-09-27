package com.javadslab.core.graph;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Graph / BFS / DFS / Dijkstra / Bellman-Ford / Floyd / Prim / Kruskal / 拓扑排序。 */
class GraphAlgorithmsTest {

    private Graph sampleGraph() {
        Graph g = new Graph(6, true);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 2, 2);
        g.addEdge(0, 3, 5);
        g.addEdge(3, 4, 1);
        g.addEdge(4, 2, 2);
        g.addEdge(1, 4, 1);
        g.addEdge(4, 5, 1);
        return g;
    }

    /* ================= Graph 结构 ================= */

    @Test
    void graphMatrixAndAdjacencyViews() {
        Graph g = new Graph(3, false);
        g.addEdge(0, 1, 5);
        g.addEdge(1, 2, 2);
        int[][] m = g.matrix();
        assertEquals(5, m[0][1]);
        assertEquals(5, m[1][0]); // 无向对称
        assertEquals(0, m[0][0]);
        assertEquals(Graph.NO_EDGE, m[0][2]);
        assertEquals(2, g.degree(1));
        assertEquals(2, g.edgeList().size());
        assertEquals(2, g.adjacencyList().get(1).size());
    }

    @Test
    void graphDirectedAsymmetry() {
        Graph g = new Graph(2, true);
        g.addEdge(0, 1, 3);
        assertEquals(3, g.matrix()[0][1]);
        assertEquals(Graph.NO_EDGE, g.matrix()[1][0]);
        assertEquals(1, g.degree(0));
        assertEquals(0, g.degree(1));
    }

    @Test
    void graphValidation() {
        Graph g = new Graph(2, false);
        assertThrows(IndexOutOfBoundsException.class, () -> g.addEdge(0, 5, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> g.degree(-1));
        assertThrows(IllegalArgumentException.class, () -> g.addEdge(0, 1, -2)); // 负权被 Graph 拒绝
        assertThrows(IllegalArgumentException.class, () -> new Graph(-1, false));
    }

    /* ================= BFS / DFS ================= */

    @Test
    void bfsOrderAndDistances() {
        Graph g = sampleGraph();
        GraphSearch gs = new GraphSearch();
        List<Integer> order = gs.bfs(g, 0);
        assertEquals(0, order.get(0));
        assertEquals(6, order.size());
        assertEquals(6, new java.util.HashSet<>(order).size()); // 全部访问一次
        int[] dist = gs.bfsDistances(g, 0);
        assertArrayEquals(new int[]{0, 1, 2, 1, 2, 3}, dist); // BFS 是跳数，不是权重
    }

    @Test
    void dfsRecursiveMatchesIterative() {
        Graph g = sampleGraph();
        GraphSearch gs = new GraphSearch();
        assertEquals(gs.dfsRecursive(g, 0), gs.dfsIterative(g, 0));
        assertEquals(6, gs.dfsRecursive(g, 0).size());
        // 不连通图：从单点出发只覆盖连通分量
        Graph disc = new Graph(4, false);
        disc.addEdge(0, 1, 1);
        disc.addEdge(2, 3, 1);
        assertEquals(2, gs.dfsRecursive(disc, 0).size());
        assertEquals(2, gs.bfs(disc, 2).size());
    }

    /* ================= Dijkstra ================= */

    @Test
    void dijkstraShortestPathKnownAnswer() {
        Graph g = sampleGraph();
        ShortestPath sp = new ShortestPath();
        ShortestPath.Result r = sp.dijkstra(g, 0);
        assertArrayEquals(new int[]{0, 1, 3, 5, 2, 3}, r.dist());
        int v = 5;
        assertEquals(3, r.dist()[v]); // 0->1->4->5
        // 回溯 prev 链：5 <- 4 <- 1 <- 0
        List<Integer> path = new ArrayList<>();
        for (int x = 4; x != -1; x = r.prev()[x]) path.add(x);
        assertEquals(List.of(4, 1, 0), path);
    }

    @Test
    void dijkstraUnreachable() {
        Graph g = new Graph(3, true);
        g.addEdge(0, 1, 1);
        ShortestPath.Result r = new ShortestPath().dijkstra(g, 0);
        assertEquals(0, r.dist()[0]);
        assertEquals(1, r.dist()[1]);
        assertTrue(r.dist()[2] >= ShortestPath.INF); // 不可达
    }

    @Test
    void dijkstraRandomCrossCheckWithBellmanFord() {
        Random rnd = new Random(41L);
        ShortestPath sp = new ShortestPath();
        for (int trial = 0; trial < 200; trial++) {
            int n = 2 + rnd.nextInt(12);
            Graph g = new Graph(n, rnd.nextBoolean());
            int m = rnd.nextInt(n * 2);
            for (int i = 0; i < m; i++) {
                g.addEdge(rnd.nextInt(n), rnd.nextInt(n), 1 + rnd.nextInt(10));
            }
            int src = rnd.nextInt(n);
            int[] dj = sp.dijkstra(g, src).dist();
            // Bellman-Ford 吃边表：无向图必须补反向边
            List<Graph.Edge> bfEdges = new ArrayList<>();
            for (Graph.Edge e : g.edgeList()) {
                bfEdges.add(e);
                if (!g.isDirected()) bfEdges.add(new Graph.Edge(e.v(), e.u(), e.w()));
            }
            int[] bf = sp.bellmanFord(n, bfEdges, src);
            assertArrayEquals(bf, dj, "trial " + trial);
        }
    }

    /* ================= Bellman-Ford（负权 + 负环） ================= */

    @Test
    void bellmanFordNegativeWeights() {
        // 直接用边表构造（绕过 Graph 的非负约束）
        List<Graph.Edge> edges = List.of(
                new Graph.Edge(0, 1, 4),
                new Graph.Edge(0, 2, 5),
                new Graph.Edge(1, 3, -3),
                new Graph.Edge(2, 3, 4),
                new Graph.Edge(3, 4, 2));
        int[] dist = new ShortestPath().bellmanFord(5, edges, 0);
        assertArrayEquals(new int[]{0, 4, 5, 1, 3}, dist);
    }

    @Test
    void bellmanFordDetectsNegativeCycle() {
        List<Graph.Edge> edges = List.of(
                new Graph.Edge(0, 1, 1),
                new Graph.Edge(1, 2, -2),
                new Graph.Edge(2, 1, -1)); // 1<->2 负环
        assertThrows(IllegalStateException.class,
                () -> new ShortestPath().bellmanFord(3, edges, 0));
    }

    /* ================= Floyd ================= */

    @Test
    void floydAllPairsMatchesDijkstra() {
        Graph g = sampleGraph();
        ShortestPath sp = new ShortestPath();
        int[][] floyd = sp.floyd(g.n(), g.edgeList(), g.isDirected());
        for (int s = 0; s < g.n(); s++) {
            int[] dj = sp.dijkstra(g, s).dist();
            for (int t = 0; t < g.n(); t++) {
                int want = dj[t] >= ShortestPath.INF ? ShortestPath.INF : dj[t];
                assertEquals(want, floyd[s][t], "s=" + s + " t=" + t);
            }
        }
    }

    @Test
    void floydSmallDirectedGraph() {
        List<Graph.Edge> edges = List.of(
                new Graph.Edge(0, 1, 3),
                new Graph.Edge(1, 2, -2),
                new Graph.Edge(2, 0, 1));
        int[][] d = new ShortestPath().floyd(3, edges, true);
        assertEquals(0, d[0][0]);
        assertEquals(3, d[0][1]);
        assertEquals(1, d[0][2]);  // 0->1->2 = 1
        assertEquals(1, d[2][0]);  // 直达边 2->0 w=1
        assertEquals(4, d[2][1]);  // 2->0->1 = 1+3
    }

    /* ================= Prim / Kruskal ================= */

    @Test
    void primKruskalAgreeOnSample() {
        Graph g = new Graph(5, false);
        g.addEdge(0, 1, 2);
        g.addEdge(0, 3, 6);
        g.addEdge(1, 2, 3);
        g.addEdge(1, 3, 8);
        g.addEdge(1, 4, 5);
        g.addEdge(2, 4, 7);
        g.addEdge(3, 4, 9);
        MinimumSpanningTree mst = new MinimumSpanningTree();
        MinimumSpanningTree.MstResult prim = mst.prim(g, 0);
        MinimumSpanningTree.MstResult kruskal = mst.kruskal(g);
        assertEquals(16L, prim.totalWeight());
        assertEquals(16L, kruskal.totalWeight());
        assertEquals(4, prim.edges().size());
        assertEquals(4, kruskal.edges().size());
    }

    @Test
    void mstDisconnectedThrows() {
        Graph g = new Graph(4, false);
        g.addEdge(0, 1, 1);
        g.addEdge(2, 3, 1);
        assertThrows(IllegalStateException.class, () -> new MinimumSpanningTree().prim(g, 0));
        assertThrows(IllegalStateException.class, () -> new MinimumSpanningTree().kruskal(g));
    }

    @Test
    void mstRandomCrossCheckPrimEqualsKruskal() {
        Random rnd = new Random(42L);
        MinimumSpanningTree mst = new MinimumSpanningTree();
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + rnd.nextInt(12);
            Graph g = new Graph(n, false);
            // 稀疏连通：随机树边 + 额外随机边
            for (int v = 1; v < n; v++) g.addEdge(rnd.nextInt(v), v, 1 + rnd.nextInt(20));
            int extra = rnd.nextInt(n);
            for (int i = 0; i < extra; i++) g.addEdge(rnd.nextInt(n), rnd.nextInt(n), 1 + rnd.nextInt(20));
            long wPrim = mst.prim(g, rnd.nextInt(n)).totalWeight();
            long wKruskal = mst.kruskal(g).totalWeight();
            assertEquals(wPrim, wKruskal, "trial " + trial);
        }
    }

    /* ================= 拓扑排序 ================= */

    @Test
    void kahnAndDfsAgreeOnDag() {
        // 依赖：5->2, 5->0, 4->0, 4->1, 2->3, 3->1 （经典 CLRS 例子）
        Graph g = new Graph(6, true);
        g.addEdge(5, 2, 1);
        g.addEdge(5, 0, 1);
        g.addEdge(4, 0, 1);
        g.addEdge(4, 1, 1);
        g.addEdge(2, 3, 1);
        g.addEdge(3, 1, 1);
        TopologicalSorter ts = new TopologicalSorter();
        List<Integer> kahn = ts.kahn(g);
        List<Integer> dfs = ts.dfsOrder(g);
        assertEquals(6, kahn.size());
        assertEquals(6, dfs.size());
        assertValidTopologicalOrder(g, kahn);
        assertValidTopologicalOrder(g, dfs);
    }

    private void assertValidTopologicalOrder(Graph g, List<Integer> order) {
        int[] pos = new int[g.n()];
        for (int i = 0; i < order.size(); i++) pos[order.get(i)] = i;
        for (Graph.Edge e : g.edgeList()) {
            assertTrue(pos[e.u()] < pos[e.v()], "边 " + e.u() + "->" + e.v() + " 违反拓扑序");
        }
    }

    @Test
    void cycleDetection() {
        Graph cyclic = new Graph(3, true);
        cyclic.addEdge(0, 1, 1);
        cyclic.addEdge(1, 2, 1);
        cyclic.addEdge(2, 0, 1);
        TopologicalSorter ts = new TopologicalSorter();
        assertThrows(IllegalStateException.class, () -> ts.kahn(cyclic));
        assertThrows(IllegalStateException.class, () -> ts.dfsOrder(cyclic));
    }

    @Test
    void undirectedRejected() {
        Graph g = new Graph(2, false);
        assertThrows(IllegalArgumentException.class, () -> new TopologicalSorter().kahn(g));
        assertThrows(IllegalArgumentException.class, () -> new TopologicalSorter().dfsOrder(g));
    }

    /* ================= step-mode 快照 ================= */

    @Test
    void graphTracersProduceValidSnapshots() {
        var tb = new com.javadslab.trace.Tracer("graph", "BFS 演示");
        GraphSearch searcher = new GraphSearch();
        searcher.attachTracer(tb);
        searcher.bfs(sampleGraph(), 0);
        assertTrue(tb.stepCount() > 5);

        var td = new com.javadslab.trace.Tracer("graph", "Dijkstra 演示");
        ShortestPath spTrace = new ShortestPath();
        spTrace.attachTracer(td);
        spTrace.dijkstra(sampleGraph(), 0);
        assertTrue(td.stepCount() > 5);

        var tf = new com.javadslab.trace.Tracer("matrix", "Floyd 演示");
        ShortestPath floydTrace = new ShortestPath();
        floydTrace.attachTracer(tf);
        floydTrace.floyd(sampleGraph().n(), sampleGraph().edgeList(), true);
        assertTrue(tf.stepCount() > 3);

        var tm = new com.javadslab.trace.Tracer("graph", "Prim 演示");
        Graph ug = new Graph(4, false);
        ug.addEdge(0, 1, 1); ug.addEdge(1, 2, 2); ug.addEdge(2, 3, 3); ug.addEdge(0, 3, 9);
        MinimumSpanningTree primTrace = new MinimumSpanningTree();
        primTrace.attachTracer(tm);
        primTrace.prim(ug, 0);
        assertTrue(tm.stepCount() >= 3);

        var tk = new com.javadslab.trace.Tracer("graph", "Kruskal 演示");
        MinimumSpanningTree kruskalTrace = new MinimumSpanningTree();
        kruskalTrace.attachTracer(tk);
        kruskalTrace.kruskal(ug);
        assertTrue(tk.stepCount() >= 3); // 收满 n-1 条边即提前结束，不一定处理所有边

        var tt = new com.javadslab.trace.Tracer("graph", "拓扑排序演示");
        Graph dag = new Graph(4, true);
        dag.addEdge(0, 1, 1); dag.addEdge(1, 2, 1); dag.addEdge(0, 2, 1); dag.addEdge(2, 3, 1);
        TopologicalSorter topoTrace = new TopologicalSorter();
        topoTrace.attachTracer(tt);
        topoTrace.kahn(dag);
        assertTrue(tt.stepCount() >= 4);

        for (var t : List.of(tb, td, tf, tm, tk, tt)) {
            Object parsed = com.javadslab.trace.Json.parse(com.javadslab.trace.Json.write(t.toTrace()));
            for (Object so : com.javadslab.trace.Json.arr(com.javadslab.trace.Json.obj(parsed).get("steps"))) {
                assertNotNull(com.javadslab.trace.Json.obj(so).get("before"));
                assertNotNull(com.javadslab.trace.Json.obj(so).get("after"));
            }
        }
    }
}
