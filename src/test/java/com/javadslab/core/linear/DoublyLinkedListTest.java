package com.javadslab.core.linear;

import com.javadslab.trace.Json;
import com.javadslab.trace.Tracer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** DoublyLinkedList：正常 / 边界 / 异常 / 大规模随机对照。 */
class DoublyLinkedListTest {

    /* ---------------- 正常 ---------------- */

    @Test
    void twoWayOperations() {
        DoublyLinkedList<Integer> l = new DoublyLinkedList<>();
        l.addLast(1); l.addLast(2); l.addLast(3);
        assertEquals(1, l.getFirst());
        assertEquals(3, l.getLast());
        l.addFirst(0);
        assertEquals(List.of(0, 1, 2, 3), toList(l));
        assertEquals(3, l.removeLast());
        assertEquals(0, l.removeFirst());
        assertEquals(List.of(1, 2), toList(l));
    }

    @Test
    void middleInsertAndRemove() {
        DoublyLinkedList<Integer> l = new DoublyLinkedList<>();
        for (int i = 0; i < 6; i++) l.addLast(i);
        l.add(3, 99); // [0,1,2,99,3,4,5]
        assertEquals(List.of(0, 1, 2, 99, 3, 4, 5), toList(l));
        assertEquals(99, l.removeAt(3));
        assertEquals(5, l.removeAt(5));
        assertEquals(List.of(0, 1, 2, 3, 4), toList(l));
    }

    @Test
    void middleAccessWalksShorterSide() {
        DoublyLinkedList<Integer> l = new DoublyLinkedList<>();
        for (int i = 0; i < 100; i++) l.addLast(i);
        assertEquals(80, l.get(80));  // 从尾部走
        assertEquals(20, l.get(20));  // 从头部走
        assertEquals(50, l.get(50));
    }

    /* ---------------- 边界 ---------------- */

    @Test
    void emptyAndSingleBoundary() {
        DoublyLinkedList<Integer> l = new DoublyLinkedList<>();
        assertThrows(NoSuchElementException.class, l::removeFirst);
        assertThrows(NoSuchElementException.class, l::removeLast);
        assertThrows(NoSuchElementException.class, l::getFirst);
        l.addLast(1);
        l.removeLast();
        assertTrue(l.isEmpty());
        l.addFirst(2); // 删空后仍能正常工作
        assertEquals(2, l.getFirst());
        assertEquals(2, l.getLast());
    }

    /* ---------------- 异常 ---------------- */

    @Test
    void indexOutOfBounds() {
        DoublyLinkedList<Integer> l = new DoublyLinkedList<>();
        l.addLast(1);
        assertThrows(IndexOutOfBoundsException.class, () -> l.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> l.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> l.set(1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> l.removeAt(1));
        assertThrows(IndexOutOfBoundsException.class, () -> l.add(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> l.add(2, 0));
    }

    /* ---------------- 大规模随机对照 ---------------- */

    @Test
    void randomCrossCheckAgainstJdk() {
        Random rnd = new Random(20260927L);
        DoublyLinkedList<Integer> mine = new DoublyLinkedList<>();
        java.util.LinkedList<Integer> ref = new java.util.LinkedList<>();
        for (int iter = 0; iter < 20_000; iter++) {
            int op = rnd.nextInt(7);
            switch (op) {
                case 0 -> { int v = rnd.nextInt(1000); mine.addFirst(v); ref.addFirst(v); }
                case 1 -> { int v = rnd.nextInt(1000); mine.addLast(v); ref.addLast(v); }
                case 2 -> { if (!ref.isEmpty()) { int i = rnd.nextInt(ref.size()); assertEquals(ref.get(i), mine.get(i)); } }
                case 3 -> { if (!ref.isEmpty()) { int i = rnd.nextInt(ref.size()); assertEquals(ref.remove(i), mine.removeAt(i)); } }
                case 4 -> { int i = rnd.nextInt(ref.size() + 1); int v = rnd.nextInt(1000); mine.add(i, v); ref.add(i, v); }
                case 5 -> { if (!ref.isEmpty() && rnd.nextBoolean()) assertEquals(ref.removeFirst(), mine.removeFirst()); }
                default -> { if (!ref.isEmpty()) assertEquals(ref.removeLast(), mine.removeLast()); }
            }
            assertEquals(ref.size(), mine.size(), "iter " + iter + " 后 size 不一致");
        }
        assertEquals(ref, toList(mine));
    }

    /* ---------------- step-mode ---------------- */

    @Test
    void tracerRecordsSteps() {
        Tracer t = new Tracer("linked", "双向链表演示");
        DoublyLinkedList<Integer> l = new DoublyLinkedList<>();
        l.attachTracer(t);
        l.addLast(1);
        l.addFirst(0);
        l.removeLast();
        assertEquals(3, t.stepCount());
        var root = Json.obj(Json.parse(Json.write(t.toTrace())));
        assertEquals(3, Json.arr(root.get("steps")).size());
        // 每步 before/after 都能再次序列化（内容合法）
        String again = Json.write(Json.parse(Json.write(t.toTrace())));
        assertTrue(again.contains("\"op\":\"removeLast\""));
    }

    private static <T> List<T> toList(Iterable<T> it) {
        List<T> out = new ArrayList<>();
        for (T v : it) out.add(v);
        return out;
    }
}
