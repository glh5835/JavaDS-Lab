# 线段树（区间和，递归版）

> 源码：`core/tree/SegmentTree.java`

## 原理图解

```
a = [2, 5, 1, 4, 9, 3]      节点标注 [l,r]=sum
                [0,5]=24
            ┌─────────────────┐
        [0,2]=8              [3,5]=16
       ┌────┴────┐          ┌────┴────┐
    [0,1]=7   [2,2]=1   [3,4]=13  [5,5]=3
    ┌───┴───┐           ┌───┴───┐
 [0,0]=2 [1,1]=5    [3,3]=4 [4,4]=9

query(1,4)：[0,2] 部分命中 → 左右分治；[3,4] 完整命中 → 直接返回 13
```

## 核心代码

```java
public void update(int index, int value) { updateRec(1, 0, n - 1, index, value); }
private void updateRec(int node, int lo, int hi, int index, int value) {
    if (lo == hi) { sum[node] = value; return; }      // 叶子
    int mid = (lo + hi) >>> 1;
    if (index <= mid) updateRec(2*node, lo, mid, index, value);
    else              updateRec(2*node + 1, mid+1, hi, index, value);
    pull(node);                                        // 回溯合并
}
public int query(int l, int r) { return queryRec(1, 0, n - 1, l, r); }
private int queryRec(int node, int lo, int hi, int l, int r) {
    if (l <= lo && hi <= r) return sum[node];          // 完整覆盖
    int mid = (lo + hi) >>> 1, total = 0;
    if (l <= mid) total += queryRec(2*node, lo, mid, l, r);
    if (r > mid)  total += queryRec(2*node+1, mid+1, hi, l, r);
    return total;
}
```

## 复杂度推导

- 每层最多访问 4 个节点（覆盖区间左右端各分裂一次），树高 log n → query/update **O(log n)**。
- 空间：满二叉树 4n 保守够用（精确界 2·2^⌈log n⌉ ≤ 4n）。
- build：每节点 O(1) 合并，节点数 O(n) → **O(n)**。
- 若支持区间赋值需引入 **lazy 懒标记**：整段覆盖时打标记不下传，pushdown 在访问子节点时结算。

## 易错点

1. 开 `4n` 而不是 `2n`——n 非幂时 2n 会越界。
2. 递归分治的判断顺序：先判完整覆盖（`l<=lo && hi<=r`），再分左右；漏写 `r > mid` 会重复累计。
3. 单点赋值回溯必须 `pull`，忘了则祖先区间和全部过期。
4. 区间约定要统一（闭区间 [l,r]），混用半开区间是 bug 之源。
5. 泛化时（区间加/区间最值）merge 函数与 lazy 标记的合并顺序必须满足结合律。

## 练习题（附答案）

1. **区间加 + 区间和**
   答：lazy 记加值 add：`sum += (hi-lo+1)*add`；pushdown 把 add 传给孩子，父清零。
2. **区间最大值线段树**
   答：merge 用 max；叶子存原值；查询同理。把 SUM 换成 MAX 即可。
3. **为什么递归线段树最多分裂成 O(log n) 个完整节点？**
   答：每层被 [l,r] 边界“切”的节点至多 2 个，其余完整命中；层深 log n。
4. **动态开点线段树适用什么场景？**
   答：值域大且稀疏（如 [0,1e9]），不预建满树，访问时才创建子节点。
5. **用两个树状数组支持区间加 + 点查，怎么推公式？**
   答：差分思想——区间加 delta 相当于 diff 数组两个单点加；前缀和即点值。需要 b1[i]=d[i], b2[i]=i·d[i] 两个 BIT。
