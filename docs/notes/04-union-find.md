# 并查集（Disjoint Set Union）

> 源码：`core/unionfind/UnionFind.java`；被 Kruskal 复用（`core/graph/MinimumSpanningTree.java`）

## 原理图解

```
初始:      0  1  2  3  4  5   （每元素自成一棵树）
union(0,1): parent[1]=0        0 ──▶ 1
union(1,2): 0 ──▶ 1, 0 ──▶ 2   （按大小合并：小树挂大树）
find(2) 路径压缩:  0 ◀── 1, 2   （沿途节点直接挂根，树变矮）
```

## 核心代码

```java
public int find(int x) {
    int root = x;
    while (parent[root] != root) root = parent[root]; // 找根
    while (parent[x] != root) {                        // 路径压缩
        int next = parent[x];
        parent[x] = root;
        x = next;
    }
    return root;
}
public boolean union(int a, int b) {
    int ra = find(a), rb = find(b);
    if (ra == rb) return false;
    if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; } // 小挂大
    parent[rb] = ra;
    size[ra] += size[rb];
    components--;
    return true;
}
```

## 复杂度推导

- 只做路径压缩：单次 find 均摊 O(log n)。
- 路径压缩 + 按大小/秩合并：单次操作均摊 **O(α(n))**，α 为反阿克曼函数，n ≤ 10^600 时 α(n) ≤ 4，视为常数。
- 直观理解：合并保证树高 O(log n)，压缩把已访问节点全部拉平，两者配合使任何单条链不会被反复走。

## 易错点

1. find 里**先找根再压缩**，边走边改 parent 会把链改断（丢 next 引用）。
2. 合并比较的是 `size[root]` 不是 `size[a]`。
3. 判断连通用 `find(a)==find(b)`，直接比 parent 数组值可能不是根。
4. `union` 返回 boolean（是否真的合并）在 Kruskal 里用来判环，勿写成 void。
5. components 计数在“已同集合”时不能减。

## 练习题（附答案）

1. **省份数量**（LeetCode 547）
   答：对每对相邻关系 union，答案 = 最终 components。
2. **冗余连接**（684）
   答：逐边 union，第一次 union 返回 false 的边即成环边。
3. **账户合并**（721）
   答：邮箱→并查集，同账户邮箱 union；再按根分组输出。
4. **若只按大小合并不做路径压缩，最坏树高是多少？**
   答：O(log n)。每次合并小树挂大树，任一节点所在树至少翻倍才增高 1。
5. **带上权并查集**（食物链类）
   答：parent 外加 relation[x] 表示 x 到 parent 的权；find 时压缩路径同时累加权值模 3。
