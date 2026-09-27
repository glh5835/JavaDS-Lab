package com.javadslab.app;

import com.javadslab.core.graph.Graph;
import com.javadslab.core.graph.GraphSearch;
import com.javadslab.core.graph.MinimumSpanningTree;
import com.javadslab.core.graph.ShortestPath;
import com.javadslab.core.graph.TopologicalSorter;
import com.javadslab.core.hash.ChainingHashMap;
import com.javadslab.core.hash.OpenAddressingHashMap;
import com.javadslab.core.heap.MyHeap;
import com.javadslab.core.linear.DoublyLinkedList;
import com.javadslab.core.linear.MyArrayList;
import com.javadslab.core.linear.SinglyLinkedList;
import com.javadslab.core.stackqueue.ArrayQueue;
import com.javadslab.core.stackqueue.ArrayStack;
import com.javadslab.core.stackqueue.CircularQueue;
import com.javadslab.core.stackqueue.MonotonicStack;
import com.javadslab.core.tree.AVLTree;
import com.javadslab.core.tree.BTree;
import com.javadslab.core.tree.BST;
import com.javadslab.core.tree.BinaryTrees;
import com.javadslab.core.tree.FenwickTree;
import com.javadslab.core.tree.RedBlackTree;
import com.javadslab.core.tree.SegmentTree;
import com.javadslab.core.tree.Trie;
import com.javadslab.core.unionfind.UnionFind;
import com.javadslab.trace.Json;
import com.javadslab.trace.TraceableDP;
import com.javadslab.trace.TraceableSearch;
import com.javadslab.trace.TraceableSorts;
import com.javadslab.trace.Tracer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * trace 生成器：为每个数据结构/算法产出至少一个演示 trace（JSON）到 trace/ 目录，
 * 并打包一份 web/traces-bundle.js 供播放器在 file:// 下直接加载。
 * 运行：java -cp target/classes com.javadslab.app.TraceGenerator [项目根目录]
 */
public final class TraceGenerator {

    public static void main(String[] args) throws IOException {
        Path root = Paths.get(args.length > 0 ? args[0] : ".").toAbsolutePath();
        Path traceDir = root.resolve("trace");
        Path webDir = root.resolve("web");
        Files.createDirectories(traceDir);
        Files.createDirectories(webDir);

        Map<String, Object> all = new LinkedHashMap<>(); // name -> trace 对象
        register(all, "arraylist", new MyArrayListDemo().run());
        register(all, "singly-linked-list", new SinglyLinkedListDemo().run());
        register(all, "doubly-linked-list", new DoublyLinkedListDemo().run());
        register(all, "stack", new StackDemo().run());
        register(all, "queue", new QueueDemo().run());
        register(all, "circular-queue", new CircularQueueDemo().run());
        register(all, "monotonic-stack", new MonotonicStackDemo().run());
        register(all, "hash-chaining", new HashChainingDemo().run());
        register(all, "hash-open-addressing", new HashOpenDemo().run());
        register(all, "union-find", new UnionFindDemo().run());
        register(all, "heap", new HeapDemo().run());
        register(all, "trie", new TrieDemo().run());
        register(all, "segment-tree", new SegmentTreeDemo().run());
        register(all, "fenwick-tree", new FenwickTreeDemo().run());
        register(all, "bst", new BSTDemo().run());
        register(all, "avl", new AVLDemo().run());
        register(all, "red-black-tree", new RedBlackDemo().run());
        register(all, "btree", new BTreeDemo().run());
        register(all, "bfs", new BfsDemo().run());
        register(all, "dfs", new DfsDemo().run());
        register(all, "dijkstra", new DijkstraDemo().run());
        register(all, "floyd", new FloydDemo().run());
        register(all, "prim", new PrimDemo().run());
        register(all, "kruskal", new KruskalDemo().run());
        register(all, "topological-sort", new TopoDemo().run());
        register(all, "sort-bubble", TraceableSorts.bubbleSort(new int[]{5, 2, 9, 1, 7, 3}));
        register(all, "sort-insertion", TraceableSorts.insertionSort(new int[]{6, 3, 8, 1, 5}));
        register(all, "sort-selection", TraceableSorts.selectionSort(new int[]{7, 2, 5, 3, 8}));
        register(all, "sort-quick", TraceableSorts.quickSort(new int[]{8, 3, 5, 2, 9, 1, 6}));
        register(all, "sort-heap", TraceableSorts.heapSort(new int[]{4, 10, 3, 5, 1}));
        register(all, "binary-search", TraceableSearch.binarySearch(new int[]{1, 3, 5, 7, 9, 11, 13, 15}, 7));
        register(all, "kmp", TraceableSearch.kmp("ababcababd", "ababd"));
        register(all, "dp-lcs", TraceableDP.lcs("ABCBDAB", "BDCABA"));
        register(all, "dp-knapsack", TraceableDP.knapsack01(new int[]{2, 3, 4, 5}, new int[]{3, 4, 5, 6}, 8));

        // 逐个写 trace/<name>.json
        for (Map.Entry<String, Object> e : all.entrySet()) {
            Path p = traceDir.resolve(e.getKey() + ".json");
            Files.writeString(p, Json.writePretty(e.getValue()), StandardCharsets.UTF_8);
        }
        // 播放器 bundle（file:// 下免 CORS）
        StringBuilder js = new StringBuilder("/* 自动生成：勿手改。由 TraceGenerator 输出 */\nwindow.TRACE_BUNDLE = {\n");
        int i = 0;
        for (Map.Entry<String, Object> e : all.entrySet()) {
            js.append("  \"").append(e.getKey()).append("\": ").append(Json.write(e.getValue()));
            if (++i < all.size()) js.append(',');
            js.append('\n');
        }
        js.append("};\n");
        Files.writeString(webDir.resolve("traces-bundle.js"), js.toString(), StandardCharsets.UTF_8);
        // 清单
        StringBuilder manifest = new StringBuilder("[");
        i = 0;
        for (String name : all.keySet()) {
            manifest.append('"').append(name).append('"');
            if (++i < all.size()) manifest.append(", ");
        }
        manifest.append(']');
        Files.writeString(traceDir.resolve("manifest.json"), manifest + "\n", StandardCharsets.UTF_8);

        System.out.println("生成 " + all.size() + " 个 trace -> " + traceDir);
    }

    private static void register(Map<String, Object> all, String name, Tracer t) {
        all.put(name, t.toTrace());
    }

    /* ---------------- 各结构演示脚本 ---------------- */

    static class MyArrayListDemo {
        Tracer run() {
            Tracer t = new Tracer("array", "动态数组 MyArrayList");
            MyArrayList<Integer> l = new MyArrayList<>();
            l.attachTracer(t);
            for (int v : new int[]{10, 20, 30, 40}) l.add(v);
            l.add(1, 15);
            l.set(0, 5);
            l.remove(3);
            return t;
        }
    }

    static class SinglyLinkedListDemo {
        Tracer run() {
            Tracer t = new Tracer("linked", "单链表");
            SinglyLinkedList<Integer> l = new SinglyLinkedList<>();
            l.attachTracer(t);
            l.addLast(1); l.addLast(2); l.addFirst(0); l.addLast(3);
            l.removeAt(2);
            l.add(2, 99);
            return t;
        }
    }

    static class DoublyLinkedListDemo {
        Tracer run() {
            Tracer t = new Tracer("linked", "双向链表（哨兵）");
            DoublyLinkedList<Integer> l = new DoublyLinkedList<>();
            l.attachTracer(t);
            l.addLast(1); l.addLast(2); l.addFirst(-1); l.removeLast(); l.add(1, 8);
            return t;
        }
    }

    static class StackDemo {
        Tracer run() {
            Tracer t = new Tracer("array", "顺序栈 ArrayStack");
            ArrayStack<String> s = new ArrayStack<>();
            s.attachTracer(t);
            s.push("a"); s.push("b"); s.push("c"); s.pop(); s.push("d");
            return t;
        }
    }

    static class QueueDemo {
        Tracer run() {
            Tracer t = new Tracer("array", "队列 ArrayQueue");
            ArrayQueue<Integer> q = new ArrayQueue<>();
            q.attachTracer(t);
            q.offer(1); q.offer(2); q.poll(); q.offer(3); q.poll(); q.offer(4);
            return t;
        }
    }

    static class CircularQueueDemo {
        Tracer run() {
            Tracer t = new Tracer("array", "循环队列（容量 4，注意 head/tail 回绕）");
            CircularQueue<Integer> q = new CircularQueue<>(4);
            q.attachTracer(t);
            q.offer(1); q.offer(2); q.offer(3); q.poll(); q.offer(4); q.poll(); q.offer(5);
            return t;
        }
    }

    static class MonotonicStackDemo {
        Tracer run() {
            Tracer t = new Tracer("monostack", "单调栈：下一个更大元素");
            MonotonicStack ms = new MonotonicStack();
            ms.attachTracer(t);
            ms.nextGreaterIndex(new int[]{2, 1, 5, 6, 2, 3});
            return t;
        }
    }

    static class HashChainingDemo {
        Tracer run() {
            Tracer t = new Tracer("hash", "哈希表（拉链法）");
            ChainingHashMap<Integer, String> m = new ChainingHashMap<>(4);
            m.attachTracer(t);
            for (int i = 1; i <= 6; i++) m.put(i * 3, "v" + i); // 制造冲突
            m.remove(9);
            m.put(100, "new");
            return t;
        }
    }

    static class HashOpenDemo {
        Tracer run() {
            Tracer t = new Tracer("hash", "哈希表（开放寻址 + 墓碑）");
            OpenAddressingHashMap<Integer, String> m = new OpenAddressingHashMap<>(8);
            m.attachTracer(t);
            m.put(10, "a"); m.put(18, "b"); m.put(26, "c"); // 同桶冲突链
            m.remove(18); // 墓碑
            m.put(34, "d");
            return t;
        }
    }

    static class UnionFindDemo {
        Tracer run() {
            Tracer t = new Tracer("uf", "并查集（路径压缩 + 按大小合并）");
            UnionFind uf = new UnionFind(8);
            uf.attachTracer(t);
            uf.union(0, 1); uf.union(1, 2); uf.union(3, 4); uf.union(2, 4); uf.union(5, 6); uf.union(0, 3);
            uf.connected(1, 4);
            return t;
        }
    }

    static class HeapDemo {
        Tracer run() {
            Tracer t = new Tracer("heap", "二叉堆（小顶）：push 与 pop 的上浮/下沉");
            MyHeap<Integer> h = new MyHeap<>();
            h.attachTracer(t);
            h.push(8); h.push(3); h.push(5); h.push(1); h.push(9);
            h.pop(); h.pop();
            return t;
        }
    }

    static class TrieDemo {
        Tracer run() {
            Tracer t = new Tracer("tree", "Trie 前缀树");
            Trie trie = new Trie();
            trie.attachTracer(t);
            trie.insert("cat"); trie.insert("car"); trie.insert("card"); trie.insert("dog");
            trie.delete("car");
            return t;
        }
    }

    static class SegmentTreeDemo {
        Tracer run() {
            Tracer t = new Tracer("tree", "线段树（区间和）");
            SegmentTree st = new SegmentTree(new int[]{2, 5, 1, 4, 9, 3});
            st.attachTracer(t);
            st.query(1, 4);
            st.update(3, 10);
            st.query(0, 5);
            return t;
        }
    }

    static class FenwickTreeDemo {
        Tracer run() {
            Tracer t = new Tracer("array", "树状数组：add 与 prefixSum 的 lowbit 跳跃");
            FenwickTree ft = new FenwickTree(8);
            ft.attachTracer(t);
            ft.add(3, 5); ft.add(5, 2); ft.add(8, 1);
            ft.prefixSum(7);
            return t;
        }
    }

    static class BSTDemo {
        Tracer run() {
            Tracer t = new Tracer("tree", "二叉搜索树");
            BST bst = new BST();
            bst.attachTracer(t);
            bst.insert(50); bst.insert(30); bst.insert(70); bst.insert(20); bst.insert(40); bst.insert(60); bst.insert(80);
            bst.remove(30); // 双孩删除：后继顶替
            return t;
        }
    }

    static class AVLDemo {
        Tracer run() {
            Tracer t = new Tracer("tree", "AVL 树（插入触发旋转）");
            AVLTree avl = new AVLTree();
            avl.attachTracer(t);
            avl.insert(30); avl.insert(20); avl.insert(10);  // LL -> 右旋
            avl.insert(40); avl.insert(50);                   // RR -> 左旋
            avl.insert(25);                                   // 触发 LR/调整
            return t;
        }
    }

    static class RedBlackDemo {
        Tracer run() {
            Tracer t = new Tracer("tree", "红黑树（变色与旋转）");
            RedBlackTree rb = new RedBlackTree();
            rb.attachTracer(t);
            rb.insert(10); rb.insert(20); rb.insert(30); rb.insert(15); rb.insert(25); rb.insert(5);
            rb.remove(20);
            return t;
        }
    }

    static class BTreeDemo {
        Tracer run() {
            Tracer t = new Tracer("tree", "B 树（t=2，节点分裂）");
            BTree bt = new BTree(2);
            bt.attachTracer(t);
            for (int k : new int[]{10, 20, 30, 40, 50, 60, 70}) bt.insert(k);
            bt.remove(20);
            return t;
        }
    }

    static Graph demoGraph() {
        Graph g = new Graph(6, true);
        g.addEdge(0, 1, 4);
        g.addEdge(0, 2, 2);
        g.addEdge(1, 2, 1);
        g.addEdge(1, 3, 5);
        g.addEdge(2, 3, 8);
        g.addEdge(2, 4, 10);
        g.addEdge(3, 5, 3);
        g.addEdge(4, 5, 4);
        g.addEdge(3, 4, 2);
        return g;
    }

    static Graph demoUndirected() {
        Graph g = new Graph(6, false);
        g.addEdge(0, 1, 7);
        g.addEdge(0, 2, 9);
        g.addEdge(0, 5, 14);
        g.addEdge(1, 2, 10);
        g.addEdge(1, 3, 15);
        g.addEdge(2, 3, 11);
        g.addEdge(2, 5, 2);
        g.addEdge(3, 4, 6);
        g.addEdge(4, 5, 9);
        return g;
    }

    static class BfsDemo {
        Tracer run() {
            Tracer t = new Tracer("graph", "广度优先搜索 BFS");
            GraphSearch search = new GraphSearch();
            search.attachTracer(t);
            search.bfs(demoGraph(), 0);
            return t;
        }
    }

    static class DfsDemo {
        Tracer run() {
            Tracer t = new Tracer("graph", "深度优先搜索 DFS");
            GraphSearch dfs = new GraphSearch();
            dfs.attachTracer(t);
            dfs.dfsRecursive(demoGraph(), 0);
            return t;
        }
    }

    static class DijkstraDemo {
        Tracer run() {
            Tracer t = new Tracer("graph", "Dijkstra 最短路（堆优化）");
            ShortestPath dij = new ShortestPath();
            dij.attachTracer(t);
            dij.dijkstra(demoGraph(), 0);
            return t;
        }
    }

    static class FloydDemo {
        Tracer run() {
            Tracer t = new Tracer("matrix", "Floyd 全源最短路（DP 表）");
            Graph g = demoUndirected();
            ShortestPath fl = new ShortestPath();
            fl.attachTracer(t);
            fl.floyd(g.n(), g.edgeList(), false);
            return t;
        }
    }

    static class PrimDemo {
        Tracer run() {
            Tracer t = new Tracer("graph", "Prim 最小生成树");
            MinimumSpanningTree prim = new MinimumSpanningTree();
            prim.attachTracer(t);
            prim.prim(demoUndirected(), 0);
            return t;
        }
    }

    static class KruskalDemo {
        Tracer run() {
            Tracer t = new Tracer("graph", "Kruskal 最小生成树（排序 + 并查集）");
            MinimumSpanningTree kru = new MinimumSpanningTree();
            kru.attachTracer(t);
            kru.kruskal(demoUndirected());
            return t;
        }
    }

    static class TopoDemo {
        Tracer run() {
            Tracer t = new Tracer("graph", "拓扑排序（Kahn 入度法）");
            Graph dag = new Graph(7, true);
            dag.addEdge(0, 1, 1); dag.addEdge(0, 2, 1); dag.addEdge(1, 3, 1);
            dag.addEdge(2, 3, 1); dag.addEdge(3, 4, 1); dag.addEdge(2, 5, 1); dag.addEdge(5, 6, 1);
            TopologicalSorter topo = new TopologicalSorter();
            topo.attachTracer(t);
            topo.kahn(dag);
            return t;
        }
    }
}
