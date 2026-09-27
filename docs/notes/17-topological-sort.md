# 拓扑排序（Kahn / DFS 逆后序）

> 源码：`core/graph/TopologicalSorter.java`

## 原理图解

```
DAG:  5→2, 5→0, 4→0, 4→1, 2→3, 3→1

Kahn（入度法）：           DFS 逆后序：
入度: 5:0 4:0 0:2 1:2      递归访问 → 完成(finish)时入栈
队列:[5,4]                  finish 序: 0 1 3 2 5 4（示例）
取5 → 释放 2,0 → 入度减     逆序 = 4 5 2 3 1 0
取4 → ...
有环 ⇒ 队列耗尽时仍有入度>0 的节点（order.size() < n）
```

## 核心代码

```java
public List<Integer> kahn(Graph g) {
    int[] indegree = new int[g.n()];
    for (int u = 0; u < g.n(); u++)
        for (int[] vw : g.neighbors(u)) indegree[vw[0]]++;
    Deque<Integer> queue = new ArrayDeque<>();
    for (int i = 0; i < g.n(); i++) if (indegree[i] == 0) queue.add(i);
    List<Integer> order = new ArrayList<>(g.n());
    while (!queue.isEmpty()) {
        int u = queue.poll();
        order.add(u);
        for (int[] vw : g.neighbors(u))
            if (--indegree[vw[0]] == 0) queue.add(vw[0]);   // 入度归零才解锁
    }
    if (order.size() != g.n()) throw new IllegalStateException("图中存在环");
    return order;
}
// DFS 版：迭代栈 + 三色标记，回边(u→灰)即有环；完成序取逆
```

## 复杂度推导

- Kahn：入度统计 O(E) + 每节点出队一次、每边释放一次 → **O(V+E)**。
- DFS 版：同 O(V+E)，空间 O(V)。
- 拓扑序**不唯一**（入度为 0 的节点任意顺序）。
- 正确性：出队时所有前驱已输出 ⇔ 每条边 u→v 满足 pos[u] < pos[v]（测试里逐边验证）。

## 易错点

1. 只能用于 **DAG**；有环时 Kahn 的 `order.size() < n` 是检测手段，别静默返回残缺序。
2. 释放入度写在 `--indegree[v]==0` 里，先减再判，不要减两次。
3. DFS 版**逆后序**不是“先序的反”，是完成序的反；三色标记灰遇灰 = 回边 = 环。
4. 迭代 DFS 需要 {node, 邻居游标} 帧结构，纯值栈会漏回溯。
5. 无向图拓扑排序无意义（环到处都是），入口应拒绝。

## 练习题（附答案）

1. **课程表 I/II**（207/210）
   答：Kahn 模板；order 长度 < n 则不可能修完。
2. **拓扑排序 + 字典序最小**
   答：把队列换小顶堆（MyHeap），每次取最小编号。O((V+E) log V)。
3. **关键路径（AOE 网）**
   答：拓扑序求最早开始（正向松弛 max），逆拓扑求最迟；差 0 的边在关键路径上。
4. **统计 DAG 中到达 v 的路径数**
   答：DP：按拓扑序 `paths[v] = Σ paths[u] (u→v)`，起点 1。
5. **为什么 DFS 逆后序就是拓扑序？**
   答：对任意边 u→v，DFS 中 v 一定比 u 先完成（v 是 u 的后代或 v 已完成），完成序里 v 在 u 前，逆序后 u 在 v 前。
