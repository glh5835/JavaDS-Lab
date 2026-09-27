# 最小生成树：Prim / Kruskal

> 源码：`core/graph/MinimumSpanningTree.java`（Prim 复用 MyHeap，Kruskal 复用 MergeSort + UnionFind）

## 原理图解

```
Prim（点视角：树向外“长”）          Kruskal（边视角：全局排序贪心）
      [A]                             边按权升序：1,2,3,4,...
     /   \                            逐条尝试，并查集判环：
    B     C  ── 每次取“树→非树”        accept: 两端不同集合 → 并入
   1|     |2   最小横切边              reject: 两端同集合 → 成环丢弃
    D ──5── E                          直到选出 n-1 条边

横切边定理：任何横切的最小边一定属于某棵 MST —— 两个算法的共同正确性来源
```

## 核心代码

```java
// Prim：堆里放 {from, to, w}，pop 时过滤已在树中的过期条目
MyHeap<int[]> heap = new MyHeap<>((a, b) -> Integer.compare(a[2], b[2]));
inTree[start] = true;
pushNeighbors(g, start, heap);
while (!heap.isEmpty() && treeEdges.size() < n - 1) {
    int[] top = heap.pop();
    if (inTree[top[1]]) continue;          // 惰性删除
    inTree[top[1]] = true;
    total += top[2];
    treeEdges.add(new Graph.Edge(top[0], top[1], top[2]));
    pushNeighbors(g, top[1], heap);
}

// Kruskal：手写归并排序 + 并查集
Graph.Edge[] arr = edges.toArray(new Graph.Edge[0]);
MergeSort.sort(arr, (a, b) -> Integer.compare(a.w(), b.w()));  // 禁用 java.util 排序
UnionFind uf = new UnionFind(n);
for (Graph.Edge e : arr) {
    if (uf.union(e.u(), e.v())) { picked.add(e); total += e.w(); }
    if (picked.size() == n - 1) break;
}
```

## 复杂度推导

- Prim 堆版：每条边至多入堆一次 O(E log E)=O(E log V) → **O(E log V)**；二叉堆版 O(E log V)。
- Kruskal：排序 **O(E log E)** 主导 + 并查集近 O(E α(V)) → **O(E log E)**。
- 稀疏图（E≈V）两者同阶；稠密图（E≈V²）Prim 更稳。Kruskal 天然适合“边流式给出”的场景。
- MST 唯一性：边权两两不同 ⇔ MST 唯一。

## 易错点

1. Prim/Kruskal 都要求**无向连通图**，不连通应抛异常（本实现检测 `picked.size() != n-1`）。
2. Prim 的惰性删除：pop 出的边若 to 已在树中直接跳过，不要试图从堆中真删。
3. Kruskal 排序**必须用手写排序**（本项目禁 java.util 排序充当核心逻辑，用 MergeSort 泛型版）。
4. 并查集 union 返回 false 即成环，继续扫下一条边而不是终止。
5. 断言 MST 权重和用 long（E 条边累加可能溢 int）。

## 练习题（附答案）

1. **证明横切边定理**
   答：反证：若最小横切边 e 不在任何 MST 中，取任一 MST，加入 e 成环，环必跨切分，环上另一横切边 e' 权 ≥ e；删 e' 得更小/等大生成树，矛盾。
2. **网络延迟时间（到最远节点）**
   答：Dijkstra 后取 max(dist)。
3. **连接所有点的最小费用（坐标曼哈顿距离）**
   答：完全图 E=O(n²) 边排序 → Kruskal O(n² log n)；或 Prim。
4. **如果图中边权有负数，Prim/Kruskal 还正确吗？**
   答：正确！MST 不受负权影响（不同于最短路），因为横切边定理只比大小。
5. **次小生成树**
   答：先求 MST；枚举非树边 (u,v,w)，加它替换 u-v 路径上最大树边，取最小增量。
