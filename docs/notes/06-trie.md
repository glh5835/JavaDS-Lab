# Trie 前缀树

> 源码：`core/tree/Trie.java`（集合语义：重复插入为无操作）

## 原理图解

```
插入 {cat, car, card, dog}          passCount = 经过该节点的单词数
        root(p=4)
       /c(3)      \d(1)
      a(3)         o(1)
     /  \            \
    t(1) r(2)          g(1)[end]
   [end]   \
            d(1)[end]
删除 "car"：passCount 递减到 0 的子树被整体剪掉，"card" 不受影响
```

## 核心代码

```java
public void insert(String word) {
    Node exist = walk(word);
    if (exist != null && exist.isEnd) return;  // 集合语义：已存在则无操作
    Node cur = root; cur.passCount++;
    for (char ch : word.toCharArray()) {
        int c = ch - 'a';
        if (cur.children[c] == null) cur.children[c] = new Node();
        cur = cur.children[c];
        cur.passCount++;
    }
    cur.isEnd = true; size++;
}
public boolean delete(String word) {
    if (!search(word)) return false;
    Node cur = root; cur.passCount--;
    for (char ch : word.toCharArray()) {
        Node child = cur.children[ch - 'a'];
        child.passCount--;
        if (child.passCount == 0) { cur.children[ch - 'a'] = null; size--; return true; } // 剪枝
        cur = child;
    }
    cur.isEnd = false; size--;
    return true;
}
```

## 复杂度推导

- insert/search/startsWith 只依赖单词长度 L：**O(L)**，与词典规模无关（哈希表做不到前缀查询）。
- 空间：最坏 O(Σ L·26)（每个字符一个 26 槽数组节点）；passCount 额外 O(1)/节点。
- countWithPrefix 直接读 passCount：**O(L)**。
- 删除：passCount 递减到 0 时整棵子树一次剪掉，单次删除 O(L)。

## 易错点

1. 集合语义 vs 计数语义：重复插入若计数，删除时 `isEnd=false` 会让仍存在的副本“消失”——本实现选择集合语义（重复插入 no-op）。
2. 剪枝条件是 `passCount==0` 而非 `isEnd==false`：后者会剪掉仍有单词经过的共享前缀。
3. 字符映射 `ch - 'a'` 必须校验范围，混入大写/数字会数组越界。
4. search 与 startsWith 的区别只在末节点的 `isEnd`。
5. 26 叉数组实现空间开销大；工程上用 `Map<Character,Node>` 压缩，教学上数组更快更直观。

## 练习题（附答案）

1. **实现 MagicDictionary**（通配一次替换）
   答：建 Trie 后 DFS，允许一次 mismatch：`search(word)` 时逐字符走，若 `children[c]` 缺失可消耗一次“修改”机会走任意存在分支……标准解法对每个候选词差一比较（n·L²）也可。
2. **单词搜索 II**（棋盘 + 词典）
   答：把词典建 Trie，DFS 棋盘同时走 Trie，命中 isEnd 收集并置 false 防重复。
3. **stream 中持续判断是否出现某个词的完整结尾**
   答：Trie + 多指针回退（AC 自动机的简化）：每次新字符从失配节点沿 fail 指针回退。
4. **Trie 比 HashMap 强在哪里、弱在哪里？**
   答：强在前缀批量查询/字典序遍历/公共前缀共享空间；弱在单键点查常数更大、空间系数 26 大、hash 均摊 O(1) 更快。
5. **若单词只含 a-z，如何把 26 槽数组的空间降到接近实际分支数？**
   答：children 用 `Map<Character,Node>`（分支少时）或左孩子右兄弟链表；或位图+紧凑数组。
