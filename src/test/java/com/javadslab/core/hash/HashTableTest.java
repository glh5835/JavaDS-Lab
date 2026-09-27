package com.javadslab.core.hash;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** 两种手写哈希表：正常 / 边界 / 异常 / 大规模随机对照（与 java.util.HashMap 对照）。 */
class HashTableTest {

    /* ---------------- ChainingHashMap ---------------- */

    @Test
    void chainingPutGetNormal() {
        ChainingHashMap<String, Integer> m = new ChainingHashMap<>();
        assertNull(m.put("a", 1));
        assertNull(m.put("b", 2));
        assertEquals(1, m.put("a", 10)); // 覆盖返回旧值
        assertEquals(10, m.get("a"));
        assertEquals(2, m.get("b"));
        assertEquals(2, m.size());
        assertTrue(m.containsKey("b"));
        assertFalse(m.containsKey("zz"));
    }

    @Test
    void chainingCollisionAndRemove() {
        // 用同一个 hashCode 的键强制全部冲突
        ChainingHashMap<CollideKey, Integer> m = new ChainingHashMap<>();
        for (int i = 0; i < 20; i++) m.put(new CollideKey(i), i);
        assertEquals(20, m.size());
        assertEquals(7, m.get(new CollideKey(7)));
        assertEquals(7, m.remove(new CollideKey(7)));
        assertEquals(19, m.size());
        assertNull(m.get(new CollideKey(7)));
    }

    @Test
    void chainingRemoveMissingThrows() {
        ChainingHashMap<String, Integer> m = new ChainingHashMap<>();
        m.put("a", 1);
        assertThrows(NoSuchElementException.class, () -> m.remove("b"));
    }

    @Test
    void chainingGrowthBeyondCapacity() {
        ChainingHashMap<Integer, Integer> m = new ChainingHashMap<>(2);
        for (int i = 0; i < 5_000; i++) m.put(i, i * 2);
        assertEquals(5_000, m.size());
        for (int i = 0; i < 5_000; i++) assertEquals(i * 2, m.get(i));
        for (int i = 0; i < 5_000; i += 2) m.remove(i);
        assertEquals(2_500, m.size());
        for (int i = 0; i < 5_000; i++) {
            if (i % 2 == 0) assertFalse(m.containsKey(i));
            else assertEquals(i * 2, m.get(i));
        }
    }

    @Test
    void chainingNullKeyRejectedByContract() {
        // 教学实现约定：键不允许为 null（hashCode 无法计算），显式抛 NPE
        ChainingHashMap<String, Integer> m = new ChainingHashMap<>();
        assertThrows(NullPointerException.class, () -> m.put(null, 1));
        m.put("x", null); // 值允许为 null
        assertNull(m.get("x"));
        assertTrue(m.containsKey("x"));
    }

    /* ---------------- OpenAddressingHashMap ---------------- */

    @Test
    void openPutGetNormal() {
        OpenAddressingHashMap<String, Integer> m = new OpenAddressingHashMap<>();
        assertNull(m.put("a", 1));
        assertNull(m.put("b", 2));
        assertEquals(1, m.put("a", 10));
        assertEquals(10, m.get("a"));
        assertEquals(2, m.size());
    }

    @Test
    void openTombstoneKeepsProbeChain() {
        // 构造连续冲突：a,b,c 同桶；删除中间的 b 后 c 仍可找到
        OpenAddressingHashMap<CollideKey, Integer> m = new OpenAddressingHashMap<>(8);
        for (int i = 0; i < 5; i++) m.put(new CollideKey(i), i);
        assertEquals(2, m.remove(new CollideKey(2))); // 中间墓碑
        assertEquals(4, m.get(new CollideKey(4)));    // 探测链未断
        assertEquals(3, m.remove(new CollideKey(3)));
        assertEquals(4, m.get(new CollideKey(4)));
        assertNull(m.put(new CollideKey(2), 99)); // 原键已删，put 返回 null（写入复用墓碑位）
        assertEquals(99, m.get(new CollideKey(2)));
    }

    @Test
    void openRemoveMissingThrows() {
        OpenAddressingHashMap<Integer, Integer> m = new OpenAddressingHashMap<>();
        m.put(1, 1);
        assertThrows(NoSuchElementException.class, () -> m.remove(2));
    }

    @Test
    void openGrowthBeyondCapacity() {
        OpenAddressingHashMap<Integer, Integer> m = new OpenAddressingHashMap<>(2);
        for (int i = 0; i < 5_000; i++) m.put(i, i * 2);
        assertEquals(5_000, m.size());
        for (int i = 0; i < 5_000; i++) assertEquals(i * 2, m.get(i));
        for (int i = 0; i < 5_000; i += 3) m.remove(i);
        assertEquals(5_000 - (5_000 + 2) / 3, m.size());
        for (int i = 0; i < 5_000; i++) {
            if (i % 3 == 0) assertFalse(m.containsKey(i));
            else assertEquals(i * 2, m.get(i));
        }
    }

    /* ---------------- 大规模随机对照：两种实现 vs java.util.HashMap ---------------- */

    @Test
    void randomCrossCheckAgainstJdk() {
        Random rnd = new Random(20260927L);
        ChainingHashMap<Integer, Integer> mine1 = new ChainingHashMap<>();
        OpenAddressingHashMap<Integer, Integer> mine2 = new OpenAddressingHashMap<>();
        Map<Integer, Integer> ref = new HashMap<>();
        for (int iter = 0; iter < 30_000; iter++) {
            int op = rnd.nextInt(4);
            int key = rnd.nextInt(2_000);
            switch (op) {
                case 0 -> {
                    int v = rnd.nextInt(1_000_000);
                    Integer r1 = mine1.put(key, v), r2 = mine2.put(key, v);
                    assertEquals(ref.put(key, v), r1, "拉链法 put 返回值");
                    assertEquals(r1, r2, "开放寻址 put 返回值");
                }
                case 1 -> {
                    Integer r1 = mine1.get(key), r2 = mine2.get(key);
                    assertEquals(ref.get(key), r1, "拉链法 get");
                    assertEquals(r1, r2, "开放寻址 get");
                }
                case 2 -> {
                    Integer r1 = null, r2 = null;
                    NoSuchElementException e1 = null, e2 = null;
                    try { r1 = mine1.remove(key); } catch (NoSuchElementException ex) { e1 = ex; }
                    try { r2 = mine2.remove(key); } catch (NoSuchElementException ex) { e2 = ex; }
                    if (ref.containsKey(key)) {
                        assertEquals(ref.remove(key), r1);
                        assertEquals(r1, r2);
                    } else {
                        assertNotNull(e1, "拉链法应抛键不存在");
                        assertNotNull(e2, "开放寻址应抛键不存在");
                    }
                }
                default -> {
                    assertEquals(ref.size(), mine1.size());
                    assertEquals(ref.size(), mine2.size());
                    assertEquals(ref.containsKey(key), mine1.containsKey(key));
                    assertEquals(ref.containsKey(key), mine2.containsKey(key));
                }
            }
        }
    }

    /* ---------------- step-mode ---------------- */

    @Test
    void tracersProduceValidSnapshots() {
        var t1 = new com.javadslab.trace.Tracer("hash", "拉链法演示");
        ChainingHashMap<Integer, Integer> m1 = new ChainingHashMap<>(4);
        m1.attachTracer(t1);
        for (int i = 0; i < 8; i++) m1.put(i, i); // 触发 resize
        m1.remove(3);
        assertTrue(t1.stepCount() >= 9);

        var t2 = new com.javadslab.trace.Tracer("hash", "开放寻址演示");
        OpenAddressingHashMap<Integer, Integer> m2 = new OpenAddressingHashMap<>(4);
        m2.attachTracer(t2);
        for (int i = 0; i < 8; i++) m2.put(i, i);
        m2.remove(2);
        assertTrue(t2.stepCount() >= 9);

        for (var t : java.util.List.of(t1, t2)) {
            Object parsed = com.javadslab.trace.Json.parse(com.javadslab.trace.Json.write(t.toTrace()));
            var steps = com.javadslab.trace.Json.arr(com.javadslab.trace.Json.obj(parsed).get("steps"));
            for (Object so : steps) {
                var st = com.javadslab.trace.Json.obj(so);
                assertNotNull(st.get("before"));
                assertNotNull(st.get("after"));
            }
        }
    }

    /** 所有实例 hashCode 相同，用于制造极端冲突。 */
    record CollideKey(int id) {
        @Override
        public int hashCode() { return 42; }

        @Override
        public boolean equals(Object o) {
            return o instanceof CollideKey k && k.id == id;
        }
    }
}
