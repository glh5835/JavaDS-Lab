# 最短路三算法：Dijkstra / Bellman-Ford / Floyd

> 源码：`core/graph/ShortestPath.java`

## 原理图解

```
Dijkstra（贪心，堆优化）—— 非负权
  0 --4--> 1 --5--> 3
  |        ↑        0→1→3 = 9
  2 --8--> (2→3=10) 0→2→3 = 10 ... 每轮“定居”未定中 dist 最小者

Bellman-Ford（DP）—— 可负权，第 n 轮仍能松弛 ⇒ 负环
  round 1..n-1: 对每条边 (u,v,w) 尝试 dist[v] = min(dist[v], dist[u]+w)

Floyd（区间 DP）—— 全源
  dp_k[i][j] = 允许中转点 0..k 时 i→j 最短路
  dp_k[i][j] = min(dp_{k-1}[i][j], dp_{k-1}[i][k] + dp_{k-1}[k][j])
```

## 核心代码

```java
// Dijkstra + 惰性删除堆
MyHeap<int[]> heap = new MyHeap<>((a, b) -> Integer.compare(a[1], b[1]));
dist[src] = 0;
heap.push(new int[]{src, 0});
while (!heap.isEmpty()) {
    int[] top = heap.pop();
    int u = top[0];
    if (settled[u]) continue;              // 过期条目跳过
    settled[u] = true;
    for (int[] vw : g.neighbors(u))
        if (!settled[vw[0]] && dist[u] + vw[1] < dist[vw[0]]) {
            dist[vw[0]] = dist[u] + vw[1];
            heap.push(new int[]{vw[0], dist[vw[0]]});  // 旧条目留在堆里变废条
        }
}

// Floyd 三重循环，k 在最外层！
for (int k = 0; k < n; k++)
    for (int i = 0; i < n; i++)
        for (int j = 0; j < n; j++)
            if (d[i][k] + d[k][j] < d[i][j]) d[i][j] = d[i][k] + d[k][j];
```

## 复杂度推导

- Dijkstra 堆版：每条边可能产生一次入堆 O(E)，每次出堆 O(log E) → **O(E log V)**；贪心正确性依赖“边权非负”（定居点距离不会再变小）。
- Bellman-Ford：n-1 轮 × E 条边 → **O(VE)**；第 n 轮若仍可松弛 ⇒ 存在负环（每绕一圈 dist 更小，最短路无定义）。
- Floyd：三重循环 **O(V³)**，空间 O(V²)；适合稠密图/全源。k 必须在最外层——内层用的是 dp_{k-1} 的值（k 在外保证同轮不被覆盖，实际因对称性 k 内层也常见正确，但教学规范是 k 最外）。

## 易错点

1. **负权边禁止用 Dijkstra**（贪心前提被破坏），构造反例：0→1(1), 0→2(2), 2→1(-2)。
2. INF 用 `Integer.MAX_VALUE/2` 防加法溢出；判断可达用 `dist < INF` 而非 `!= INF`（松弛可能造出 INF±ε）。
3. Bellman-Ford 负环检测要在**第 n 轮**再做一次判定，而不是循环里直接抛。
4. 无向图喂给 Bellman-Ford 前要**补反向边**（本项目随机对照测试踩过：edgeList 只存单方向）。
5. Floyd 的 k 忘了放最外层会得到错误但“看起来合理”的结果，随机对照能抓出来。

## 练习题（附答案）

1. **为什么 Dijkstra 不能有负边？** 反例
   答：若先定居了 dist=2 的 0→1，而 0→2→1 总长 1，定居顺序被破坏，结果错误（上面已给反例）。
2. **判断负环**
   答：Bellman-Ford 第 n 轮仍可松弛 ⇒ 负环；或对 super 源跑一遍。
3. **Floyd 求传递闭包**
   答：把 min 换成逻辑或：`reach[i][j] |= reach[i][k] && reach[k][j]`。
4. **稠密图上 Floyd 与 n 次 Dijkstra 谁快？**
   答：Floyd O(V³) 常数小、无堆开销；堆 Dijkstra n 次 O(V·E log V) ≈ O(V³ log V)，稠密图 Floyd 更优。
5. **Dijkstra 记录路径**
   答：松弛时 `prev[v]=u`，终点回溯 prev 链倒序输出。
