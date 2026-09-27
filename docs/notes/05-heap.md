# 二叉堆与堆排序

> 源码：`core/heap/MyHeap.java`（泛型 + Comparator）；被 Dijkstra/Prim 复用

## 原理图解

```
数组表示的完全二叉树（小顶堆）
        1
      ┌─┴─┐
      3   2          数组: [1,3,2,8,5,9]
    ┌─┴─┐ └─┐        parent(i) = (i-1)/2
    8   5   9        left = 2i+1, right = 2i+2

push(0): 末尾插入，与父比较上浮       pop(): 弹根，末尾补位下沉
        1                                  1 ← 弹出
      ┌─┴─┐                              ┌─┴─┐
      3   2        ──push 0──▶           3   2    末尾 0 补根后下沉
      ...                                ...
```

## 核心代码

```java
public E push(E element) {
    data[size] = element; int i = size++;
    while (i > 0) {                          // 上浮
        int parent = (i - 1) / 2;
        if (compare(i, parent) < 0) { swap(i, parent); i = parent; }
        else break;
    }
    return element;
}
public E pop() {
    E top = (E) data[0];
    data[0] = data[--size]; data[size] = null;
    siftDown(0);                             // 下沉
    return top;
}
private void siftDown(int i) {
    while (true) {
        int l = 2*i+1, r = 2*i+2, smallest = i;
        if (l < size && compare(l, smallest) < 0) smallest = l;
        if (r < size && compare(r, smallest) < 0) smallest = r;
        if (smallest == i) break;
        swap(i, smallest); i = smallest;
    }
}
```

## 复杂度推导

- 树高 = ⌊log₂n⌋；上浮/下沉最多走树高 → push/pop **O(log n)**，peek O(1)。
- **自底向上建堆 O(n)**：第 h 层（自底）有 n/2^(h+1) 个节点，各下沉 ≤h 步，总量 Σ n·h/2^(h+1) = n·Σ h/2^(h+1) < n。
- 堆排序 = 建堆 O(n) + n 次 pop 各 O(log n) = **O(n log n)**，且最坏也是 O(n log n)，空间 O(1)（原地）。
- 与 BST 对比：堆只保证“父≤子”，不支持按序遍历；BST 支持范围查询。堆的优势是取最值 O(1)。

## 易错点

1. pop 后必须**末尾补根再下沉**，直接删 data[0] 会留下空洞。
2. 下沉要与**较小（大顶堆为较大）的孩子**交换，盲目与左孩子交换不保证堆序。
3. 惰性删除场景（Dijkstra 的过期条目）：出堆时检查条目是否已过期（`settled[u]`）而不是真正删除。
4. 建堆从 `n/2 - 1` 开始倒序，`n/2` 到 n-1 是叶子无需下沉。
5. 泛型比较：无 Comparator 时要求 E 实现 Comparable，否则 ClassCastException。

## 练习题（附答案）

1. **数组第 k 大**
   答：维护 k 大小小顶堆，扫一遍，比堆顶大则替换；O(n log k)。
2. **合并 k 个有序链表**
   答：k 个头节点入堆，每次弹最小、补其 next；O(N log k)。
3. **为什么建堆是 O(n) 而不是 n 次 push 的 O(n log n)？**
   答：push 是逐个插入（深度递增），建堆自底向上按层下沉，多数节点（叶子）下沉 0 步，级数求和收敛于 O(n)。
4. **手推 [3,1,6,5,2] 建小顶堆的过程**
   答：从 i=1 下沉：swap(3,1)→[1,3,6,5,2]；i=0：与 2 比较 swap→[2,3,6,5,1]；再与 1 比较 swap→[1,3,6,5,2]... 最终 [1,3,6,5,2] 不满足？——正确结果 [1,3,6,5,2] 中 parent(1)=3≥1 ✓, parent(2)=6≥1 ✓, parent(3)=5≥3 ✓, parent(4)=6≥2 ✓，即 [1,3,6,5,2] 已是堆。
5. **堆排序为什么不稳定？**
   答：相等元素交换到堆顶的次序与其原始次序无关，下沉交换会打乱相等元素的相对位置。
