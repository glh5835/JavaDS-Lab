# AVL 树

> 源码：`core/tree/AVLTree.java`

## 原理图解

```
四种失衡与旋转（bf = h(L) - h(R)）

LL：bf>1 且左子 bf>=0        RR：bf<-1 且右子 bf<=0
     30                 20         1                2
    /  ──右旋──▶       /  \          ──左旋──▶     /  \
   20                 10  30        2             1    3
  /                              ┌─┘
10                              3（先右旋右子变 RR）

LR：先对左子左旋转为 LL，再对根右旋     RL：先对右子右旋转为 RR，再左旋
```

## 核心代码

```java
private Node rebalance(Node n) {
    update(n);                       // 先更新高度（自底向上递归保证孩子已新）
    int bf = balanceFactor(n);
    if (bf > 1) {
        if (balanceFactor(n.left) < 0) n.left = rotateLeft(n.left);  // LR
        return rotateRight(n);                                        // LL
    }
    if (bf < -1) {
        if (balanceFactor(n.right) > 0) n.right = rotateRight(n.right); // RL
        return rotateLeft(n);                                           // RR
    }
    return n;
}
private Node rotateRight(Node y) {
    Node x = y.left;
    y.left = x.right;   // x 的右子树过继给 y
    x.right = y;
    update(y); update(x); // 先矮后高
    return x;
}
```

## 复杂度推导

- 平衡条件 |bf|≤1 ⇒ 树高 ≤ 1.44·log₂(n+2)（Fibonacci 树最坏）→ 增删查 **O(log n)** 严格保证。
- 插入：一次插入至多触发**一次**旋转（LL/RR 单旋或 LR/RL 双旋），回溯路径 O(log n)。
- 删除：可能沿回溯路径多处失衡，每处 O(1) 旋转，总 O(log n)。
- 旋转本身只改 O(1) 指针；update(y) 必须先于 update(x)（y 变矮了）。

## 易错点

1. 旋转后**先 update 更矮的 y 再 update x**，顺序反了高度错。
2. LR/RL 先旋转孩子让其形态变 LL/RR，直接单旋会留失衡。
3. 删除双孩节点：后继键上移后删后继，递归回溯每一层都要 rebalance。
4. **size 双扣 bug**（本项目真实踩过）：入口 size-- 后递归删后继又 size--，一次删除扣 2。修正：size 只在预检通过后的入口减一次，递归只管结构。
5. `insert` 遇重复键要直接 return n，不得继续下探（否则把同一键插两处）。

## 练习题（附答案）

1. **逐个插入 1..7，画出最终 AVL**
   答：完全平衡的满二叉树，根 4，左右各 [2,1,3]、[6,5,7]。
2. **插入序列 3,1,2 触发什么旋转？**
   答：LR——根 3 bf=2，左子 1 bf=-1；先左旋 1→得 2 为左子，再右旋根。结果根 2。
3. **AVL 高度上界怎么推？**
   答：最坏情形左右子树高差 1，节点数 N(h)=N(h-1)+N(h-2)+1 → N(h) 与 Fibonacci 同阶，h ≤ 1.44 log₂(n+2)。
4. **删除后为什么可能多次旋转而插入只一次？**
   答：插入只可能让一条路径上第一次失衡点的子树高度“回到原值”，回溯到此即止；删除使子树变矮，上层 bf 需要逐层重算。
5. **AVL vs 红黑树**
   答：AVL 更严格平衡（查询更快），旋转更多（写更贵）；红黑树最高 2log n 但增删旋转少——读多写少用 AVL，写多用 RB。
