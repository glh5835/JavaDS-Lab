package com.javadslab.core.linear;

import com.javadslab.trace.Json;
import com.javadslab.trace.Tracer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** SinglyLinkedList：正常 / 边界 / 异常 / 大规模随机对照（与 java.util.LinkedList 对照）。 */
class SinglyLinkedListTest {

    /* ---------------- 正常 ---------------- */

    @Test
    void addFirstAddLastAndTraverse() {
        SinglyLinkedList<Integer> l = new SinglyLinkedList<>();
        l.addLast(2);
        l.addLast(3);
        l.addFirst(1);
        assertEquals(List.of(1, 2, 3), toList(l));
        assertEquals(3, l.size());
        assertEquals(1, l.get(0));
        assertEquals(3, l.get(2));
    }

    @Test
    void insertAndRemoveAt() {
        SinglyLinkedList<Integer> l = new SinglyLinkedList<>();
        for (int i = 0; i < 5; i++) l.addLast(i);      // [0,1,2,3,4]
        l.add(2, 99);                                   // [0,1,99,2,3,4]
        assertEquals(List.of(0, 1, 99, 2, 3, 4), toList(l));
        assertEquals(99, l.removeAt(2));
        assertEquals(0, l.removeAt(0));
        assertEquals(4, l.removeAt(l.size() - 1));
        assertEquals(List.of(1, 2, 3), toList(l));
    }

    @Test
    void removeFirstAndValue() {
        SinglyLinkedList<String> l = new SinglyLinkedList<>();
        l.addLast("a"); l.addLast("b"); l.addLast("a");
        assertEquals("a", l.removeFirst());
        assertTrue(l.removeValue("a")); // 删除剩下的第一个 a（即原第三个）
        assertEquals(List.of("b"), toList(l));
        assertFalse(l.removeValue("zz"));
    }

    /* ---------------- 边界 ---------------- */

    @Test
    void emptyBoundary() {
        SinglyLinkedList<Integer> l = new SinglyLinkedList<>();
        assertTrue(l.isEmpty());
        assertFalse(l.contains(1));
        assertThrows(NoSuchElementException.class, l::removeFirst);
        l.clear();
        assertEquals(0, l.size());
    }

    @Test
    void singleElementBoundary() {
        SinglyLinkedList<Integer> l = new SinglyLinkedList<>();
        l.addLast(5);
        l.removeAt(0);
        assertTrue(l.isEmpty());
        l.addFirst(6);
        assertEquals(6, l.get(0));
        l.add(1, 7); // index==size 尾插
        assertEquals(List.of(6, 7), toList(l));
    }

    /* ---------------- 异常 ---------------- */

    @Test
    void indexOutOfBounds() {
        SinglyLinkedList<Integer> l = new SinglyLinkedList<>();
        l.addLast(1);
        assertThrows(IndexOutOfBoundsException.class, () -> l.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> l.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> l.set(1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> l.removeAt(1));
        assertThrows(IndexOutOfBoundsException.class, () -> l.add(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> l.add(2, 2));
        assertThrows(NoSuchElementException.class, () -> {
            var it = l.iterator();
            it.next(); it.next();
        });
    }

    /* ---------------- 大规模随机对照 ---------------- */

    @Test
    void randomCrossCheckAgainstJdk() {
        Random rnd = new Random(20260927L);
        SinglyLinkedList<Integer> mine = new SinglyLinkedList<>();
        java.util.LinkedList<Integer> ref = new java.util.LinkedList<>();
        for (int iter = 0; iter < 20_000; iter++) {
            int op = rnd.nextInt(6);
            switch (op) {
                case 0 -> { int v = rnd.nextInt(1000); mine.addFirst(v); ref.addFirst(v); }
                case 1 -> { int v = rnd.nextInt(1000); mine.addLast(v); ref.addLast(v); }
                case 2 -> { if (!ref.isEmpty()) { int i = rnd.nextInt(ref.size()); assertEquals(ref.get(i), mine.get(i)); } }
                case 3 -> { if (!ref.isEmpty()) { int i = rnd.nextInt(ref.size()); assertEquals(ref.remove(i), mine.removeAt(i)); } }
                case 4 -> { int i = rnd.nextInt(ref.size() + 1); int v = rnd.nextInt(1000); mine.add(i, v); ref.add(i, v); }
                default -> { if (!ref.isEmpty() && rnd.nextBoolean()) assertEquals(ref.removeFirst(), mine.removeFirst()); }
            }
            assertEquals(ref.size(), mine.size(), "iter " + iter + " 后 size 不一致");
        }
        assertEquals(ref, toList(mine));
    }

    /* ---------------- step-mode ---------------- */

    @Test
    void tracerRecordsLinkedSnapshots() {
        Tracer t = new Tracer("linked", "单链表演示");
        SinglyLinkedList<Integer> l = new SinglyLinkedList<>();
        l.attachTracer(t);
        l.addLast(1);
        l.addLast(2);
        l.removeFirst();
        assertEquals(3, t.stepCount());
        var root = Json.obj(Json.parse(Json.write(t.toTrace())));
        var steps = Json.arr(root.get("steps"));
        for (Object so : steps) {
            var before = Json.obj(Json.obj(so).get("before"));
            assertEquals("linked", before.get("kind"));
        }
        var last = Json.obj(steps.get(2));
        var after = Json.obj(last.get("after"));
        assertEquals(1, Json.arr(after.get("nodes")).size());
    }

    private static <T> List<T> toList(Iterable<T> it) {
        List<T> out = new ArrayList<>();
        for (T v : it) out.add(v);
        return out;
    }
}
