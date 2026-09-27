# 栈 / 队列 / 循环队列 / 单调栈

> 源码：`core/stackqueue/ArrayStack.java`、`ArrayQueue.java`、`CircularQueue.java`、`MonotonicStack.java`

## 原理图解

```
栈（LIFO，top 指向下一可写位）          循环队列（取模回绕）
┌──┬──┬──┬──┬──┐                      ┌──┬──┬──┬──┐
│ a│ b│ c│  │  │  top=3              │ H→1 │ 2 │ 3 │ T→∅ │  head=0 tail=3
└──┴──┴──┴──┴──┘                      └──┴──┴──┴──┘
  push: data[top++]=e                  offer: data[tail]=(tail+1)%cap
  pop : e=data[--top]                  poll : e=data[head]; head=(head+1)%cap

单调栈（下一个更大元素）——栈存下标，对应值严格递减
a = [2, 1, 5, 6, 2, 3]
i=2 (5): 弹 1、弹 2 → res[0]=2, res[1]=2, 5 入栈
栈底 ──▶ 栈顶 值递减；遇到更大值即弹出并结算答案
```

## 核心代码

```java
// 循环队列：不扩容，满则抛异常（用计数器区分空/满）
public boolean offer(E element) {
    if (isFull()) throw new IllegalStateException("循环队列已满");
    data[tail] = element;
    tail = (tail + 1) % data.length;   // 回绕
    size++;
    return true;
}

// 单调栈核心：均摊 O(n)
for (int i = 0; i < n; i++) {
    while (top > 0 && a[stack[top - 1]] < a[i]) res[stack[--top]] = i;
    stack[top++] = i;
}
```

## 复杂度推导

- 栈 push/pop：一次赋值 → **O(1)**（扩容均摊 O(1)）。
- 循环队列 offer/poll：取模 + 赋值 → **O(1)**。用 `size` 计数器判空/满，避免了牺牲一格的经典技巧的歧义。
- 单调栈：每个下标**至多入栈一次、出栈一次**，总操作 ≤ 2n → **均摊 O(n)**，这是“均摊分析”的经典案例。

## 易错点

1. 循环队列判满：`size == cap`（计数器法）。若用 `head==tail` 判空/满会混淆，需牺牲一格或加标志位。
2. 取模回绕时 `tail` 可能越过 head，写入前必须检查 `isFull()`，否则**静默覆盖队头数据**。
3. 单调栈“严格大于”与“大于等于”决定结果：相等的元素是否算“更大”必须先想清楚（本实现是严格大于）。
4. pop 空栈要显式抛 EmptyStackException，不要返回 null 掩盖调用方错误。

## 练习题（附答案）

1. **用两个栈实现队列**
   答：in 栈收数据；出队时若 out 空则把 in 全部倒入 out 再弹。每个元素最多进出各一次，均摊 O(1)。
2. **用两个队列实现栈**
   答：入队进非空队列；出栈把前 n-1 个元素倒到另一队列再弹最后一个。均摊 O(n)。
3. **每日温度**（单调栈变形）
   答：与“下一个更大元素”同构：res[j]=i-j，栈存未结算下标，值递减。
4. **循环队列容量 4，依次 offer 1,2,3,4, poll 两次, offer 5,6 后 head/tail 是多少？**
   答：offer 4 次后 tail=0（回绕）；poll 两次 head=2；offer 5,6 写入下标 0,1，tail=2。此时 size=4 已满。
5. **柱状图中最大矩形**（单调栈进阶）
   答：对每根柱子找左右两侧第一个更矮的位置（两侧各跑一遍单调栈），宽度×高度取最大；栈存下标、高度递增。
