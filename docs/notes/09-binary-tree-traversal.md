# 二叉树遍历（先序 / 中序 / 后序 / 层序）

> 源码：`core/tree/BinaryTrees.java`（递归 + 迭代双版本）

## 原理图解

```
        1                     先序（根左右）: 1 2 4 5 3 6
      ┌─┴─┐                   中序（左根右）: 4 2 5 1 3 6
      2   3                   后序（左右根）: 4 5 2 6 3 1
    ┌─┴─┐   └─┐               层序（BFS）  : 1 2 3 4 5 6
    4   5     6

迭代中序：一路向左压栈 → 弹出访问 → 转向右子
迭代后序：根右左变形后逆序（或双栈）
```

## 核心代码

```java
// 迭代中序：栈代替递归的“回溯”
public static List<Integer> inOrderIter(TreeNode root) {
    List<Integer> out = new ArrayList<>();
    Deque<TreeNode> stack = new ArrayDeque<>();
    TreeNode cur = root;
    while (cur != null || !stack.isEmpty()) {
        while (cur != null) { stack.push(cur); cur = cur.left; } // 一路向左
        cur = stack.pop();
        out.add(cur.val);                                        // 访问
        cur = cur.right;                                         // 转右
    }
    return out;
}

// 迭代后序：根右左 → 逆序 = 左右根
stack.push(root);
while (!stack.isEmpty()) {
    TreeNode n = stack.pop();
    out.add(n.val);
    if (n.left != null) stack.push(n.left);
    if (n.right != null) stack.push(n.right);
}
Collections.reverse(out);
```

## 复杂度推导

- 任何遍历访问每节点恰一次，递归/迭代均 **O(n)** 时间。
- 递归空间 = 栈深 = 树高：平衡 O(log n)，退化链 O(n)。
- 迭代显式栈同理；层序队列宽度最坏 n/2（最后一层）。
- 中序遍历 BST 得升序——这是 BST 有序性的直接体现（SearchTreesTest 用它做随机对照）。

## 易错点

1. `build()`（LeetCode 层序数组）的 null **必须消耗子槽位**，跳过 null 会让后续节点错挂到别的父节点。
2. 迭代先序压栈顺序：先右后左，否则顺序颠倒。
3. 迭代后序不能直接在中序框架里改访问位置——用“根右左 + 逆序”或“前驱回指”法。
4. 递归深度 10 万层的退化树会 StackOverflow，大数据用迭代。
5. 层序每层分开处理时，循环内先取 `int size = queue.size()` 再内层 for。

## 练习题（附答案）

1. **根据先序+中序重建二叉树**
   答：先序首元素为根，在中序中定位分割左右；哈希表存中序值→下标加速。O(n)。
2. **锯齿形层序**
   答：层序 + 奇数层 reverse，或双端队列交替方向。
3. **验证二叉搜索树**
   答：中序遍历严格递增；或递归传 (min,max) 界。
4. **非递归求树高**
   答：层序记录层数；或后序迭代栈中节点深度取 max。
5. **中序后继**
   答：有右子树→右子树最左；否则自底向上找第一个“自己是左孩子”的祖先。
