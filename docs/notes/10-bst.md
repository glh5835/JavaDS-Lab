# 二叉搜索树 BST

> 源码：`core/tree/BST.java`

## 原理图解

```
插入 50,30,70,20,40,60,80：           删除 30（双孩）：
            50                          50
          ┌──┴──┐                     ┌──┴──┐
         30     70        ──▶       40     70      ← 中序后继 40 顶替
        ┌┴─┐   ┌─┴┐                 ┌┘     ┌─┴┐
       20  40 60  80               20     60  80

三种删除情形：叶直接删 / 单孩顶替 / 双孩=后继键上移再删后继
```

## 核心代码

```java
public boolean remove(int key) {
    Node parent = null, cur = root;
    while (cur != null && cur.key != key) {          // 迭代定位（避免递归栈）
        parent = cur;
        cur = key < cur.key ? cur.left : cur.right;
    }
    if (cur == null) return false;
    if (cur.left != null && cur.right != null) {     // 双孩：找后继
        Node succParent = cur, succ = cur.right;
        while (succ.left != null) { succParent = succ; succ = succ.left; }
        cur.key = succ.key;                          // 键上移
        cur = succ; parent = succParent;             // 转化为删后继（至多单孩）
    }
    Node child = cur.left != null ? cur.left : cur.right;
    if (parent == null) root = child;
    else if (parent.left == cur) parent.left = child;
    else parent.right = child;
    return true;
}
```

## 复杂度推导

- 所有操作走一条根到叶路径：**O(h)**。
- 随机插入序期望 h ≈ 2 ln n → O(log n)；**有序插入退化成链 h=n → O(n)**（测试 `bstSequentialDegenerate` 验证）。
- 中序遍历 = 升序：n 个键 O(n)。
- n 个键共 n! 种插入序，平均树高 √(log n) 级（精确常数 ~4.31 ln n 内），但最坏不可控 → 引出 AVL/RB。

## 易错点

1. 双孩删除必须**复制后继的键**（不是值对象引用混乱），然后删后继节点本身。
2. 迭代删除要同时维护 `parent` 指针，递归写法则让 `removeRec` 返回新子树根。
3. 重复键策略要定死：本实现“重复插入忽略（集合语义）”，也有存 count 的多重集实现。
4. 有序数据插入 BST 是最坏情况，工程上应换自平衡树（AVL/红黑树）。
5. `contains` 用循环不用递归，防深链爆栈。

## 练习题（附答案）

1. **第 k 小元素**
   答：中序遍历计数到 k；或节点维护子树大小做 O(h) 选择。
2. **范围和 [L,R]**
   答：递归剪枝：key<L 只走右，key>R 只走左，区间内累加。O(h + 出口数)。
3. **验证 BST（陷阱：只比较父子不够）**
   答：必须传整条祖先链的 (min,max) 界，或中序严格递增。反例：[5,1,6] 根 5，右子 4——只比父子会漏。
4. **BST 中序前驱**
   答：有左子树→左子树最右；否则从根往下，最后一次“向右拐”的节点。
5. **把有序数组转平衡 BST**
   答：取中点为根，左右递归。O(n)。
