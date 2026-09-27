# 树状数组（Binary Indexed Tree / Fenwick Tree）

> 源码：`core/tree/FenwickTree.java`（1-based）

## 原理图解

```
lowbit(i) = i & (-i)   —— 取 i 二进制最低位的 1 及其后 0
tree[i] 管辖 (i - lowbit(i), i] 区间的和

i :  1   2   3   4   5   6   7   8
lowbit:1  2   1   4   1   2   1   8
tree[8] 管 1..8   tree[6] 管 5..6   tree[5] 管 5

prefixSum(7) = tree[7] + tree[6] + tree[4]      (7→6→4→0)
     7=111₂ → 110₂ → 100₂ → 0    每步去掉最低位的 1
add(3, d) 更新 tree[3] → tree[4] → tree[8]      (3→4→8→16)
     3=011₂ → 100₂ → 1000₂       每步加上 lowbit
```

## 核心代码

```java
public void add(int index1, int delta) {
    for (int i = index1; i <= n; i += lowbit(i)) tree[i] += delta;
}
public int prefixSum(int index1) {
    int total = 0;
    for (int i = index1; i > 0; i -= lowbit(i)) total += tree[i];
    return total;
}
public int rangeSum(int l, int r) {
    int left = l == 1 ? 0 : prefixSum(l - 1);
    return prefixSum(r) - left;
}
```

## 复杂度推导

- 每次循环 i 的二进制 1 的个数变化 ≤ log₂n 次 → add/prefixSum **O(log n)**，空间 O(n)。
- 对比前缀和数组：静态区间和 O(1) 但单点修改 O(n)；BIT 修改/查询皆 log n。
- 对比线段树：BIT 功能弱（原生只支持“单点改 + 前缀和”这类可逆运算），但常数小、代码短。
- 线性建树 O(n)：`tree[i] += tree[i-1]` 后向上合并，或 `tree[i+lowbit(i)] += tree[i]` 顺序推。

## 易错点

1. **必须 1-based**：lowbit(0)=0 会死循环，所以下标整体 +1。
2. rangeSum(l,r) 在 l==1 时 prefixSum(0) 会触发越界，要特判。
3. add 支持负 delta（减值），但语义仍是“单点加”，不能直接做区间赋值。
4. 区间和公式 `prefixSum(r) - prefixSum(l-1)`，容易把 l-1 写成 l。
5. 逆序对/离散化场景：先对值离散化再逐个 add(i)+1、查询已插入的 ≤a[i] 个数。

## 练习题（附答案）

1. **逆序对计数**
   答：从右往左扫，query(a[i]-1) 得到右侧比它小的个数；或从左往右 query(n) - query(a[i])。O(n log n)。
2. **手推 n=8、add(3,+5) 后哪些 tree 元素变化？**
   答：tree[3]、tree[4]、tree[8]（3→3+1=4→4+4=8→8+8=16>8 停）。
3. **求 prefixSum(6) 用到哪些 tree 元素？**
   答：6=110₂ → tree[6] + tree[4]（6-2=4 → 4-4=0）。
4. **支持区间加、单点查**
   答：差分数组建 BIT：区间 [l,r] 加 d ⇒ add(l,d), add(r+1,-d)；点值 = prefixSum(i)。
5. **BIT 为什么不能高效做区间最值？**
   答：减法不可逆——prefixSum 可用前缀相减，max(prefix(i)) 无法推出 max(l..r)；区间最值要用线段树或 ST 表。
