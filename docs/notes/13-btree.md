# B 树

> 源码：`core/tree/BTree.java`（最小度 t，t=2 即 2-3-4 树）

## 原理图解

```
t=2（键数 1..3）插入 10,20,30,40,50,60,70 的分裂过程
[10 20 30] 插 40 → 根满预分裂：        [20] 插 50 → 叶满分裂：
        [20]                                [20 40]
       /    \                              /  |   \
   [10]    [30]                         [10] [30] [50]

规则：节点键数 [t-1, 2t-1]；叶全同层；
     插入自顶向下“预分裂”（一次性下行，不需回溯）
删除：叶直接删 / 内部用前驱后继顶替 / 子节点 <t 键时向兄弟借或与父键合并
```

## 核心代码

```java
public void insert(int key) {
    if (contains(key)) return;
    Node r = root;
    if (r.keys.size() == 2 * t - 1) {       // 根满 → 升根
        Node newRoot = new Node();
        newRoot.leaf = false;
        newRoot.children.add(r);
        splitChild(newRoot, 0);
        root = newRoot;
    }
    insertNonFull(root, key);
}
private void splitChild(Node parent, int i) {   // 中间键上移
    Node full = parent.children.get(i), right = new Node();
    right.leaf = full.leaf;
    Integer upKey = full.keys.get(t - 1);
    right.keys.addAll(full.keys.subList(t, full.keys.size()));
    full.keys.subList(t - 1, full.keys.size()).clear();
    if (!full.leaf) {
        right.children.addAll(full.children.subList(t, full.children.size()));
        full.children.subList(t, full.children.size()).clear();
    }
    parent.keys.add(i, upKey);
    parent.children.add(i + 1, right);
}
```

## 复杂度推导

- 节点键数 ≥ t-1 ⇒ 分支因子 ≥ t ⇒ 高度 h ≤ log_t((n+1)/2) → 增删查 **O(t·log_t n)**（节点内线性比较）。
- 磁盘视角：一个节点 = 一次页读取，高 h 意味着 h 次磁盘 IO——B 树为**外存**设计，矮胖是核心。
- 插入预分裂保证路径上永不满，单次下行 O(h) 次节点写。
- 与红黑树关系：t=2 的 B 树（2-3-4 树）与红黑树同构。

## 易错点

1. 分裂时 `subList` 的边界：中间键**上移父节点**，右节点拿 `mid+1..` 半区；清除左半区时 subList 要**含**被上移键。
2. 根分裂要新建根再 splitChild(newRoot, 0)，直接对旧根操作会丢结构。
3. 删除下潜前必须保证子节点 ≥ t 键（借位或合并），否则可能删到只有 t-2 键的节点破坏下限。
4. 合并方向统一（有前驱兄弟往 idx-1 合，否则往 idx 合），递归目标节点要跟着变。
5. 全删空后根回落为空叶（`root = root.children.get(0)` 的降根在删除入口处理）。

## 练习题（附答案）

1. **t=2 时最多几个键？最少（根除外）？**
   答：最多 2t-1=3，最少 t-1=1。
2. **n=1,000,000、t=64 的 B 树高度上界？**
   答：h ≤ log₆₄((n+1)/2) ≈ log₆₄(5×10⁵) ≈ 2.94 → 最多 3~4 层，即 3~4 次磁盘页访问。
3. **为什么插入用“预分裂”而不是“满了再回溯分裂”？**
   答：预分裂保证当前节点必有空间容纳分裂上移的键，单次下行完成插入，无需回溯。
4. **B+ 树与 B 树的两点差别**
   答：B+ 树数据全在叶且叶子成链（利于范围扫描），内部节点只存索引键；B 树数据可存在任意节点。
5. **t=2 下插入 1..15，最终根是哪个键？**
   答：按 2-3-4 树分裂规律，最终根为 [8]（第 4 次升根后保持平衡，可手推验证）。
