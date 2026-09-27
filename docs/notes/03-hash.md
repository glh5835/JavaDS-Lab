# 哈希表：拉链法与开放寻址法

> 源码：`core/hash/ChainingHashMap.java`、`OpenAddressingHashMap.java`

## 原理图解

```
拉链法（Separate Chaining）             开放寻址（线性探测）
bucket[0] → (k1,v) → (k7,v) → ∅        [k1][k7][∅][k3][墓碑][∅]...
bucket[1] → ∅                                 ↑
bucket[2] → (k3,v) → ∅                 删除留墓碑 TOMBSTONE，
bucket[3] → ∅                          探测链遇 null 才停，遇墓碑跳过

hash = (h ^ (h>>>16)) & (cap-1)   —— 高低位异或扰动 + 2 的幂掩码
```

## 核心代码

```java
// 拉链法：头插 + 负载因子 0.75 扩容
public V put(K key, V value) {
    if (size + 1 > buckets.length * LOAD_FACTOR) resize(); // 2 倍重拉链
    int idx = indexOf(key);
    for (Entry<K,V> e = buckets[idx]; e != null; e = e.next)
        if (Objects.equals(e.key, key)) { V old = e.value; e.value = value; return old; }
    buckets[idx] = new Entry<>(key, value, buckets[idx]);  // 头插
    size++; return null;
}

// 开放寻址：探测到 null 判不存在；删除置墓碑保持探测链完整
private int probeFor(K key) {
    int i = firstIndex(key);
    while (true) {
        Slot<K,V> s = table[i];
        if (s == null) return -1;                          // 链断 ⇒ 一定不存在
        if (!s.isTombstone() && Objects.equals(s.key, key)) return i;
        i = (i + 1) & (table.length - 1);
    }
}
```

## 复杂度推导

- 理想均匀散列下，拉链法每桶长度 α=load factor（常数），查找/插入 **O(1) 期望**；最坏全部冲突退化为 **O(n)** 链表。
- 扩容代价：n→2n 重哈希 O(n)，摊还到 n 次插入每次 O(1)。
- 开放寻址：探测次数期望 1/(1-α)，α=0.75 时 ≈4 次；本实现 α 上限 0.6，期望 ≈2.5 次。
- 墓碑过多会使探测链变长（used 含墓碑触发扩容），重建时墓碑被清除。

## 易错点

1. `cap-1` 掩码要求容量恒为 2 的幂，否则取模分布不均。
2. **删除开放寻址的元素不能直接置 null**——会截断探测链，后面的同桶元素永远找不到，必须放墓碑。
3. `Objects.equals` 比较 key，勿用 `==`（包装类型缓存陷阱：Integer -128~127 之外 `==` 失效）。
4. 教学实现约定 key 不允许 null（hashCode 无法计算）；value 允许 null，`containsKey` 与 `get` 语义因此不同。
5. 扩容后所有元素下标全部改变，必须逐节点重新散列，不能整体复制数组。

## 练习题（附答案）

1. **两数之和**
   答：一遍哈希——遍历时查 `target-nums[i]` 是否已在表中，在则返回，否则存入。O(n)/O(n)。
2. **字母异位词分组**
   答：key 用 26 位计数数组编码的字符串（或排序后字符串），value 存分组列表。
3. **为什么开放寻址的负载因子阈值（0.6）要低于拉链法（0.75）？**
   答：开放寻址探测次数随 α 急剧上升（1/(1-α)），α→1 时逼近无限；拉链只是链变长，仍是线性可接受。
4. **大量删除后开放寻址性能下降的原因与对策？**
   答：墓碑拉长探测链；对策：删除量达阈值后重建（去掉墓碑），或定期 rehash。
5. **设计一个支持 getRandom O(1) 的集合**
   答：数组存元素 + 哈希表存值→下标。删除时与尾元素交换后 pop，并更新被换元素的下标。
