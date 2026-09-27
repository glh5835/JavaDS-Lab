# KMP 字符串匹配

> 源码：`core/search/KMP.java`

## 原理图解

```
pattern = "ababd" 的 next（最长相等真前后缀）
 index:  0  1  2  3  4
 char :  a  b  a  b  d
 next :  0  0  1  2  0     ← next[2]=1：前缀 "a" = 后缀 "a"

匹配 text="ababcababd"：
i=4 处 text[4]='c' ≠ pattern[4]='d' 且 j=4
   → 不回退 i！j = next[3] = 2（已匹配的 "abab" 有前缀 "ab" 可复用）
   → pattern 滑到从 j=2 继续比，i 永不回退 ⇒ O(n)
```

## 核心代码

```java
public static int[] buildNext(String pattern) {
    int[] next = new int[pattern.length()];
    int len = 0;                                 // 当前最长相等真前后缀
    for (int i = 1; i < pattern.length(); i++) {
        while (len > 0 && pattern.charAt(i) != pattern.charAt(len))
            len = next[len - 1];                 // 失配回退：找次长的相等前后缀
        if (pattern.charAt(i) == pattern.charAt(len)) len++;
        next[i] = len;
    }
    return next;
}
public static List<Integer> searchAll(String text, String pattern) {
    int[] next = buildNext(pattern);
    int j = 0;
    List<Integer> hits = new ArrayList<>();
    for (int i = 0; i < text.length(); i++) {
        while (j > 0 && text.charAt(i) != pattern.charAt(j)) j = next[j - 1];
        if (text.charAt(i) == pattern.charAt(j)) j++;
        if (j == pattern.length()) {
            hits.add(i - j + 1);
            j = next[j - 1];                     // 允许重叠匹配
        }
    }
    return hits;
}
```

## 复杂度推导

- buildNext：`len` 每轮至多 +1，回退总共 ≤ +1 的总量 ⇒ 均摊 **O(m)**。
- searchAll：i 单调递增 n 次；j 的回退总量受 j 的增量约束（每次至多减到 next 链）⇒ 均摊 **O(n)**；总计 **O(n+m)**。
- 对比暴力 O(n·m)；对比 BM/ Sunday：实践中更快但实现复杂。
- 允许重叠 vs 不重叠：命中后 `j = next[j-1]`（重叠）vs `j = 0`（不重叠）。

## 易错点

1. `next` 的含义是“0..i 的最长相等**真**前后缀”，next[0] 恒为 0，循环从 i=1 开始。
2. 失配回退用 `while` 不是 `if`：可能连续回退多层。
3. 回退是 `next[len-1]`（对 len-1 查表），不是 `next[len]`。
4. 命中后继续匹配（重叠）时也要 `j = next[j-1]`，置 0 会漏跨界的重叠命中。
5. next 有两种流派（本实现是“前缀函数”，另有一种整体 -1 移位的 nextval），混看资料易懵，认准一种。

## 练习题（附答案）

1. **找 "aabaaabaa" 中 "aabaa" 的所有出现（含重叠）**
   答：出现于 0 和 5（命中后 j=next[4]=2 继续匹配）。
2. **手推 "abaabab" 的 next**
   答：{0,0,1,1,2,3,2}（末位回退：'b'≠pattern[3]='a' → len=next[2]=1 → 'b'=pattern[1] → 2）。
3. **最短回文拼接**（LeetCode 214）
   答：s + '#' + reverse(s) 跑 KMP，末位 next 值即 s 的最长回文前缀长度。
4. **循环同构字符串最小表示**
   答：文本 t=s+s 查所有起点，用 KMP 思想双指针比较。
5. **为什么 KMP 的 i 不用回退？**
   答：next 数组把“模式串已匹配部分”的最长相等前后缀编码了，失配时直接跳到可复用前缀的末尾继续，主串信息零浪费。
