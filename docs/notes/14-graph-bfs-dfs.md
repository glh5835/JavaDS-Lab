# 图的存储 + BFS / DFS

> 源码：`core/graph/Graph.java`（邻接矩阵+邻接表双视图）、`GraphSearch.java`

## 原理图解

```
邻接矩阵                    邻接表
   0  1  2  3               0 → (1,4) (2,2)
0 [0  4  2  ∞]              1 → (0,4) (3,3)
1 [4  0  ∞  3]              2 → (0,2)
2 [2  ∞  0  ∞]              3 → (1,3)
（稠密图省心，O(n²) 空间）   （稀疏图省空间，遍历 O(V+E)）

BFS：队列，按层扩散          DFS：栈/递归，一条路走到底
   0                          0
  / \                        /
 1   2        层1: 1,2      1   ... 回溯换分支
/ \                          |
3   4        层2: 3,4       3
```

## 核心代码

```java
public List<Integer> bfs(Graph g, int start) {
    boolean[] visited = new boolean[g.n()];
    int[] dist = new int[g.n()];
    Arrays.fill(dist, -1);
    List<Integer> order = new ArrayList<>();
    ArrayDeque<Integer> queue = new ArrayDeque<>();
    visited[start] = true; dist[start] = 0; queue.add(start);
    while (!queue.isEmpty()) {
        int u = queue.poll();
        order.add(u);
        for (int[] vw : g.neighbors(u))
            if (!visited[vw[0]]) {
                visited[vw[0]] = true;
                dist[vw[0]] = dist[u] + 1;   // BFS 首达即最短（无权图）
                queue.add(vw[0]);
            }
    }
    return order;
}
// 迭代 DFS：逆序压邻居保持与递归一致的访问次序
for (int i = nbrs.size() - 1; i >= 0; i--)
    if (!visited[nbrs.get(i)[0]]) stack.push(nbrs.get(i)[0]);
```

## 复杂度推导

- BFS/DFS 每个节点入队/入栈一次，每条边检查两次（无向）→ **O(V+E)**。
- 邻接矩阵版找邻居要扫一行 → **O(V²)**。
- BFS 空间 O(V)（队列最宽层），DFS 空间 O(h) 递归栈。
- BFS 的最短性证明：队列单调不减，首达时 dist = dist[parent]+1 已是理论最小（无权图）。

## 易错点

1. **入队时标记 visited**，不是出队时——否则同一节点被多个邻居重复入队，队列膨胀。
2. 迭代 DFS 想与递归顺序一致要**逆序压栈**。
3. 不连通图：单次 BFS/DFS 只覆盖一个连通分量，全图遍历要外层循环所有起点。
4. 有向图 BFS/DFS 与无向图代码相同，只是邻居表方向不同；矩阵视图不对称。
5. 递归 DFS 深链爆栈：10 万节点链式图会 StackOverflow，用迭代版。

## 练习题（附答案）

1. **岛屿数量**
   答：网格图 DFS 洪水填充，访问过置 '#'，岛屿数 = 触发 DFS 的次数。
2. **二分图判定**
   答：BFS 染色，相邻异色；冲突则不是二分图。
3. **迷宫最短路（无权）**
   答：BFS + prev 数组回溯路径；首达即最短。
4. **为什么 BFS 能求无权最短路而 DFS 不能？**
   答：BFS 层序 = 距离序，首达即最少边数；DFS 先深入，到达顺序与边数无关。
5. **欧拉路径判定（存在性）**
   答：连通且奇度顶点数为 0（回路）或 2（路径）。
