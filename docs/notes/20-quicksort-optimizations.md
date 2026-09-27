# 快速排序分区优化

> 源码：`core/sort/QuickSortOptimized.java`（对比基础版 `SortingAlgorithms.quickSort`）

## 原理图解

```
①三数取中：lo/mid/hi 三个数取中位数当基准，防有序输入退化
   [1,2,3,4,5] 固定取 hi=5 → 每次切 1/(n-1) → O(n²)
   取 median(1,3,5)=3 → 大致对半 → O(n log n)

②三路分区（荷兰国旗）：大量重复元素时一次性归位
   [lo..lt-1] < pivot | [lt..gt] == pivot | [gt+1..hi] > pivot
   i 扫描：a[i]<p 交换到 lt++；a[i]>p 交换到 gt--；a[i]==p i++
   全相同数组：一趟 O(n) 结束（基础版 O(n²) 或 O(n log n)）

③小区间插排：长度 ≤ 16 时插入排序，递归开销 < 收益
④尾递归消除：先递归较小一侧，较大一侧循环，栈深 O(log n)
```

## 核心代码

```java
private static void quickRec(int[] a, int lo, int hi) {
    while (lo < hi) {
        if (hi - lo + 1 <= INSERTION_THRESHOLD) {
            SortingAlgorithms.insertionSort(a, lo, hi);
            return;
        }
        int pivot = medianOfThree(a, lo, lo + (hi - lo) / 2, hi);
        int lt = lo, gt = hi, i = lo;
        while (i <= gt) {
            if (a[i] < pivot) swap(a, i++, lt++);
            else if (a[i] > pivot) swap(a, i, gt--);
            else i++;
        }
        // 先递归小侧，大侧循环（尾递归消除）
        if (lt - lo < hi - gt) { quickRec(a, lo, lt - 1); lo = gt + 1; }
        else                   { quickRec(a, gt + 1, hi); hi = lt - 1; }
    }
}
```

## 复杂度推导

- 三路分区：设不同键 k 个、第 i 个键出现 cᵢ 次，T=O(n log k)；全相同 k=1 → **O(n)**（测试 `quickSortOptimizedHandlesDuplicatesFast`：10 万个相同元素毫秒级）。
- 期望比较次数（随机基准）：C(n) = n-1 + (2/n)Σ C(i) ≈ 1.39 n log₂n。
- 尾递归消除后栈深 ≤ log₂n（每次递归的是较小半区，大小 ≤ n/2）。
- 三数取中不能防“对抗性输入”，但能防最常见的有序/逆序退化；工业级用 introsort（超深转堆排）。

## 易错点

1. 三路分区循环条件是 `i <= gt`，gt 在右区收缩；写成 `i < gt` 会漏判最后元素。
2. `a[i] > pivot` 分支里 i **不前进**（换过来的 gt 位元素还没看过）。
3. medianOfThree 返回的是“值”不是下标，三路分区按值分区即可（重复值自动归中段）。
4. 先递归小侧的方向判断 `lt - lo < hi - gt` 左右区间边界写错会越界。
5. 递归版本仍有爆栈风险（恶意构造），工程上加深度限制转堆排。

## 练习题（附答案）

1. **为什么全相同数组基础快排会退化？**
   答：Lomuto 分区所有元素都不小于基准 → 每次只切掉 1 个 → O(n²)（且递归深 n）。
2. **手推三路分区 [3,1,3,2,3] 基准=3**
   答：lt=0,gt=4,i=0：a[0]=3→i=1；a[1]=1→swap(0,1),lt=1,i=2；a[2]=3→i=3；a[3]=2→swap(0,3)?? 注意 lt=1：swap(1,3)→[2,1,3,3,3]，lt=2,i=4；a[4]=3→i=5>gt 停。结果 [2,1,3,3,3]，中段全 3。
3. ** introsort 的思路**
   答：快排为主；递归深度超 2log n 转堆排（防退化）；小区间插排。C++ std::sort 的实现。
4. **双路分区（Hoare/两端扫描）与三路的适用差异**
   答：双路适合少量重复；三路在重复多时把等值段直接排除出递归，重复越多越快。
5. **快排的空间复杂度为什么是 O(log n)？**
   答：递归栈深度（每次先处理小半区），最坏 O(log n)；不是分区本身需要的。
