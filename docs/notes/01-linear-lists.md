# 线性表 / 单链表 / 双向链表

> 源码：`core/linear/MyArrayList.java`、`SinglyLinkedList.java`、`DoublyLinkedList.java`

## 原理图解

```
MyArrayList（连续内存 + size 指针）        SinglyLinkedList（节点 + next）
┌──┬──┬──┬──┬──┬────────┐               head ┌──────┐    ┌──────┐
│10│20│30│40│  │ 空闲   │                 ┌──▶│ val:1 │───▶│ val:2│───▶ ∅
└──┴──┴──┴──┴──┴────────┘                 │   └──────┘    └──────┘
 0  1  2  3  4 ← size=4                   └─ head 指针

DoublyLinkedList（哨兵 + prev/next）
head ⇄ [1] ⇄ [2] ⇄ [3] ⇄ tail   （哨兵使头尾插删永远 O(1)，无边界特判）
```

## 核心代码

```java
// MyArrayList：1.5 倍扩容 + 插入搬移
private void ensureCapacity(int minCapacity) {
    if (minCapacity <= data.length) return;
    int newCap = data.length + (data.length >> 1);   // 1.5 倍
    if (newCap < minCapacity) newCap = minCapacity;
    data = Arrays.copyOf(data, newCap);
}
public void add(int index, E element) {
    ensureCapacity(size + 1);
    System.arraycopy(data, index, data, index + 1, size - index); // 整体后移
    data[index] = element; size++;
}

// 双向链表：哨兵链接
private void linkBefore(Node<E> succ, Node<E> node) {
    node.prev = succ.prev; node.next = succ;
    succ.prev.next = node; succ.prev = node; size++;
}
```

## 复杂度推导

- `ArrayList.get(i)`：直接 `data[i]`，一次寻址 → **O(1)**。
- `ArrayList.add(i, e)`：期望搬移 `(n-i)/2` 个元素 → **O(n)**；尾部追加均摊 O(1)：
  扩容总搬移量 n/1.5 + n/1.5² + … ≈ 2n，摊到 n 次 add 每次 O(1)。
- `SinglyLinkedList.addFirst`：改 2 个指针 → **O(1)**；`addLast` 需遍历到尾 → **O(n)**。
- `DoublyLinkedList` 头尾均 **O(1)**；`get(i)` 智能方向：min(i, n-i) 步 → **O(n/2)=O(n)** 但常数减半。

## 易错点

1. 扩容用 `>> 1` 时别忘了 `newCap < minCapacity` 的兜底（初始容量 0/1 会算出 0）。
2. 删除后 `data[--size] = null` 防止对象游离（内存泄漏）。
3. 单链表按下标插删要先定位到 **前驱**，`index-1`，边界 `index==0` 要特判。
4. 双向链表用哨兵后，所有删除都变成“ unlink 中间节点”，但哨兵本身不可删。
5. 遍历中删除元素必须用迭代器的 `remove`，否则抛 ConcurrentModificationException（本实现未做 fail-fast，教学中应说明）。

## 练习题（附答案）

1. **逆置单链表**（迭代，O(1) 空间）
   答：三指针 `prev/cur/next`，逐个把 next 指针反转：`next=cur.next; cur.next=prev; prev=cur; cur=next;` 返回 prev。
2. **找中间节点**（快慢指针）
   答：slow 每步 1、fast 每步 2，fast 到尾时 slow 即中间（偶数长度取第二个中点）。
3. **删除链表倒数第 k 个节点**
   答：快指针先走 k 步，随后快慢同走，快到尾时慢指针的 next 即目标；哨兵可统一处理删头。
4. **ArrayList 初始容量为 0 时连续 add 1 个元素，内部数组长度变为多少？**
   答：`newCap = 0 + 0>>1 = 0 < minCapacity=1`，兜底取 1 → 长度 1；后续按 max(1.5倍, 需求) 增长。
5. **判断链表是否有环**
   答：Floyd 判圈——快慢指针相遇则有环；相遇后把一指针放回头部，同速前进再相遇处即入环口。
