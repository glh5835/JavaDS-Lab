package com.javadslab.core.linear;

import com.javadslab.trace.Json;
import com.javadslab.trace.Tracer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** MyArrayList：正常 / 边界 / 异常 / 大规模随机对照（与 java.util.ArrayList 对照）。 */
class MyArrayListTest {

    /* ---------------- 正常 ---------------- */

    @Test
    void addAndGetAndSet() {
        MyArrayList<String> l = new MyArrayList<>();
        for (int i = 0; i < 100; i++) l.add("v" + i);
        assertEquals(100, l.size());
        assertEquals("v0", l.get(0));
        assertEquals("v99", l.get(99));
        assertEquals("v50", l.set(50, "x"));
        assertEquals("x", l.get(50));
        assertTrue(l.contains("v1"));
        assertFalse(l.contains("nope"));
        assertEquals(1, l.indexOf("v1"));
        assertEquals(-1, l.indexOf("nope"));
    }

    @Test
    void insertAndRemoveMiddle() {
        MyArrayList<Integer> l = new MyArrayList<>();
        for (int i = 0; i < 5; i++) l.add(i);          // [0,1,2,3,4]
        l.add(2, 99);                                   // [0,1,99,2,3,4]
        assertEquals(List.of(0, 1, 99, 2, 3, 4), toList(l));
        assertEquals(2, l.remove(3));                   // [0,1,99,3,4]
        assertEquals(List.of(0, 1, 99, 3, 4), toList(l));
        assertTrue(l.remove((Object) 99));
        assertEquals(List.of(0, 1, 3, 4), toList(l));
    }

    @Test
    void growthAcrossDefaultCapacity() {
        MyArrayList<Integer> l = new MyArrayList<>();
        for (int i = 0; i < 10_000; i++) l.add(i);
        assertEquals(10_000, l.size());
        for (int i = 0; i < 10_000; i++) assertEquals(i, l.get(i));
    }

    @Test
    void iterableAndToString() {
        MyArrayList<Integer> l = new MyArrayList<>();
        l.add(1); l.add(2); l.add(3);
        assertEquals("[1, 2, 3]", l.toString());
        assertEquals(List.of(1, 2, 3), toList(l));
    }

    /* ---------------- 边界 ---------------- */

    @Test
    void emptyListBoundary() {
        MyArrayList<Integer> l = new MyArrayList<>();
        assertEquals(0, l.size());
        assertTrue(l.isEmpty());
        assertFalse(l.contains(1));
        assertFalse(l.remove((Object) 1));
        l.clear(); // 空表 clear 不应出错
        assertEquals(0, l.size());
    }

    @Test
    void singleElement() {
        MyArrayList<Integer> l = new MyArrayList<>();
        l.add(7);
        assertEquals(1, l.size());
        l.add(0, 6);                          // 头插到唯一元素前
        assertEquals(List.of(6, 7), toList(l));
        assertEquals(7, l.remove(1));
        assertEquals(6, l.remove(0));
        assertTrue(l.isEmpty());
        l.add(8); // clear/删空后可复用
        assertEquals(8, l.get(0));
    }

    @Test
    void explicitCapacityZero() {
        MyArrayList<Integer> l = new MyArrayList<>(0);
        l.add(1); // 立即触发扩容也不应出错
        assertEquals(1, l.get(0));
    }

    /* ---------------- 异常 ---------------- */

    @Test
    void indexOutOfBounds() {
        MyArrayList<Integer> l = new MyArrayList<>();
        l.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> l.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> l.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> l.set(1, 9));
        assertThrows(IndexOutOfBoundsException.class, () -> l.remove(1));
        assertThrows(IndexOutOfBoundsException.class, () -> l.remove(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> l.add(-1, 9));
        assertThrows(IndexOutOfBoundsException.class, () -> l.add(2, 9));
    }

    @Test
    void negativeCapacityThrows() {
        assertThrows(IllegalArgumentException.class, () -> new MyArrayList<Integer>(-1));
    }

    @Test
    void iteratorNoSuchElement() {
        MyArrayList<Integer> l = new MyArrayList<>();
        l.add(1);
        Iterator<Integer> it = l.iterator();
        it.next();
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void nullElementsSupported() {
        MyArrayList<String> l = new MyArrayList<>();
        l.add(null);
        l.add("a");
        assertTrue(l.contains(null));
        assertEquals(0, l.indexOf(null));
        assertEquals("a", l.remove(1));
        assertNull(l.get(0));
    }

    /* ---------------- 大规模随机对照 ---------------- */

    @Test
    void randomCrossCheckAgainstJdk() {
        Random rnd = new Random(20260927L);
        MyArrayList<Integer> mine = new MyArrayList<>();
        ArrayList<Integer> ref = new ArrayList<>();
        for (int iter = 0; iter < 20_000; iter++) {
            int op = rnd.nextInt(6);
            switch (op) {
                case 0 -> { int v = rnd.nextInt(1000); mine.add(v); ref.add(v); }
                case 1 -> { if (!ref.isEmpty()) { int i = rnd.nextInt(ref.size()); assertEquals(ref.get(i), mine.get(i)); } }
                case 2 -> { int i = rnd.nextInt(ref.size() + 1); int v = rnd.nextInt(1000); mine.add(i, v); ref.add(i, v); }
                case 3 -> { if (!ref.isEmpty()) { int i = rnd.nextInt(ref.size()); assertEquals(ref.remove(i), mine.remove(i)); } }
                case 4 -> { if (!ref.isEmpty()) { int i = rnd.nextInt(ref.size()); int v = rnd.nextInt(1000); assertEquals(ref.set(i, v), mine.set(i, v)); } }
                default -> { if (!ref.isEmpty() && rnd.nextInt(4) == 0) { int v = ref.get(rnd.nextInt(ref.size())); assertEquals(ref.indexOf(v), mine.indexOf(v)); } }
            }
            assertEquals(ref.size(), mine.size(), "iter " + iter + " 后 size 不一致");
        }
        assertEquals(ref, toList(mine));
    }

    /* ---------------- step-mode 追踪 ---------------- */

    @Test
    void tracerRecordsStepsWithValidSnapshots() {
        Tracer t = new Tracer("array", "MyArrayList 演示");
        MyArrayList<Integer> l = new MyArrayList<>();
        l.attachTracer(t);
        l.add(1);
        l.add(2);
        l.add(0, 0);
        l.set(1, 10);
        l.remove(2);
        assertEquals(5, t.stepCount());

        String json = Json.write(t.toTrace());
        Object parsed = Json.parse(json);
        var root = Json.obj(parsed);
        var meta = Json.obj(root.get("meta"));
        assertEquals("array", meta.get("kind"));
        var steps = Json.arr(root.get("steps"));
        assertEquals(5, steps.size());
        for (Object so : steps) {
            var s = Json.obj(so);
            assertNotNull(s.get("op"));
            assertNotNull(s.get("before"));
            assertNotNull(s.get("after"));
            // 每个快照都是合法的 array 形状
            var before = Json.obj(s.get("before"));
            assertEquals("array", before.get("kind"));
        }
    }

    @Test
    void noTracerMeansNoOverheadField() {
        MyArrayList<Integer> l = new MyArrayList<>();
        l.add(1);
        assertNull(l.tracer); // tracer 字段存在但默认 null
        assertEquals(1, l.size());
    }

    private static <T> List<T> toList(Iterable<T> it) {
        List<T> out = new ArrayList<>();
        for (T v : it) out.add(v);
        return out;
    }
}
