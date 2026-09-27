# 二分查找变体

> 源码：`core/search/BinarySearch.java`

## 原理图解

```
标准二分（闭区间 [lo,hi]）：
  [1,2,4,4,4,6,8]  key=4
   lo=0      mid=3(4✓)          命中返回 3
   但第一次出现是 index 2 → 需要 lowerBound 变体

lowerBound（第一个 >= key，半开 [lo,hi)）：
  不变量：[0,lo) 全部 < key；[hi,n) 全部 >= key
  lo==hi 时 lo 即答案（可能是 n = 不存在）

upperBound（第一个 > key）：同构，比较改为 <=
firstOccurrence = lowerBound 命中确认
lastOccurrence  = upperBound - 1 命中确认

旋转数组（无重复）：
  [4,5,6,7,0,1,2]  每次至少一半有序：
  a[lo]<=a[mid] → 左半有序；key 在 [a[lo],a[mid]) 内则去左，否则去右
```

## 核心代码

```java
public static int lowerBound(int[] a, int key) {
    int lo = 0, hi = a.length;                 // 不变量：[0,lo)<key<=[hi,n)
    while (lo < hi) {
        int mid = (lo + hi) >>> 1;
        if (a[mid] < key) lo = mid + 1;
        else hi = mid;                          // mid 可能就是答案，hi 不减 1
    }
    return lo;
}
public static int searchRotated(int[] a, int key) {
    int lo = 0, hi = a.length - 1;
    while (lo <= hi) {
        int mid = (lo + hi) >>> 1;
        if (a[mid] == key) return mid;
        if (a[lo] <= a[mid]) {                                   // 左半有序
            if (a[lo] <= key && key < a[mid]) hi = mid - 1;
            else lo = mid + 1;
        } else {                                                 // 右半有序
            if (a[mid] < key && key <= a[hi]) lo = mid + 1;
            else hi = mid - 1;
        }
    }
    return -1;
}
```

## 复杂度推导

- 每轮区间减半：T(n)=T(n/2)+O(1) → **O(log n)**。
- lowerBound 的循环不变量证明终止正确：lo 单调增、hi 单调减、lo<hi 收敛；答案必在不变量缝隙处。
- 二分求平方根：值域二分，`mid > x/mid` 判 mid²>x（除法防 long 溢出）→ O(log x)。
- “二分答案”：把单调性从数组搬到问题本身（最小可行容量、最大最小间距），复杂度 O(值域 × 验证代价)。

## 易错点

1. **溢出**：`(lo+hi)/2` 大数组会溢 int，用 `(lo+hi)>>>1` 或 `lo+(hi-lo)/2`。
2. lowerBound 的 `hi = mid`（不是 mid-1）——半开区间里 mid 可能是答案；写错会跳过答案。
3. 死循环三件套：区间没收缩、mid 永远等于 lo、`<=` 与 `=` 判断混用。
4. 旋转数组 `a[lo] <= a[mid]` 的 `<=` 不能少（区间只剩 1~2 个元素时 lo==mid）。
5. sqrtInt 用除法比较而非 `mid*mid`，long 也可能溢出（mid≈3e9 时平方≈9e18 恰好溢）。

## 练习题（附答案）

1. **[1,2,2,2,3] 中 key=2：lowerBound / upperBound / first / last？**
   答：lowerBound=1，upperBound=4，first=1，last=3。
2. **旋转数组找最小值 [4,5,6,7,0,1,2]**
   答：`a[mid] > a[hi]` → 最小在右半 lo=mid+1；否则 hi=mid。答案 0。
3. **有重复的旋转数组查找（LeetCode 81）**
   答：`a[lo]==a[mid]==a[hi]` 时无法判方向 → `lo++, hi--` 保守收缩，最坏 O(n)。
4. **二分求 x 的整数立方根**
   答：同 sqrtInt：`mid³ > x` 收 hi，注意用除法防溢出或限 hi=2×10⁶。
5. **山峰数组找峰顶（先升后降）**
   答：`a[mid] < a[mid+1]` 峰在右，lo=mid+1；否则 hi=mid。O(log n)。
