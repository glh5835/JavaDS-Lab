# 红黑树

> 源码：`core/tree/RedBlackTree.java`（CLRS 实现，null 视为黑）

## 原理图解

```
五条性质：①节点红/黑 ②根黑 ③叶(null)黑 ④红节点孩子必黑 ⑤任一节点到叶黑高相等
⇒ 最长路径 ≤ 2×最短路径 ⇒ 高度 ≤ 2·log₂(n+1)

插入修复（新节点标红）：
  叔红 → 父叔变黑、爷变红，z 上溯两层
  叔黑 + LL → 父黑、爷红、右旋爷
  叔黑 + LR → 先左旋父转成 LL
  （右半边镜像）

删除修复（双黑）：
  ①兄红 → 旋转降级为兄黑
  ②兄全黑 → 兄变红，双黑上移一层
  ③近侄红远侄黑 → 转成④
  ④远侄红 → 旋转 + 变色终结
```

## 核心代码

```java
private void insertFixup(Node z) {
    while (isRed(z.parent)) {
        Node parent = z.parent, grand = parent.parent;
        if (parent == grand.left) {
            Node uncle = grand.right;
            if (isRed(uncle)) {                        // 情形1：叔红
                parent.color = BLACK; uncle.color = BLACK;
                grand.color = RED; z = grand;
            } else {
                if (z == parent.right) { z = parent; rotateLeft(z); parent = z.parent; } // LR
                parent.color = BLACK; grand.color = RED; rotateRight(grand);             // LL
            }
        } else { /* 镜像 */ }
    }
    root.color = BLACK;
}
```

删除用 transplant + 后继顶替；黑高被删则进入 deleteFixup 的双黑四情形（源码含全四情形与镜像）。

## 复杂度推导

- 性质⑤ ⇒ 从根到任一叶黑高相同，记 bh；性质④ ⇒ 路径上黑节点 ≥ 路径长一半 ⇒ n ≥ 2^bh - 1 ⇒ bh ≤ log₂(n+1)。
- 路径长 ≤ 2bh ≤ 2log₂(n+1) → 增删查 **O(log n)**。
- 插入至多 2 次旋转；删除至多 3 次旋转（情形 1+3+4 链）——比 AVL 少，写密集场景更优。

## 易错点

1. rotate 必须**维护 parent 指针**（CLRS 版本），漏掉 parent 会树结构错乱。
2. 删除后继时 `y.parent == z` 的分支要给 `x.parent` 赋值，否则 fixup 里访问 `xParent` 出错。
3. `transplant` 对 v==null 也要正确处理（不设 parent），fixup 需要显式传 xParent。
4. 新插节点必须标红；若标黑立即破坏性质⑤。
5. fixup 终止后 `root.color = BLACK`（插入上溯可能把根染红）。

## 练习题（附答案）

1. **依次插入 10,20,30,15,25,5 后画出树**
   答：30 叔红变色上溯 → 根 20 黑；15 触发 LL 单旋……最终根 20，左子树 10(黑)/5(红)/15(红)，右 30(黑)/25(红)。
2. **为什么红黑树查询比 AVL 慢但写更快？**
   答：高上界 2log n vs 1.44log n，查询路径更长；但修复旋转次数更少。
3. **黑高 bh 与节点数的关系**
   答：n ≥ 2^bh - 1（全部黑的满树最少），故 bh ≤ log₂(n+1)。
4. **Java 的 TreeMap/HashMap 树化阈值为什么是 8？**
   答：泊松分布下链长 8 概率 ≈ 5e-8，树化兜底极端哈希退化（树节点用红黑树）。
5. **双黑情形②为什么能上溯？**
   答：兄变红后该子树整体黑高减 1，父节点一侧“欠 1”，相当于父节点成为新的双黑，直至根或遇红节点染色终止。
