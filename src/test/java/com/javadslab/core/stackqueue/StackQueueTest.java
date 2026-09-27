package com.javadslab.core.stackqueue;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EmptyStackException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** ArrayStack / ArrayQueue / CircularQueue / MonotonicStack 四件套测试。 */
class StackQueueTest {

    /* ================= ArrayStack ================= */

    @Test
    void stackLifoNormal() {
        ArrayStack<Integer> s = new ArrayStack<>();
        s.push(1); s.push(2); s.push(3);
        assertEquals(3, s.size());
        assertEquals(3, s.peek());
        assertEquals(3, s.pop());
        assertEquals(2, s.pop());
        assertEquals(1, s.pop());
        assertTrue(s.isEmpty());
    }

    @Test
    void stackBoundaryAndGrowth() {
        ArrayStack<Integer> s = new ArrayStack<>();
        assertThrows(EmptyStackException.class, s::pop);
        assertThrows(EmptyStackException.class, s::peek);
        for (int i = 0; i < 10_000; i++) s.push(i);
        for (int i = 9_999; i >= 0; i--) assertEquals(i, s.pop());
        s.push(7); s.clear();
        assertTrue(s.isEmpty());
    }

    @Test
    void stackRandomCrossCheck() {
        Random rnd = new Random(7L);
        ArrayStack<Integer> mine = new ArrayStack<>();
        Deque<Integer> ref = new ArrayDeque<>();
        for (int iter = 0; iter < 50_000; iter++) {
            if (ref.isEmpty() || rnd.nextBoolean()) {
                int v = rnd.nextInt(1000);
                mine.push(v);
                ref.push(v);
            } else {
                assertEquals(ref.pop(), mine.pop());
            }
            assertEquals(ref.size(), mine.size());
        }
    }

    /* ================= ArrayQueue ================= */

    @Test
    void queueFifoNormal() {
        ArrayQueue<String> q = new ArrayQueue<>();
        q.offer("a"); q.offer("b"); q.offer("c");
        assertEquals("a", q.peek());
        assertEquals("a", q.poll());
        assertEquals("b", q.poll());
        q.offer("d");
        assertEquals("c", q.poll());
        assertEquals("d", q.poll());
        assertTrue(q.isEmpty());
    }

    @Test
    void queueWrapAroundAndGrowth() {
        // 反复 offer/poll 使 head 指针多次回绕，验证取模逻辑
        ArrayQueue<Integer> q = new ArrayQueue<>();
        int expect = 0;
        for (int i = 0; i < 10_000; i++) {
            q.offer(i);
            if (i % 3 == 0) assertEquals(expect++, q.poll());
        }
        while (!q.isEmpty()) assertEquals(expect++, q.poll());
        assertEquals(10_000, expect);
        assertThrows(NoSuchElementException.class, q::poll);
        assertThrows(NoSuchElementException.class, q::peek);
    }

    @Test
    void queueRandomCrossCheck() {
        Random rnd = new Random(11L);
        ArrayQueue<Integer> mine = new ArrayQueue<>();
        Deque<Integer> ref = new ArrayDeque<>();
        for (int iter = 0; iter < 50_000; iter++) {
            int op = rnd.nextInt(3);
            if (op == 0) { int v = rnd.nextInt(1000); mine.offer(v); ref.addLast(v); }
            else if (!ref.isEmpty()) { assertEquals(ref.removeFirst(), mine.poll()); }
            assertEquals(ref.size(), mine.size());
        }
    }

    /* ================= CircularQueue ================= */

    @Test
    void circularQueueFixedCapacity() {
        CircularQueue<Integer> q = new CircularQueue<>(3);
        assertTrue(q.offer(1)); assertTrue(q.offer(2)); assertTrue(q.offer(3));
        assertTrue(q.isFull());
        assertThrows(IllegalStateException.class, () -> q.offer(4));
        assertEquals(1, q.poll());
        assertTrue(q.offer(4)); // 回绕写入
        assertEquals(2, q.poll());
        assertEquals(3, q.poll());
        assertEquals(4, q.poll());
        assertTrue(q.isEmpty());
        assertThrows(NoSuchElementException.class, q::poll);
    }

    @Test
    void circularQueueWrapManyTimes() {
        CircularQueue<Integer> q = new CircularQueue<>(4);
        for (int round = 0; round < 1000; round++) {
            for (int i = 0; i < 4; i++) q.offer(round * 10 + i);
            for (int i = 0; i < 4; i++) assertEquals(round * 10 + i, q.poll());
        }
    }

    @Test
    void circularQueueInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new CircularQueue<>(0));
        assertThrows(IllegalArgumentException.class, () -> new CircularQueue<>(-5));
    }

    /* ================= MonotonicStack ================= */

    @Test
    void nextGreaterNormal() {
        MonotonicStack ms = new MonotonicStack();
        assertArrayEquals(new int[]{1, 2, -1, -1},
                ms.nextGreaterIndex(new int[]{2, 7, 9, 1}));
        assertArrayEquals(new int[]{7, 9, -1, -1},
                ms.nextGreaterValue(new int[]{2, 7, 9, 1}));
        // 递减序列：全部无更大
        assertArrayEquals(new int[]{-1, -1, -1}, ms.nextGreaterIndex(new int[]{5, 4, 3}));
        // 相等元素：右侧相等不算更大（严格大于）
        assertArrayEquals(new int[]{-1, -1}, ms.nextGreaterIndex(new int[]{4, 4}));
    }

    @Test
    void nextGreaterBoundary() {
        MonotonicStack ms = new MonotonicStack();
        assertArrayEquals(new int[]{-1}, ms.nextGreaterIndex(new int[]{1}));
        assertArrayEquals(new int[0], ms.nextGreaterIndex(new int[0]));
    }

    @Test
    void nextGreaterBruteForceCrossCheck() {
        Random rnd = new Random(13L);
        MonotonicStack ms = new MonotonicStack();
        for (int trial = 0; trial < 300; trial++) {
            int n = rnd.nextInt(50);
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = rnd.nextInt(20);
            int[] got = ms.nextGreaterIndex(a);
            int[] want = new int[n];
            for (int i = 0; i < n; i++) {
                want[i] = MonotonicStack.NONE;
                for (int j = i + 1; j < n; j++) {
                    if (a[j] > a[i]) { want[i] = j; break; }
                }
            }
            assertArrayEquals(want, got, "数组 " + java.util.Arrays.toString(a));
        }
    }

    /* ================= step-mode 追踪 ================= */

    @Test
    void tracersProduceValidSnapshots() {
        // 栈
        var ts = new com.javadslab.trace.Tracer("array", "栈演示");
        ArrayStack<Integer> s = new ArrayStack<>();
        s.attachTracer(ts);
        s.push(1); s.push(2); s.pop();
        assertEquals(3, ts.stepCount());
        // 队列
        var tq = new com.javadslab.trace.Tracer("array", "队列演示");
        ArrayQueue<Integer> q = new ArrayQueue<>();
        q.attachTracer(tq);
        q.offer(1); q.poll(); q.offer(2);
        assertEquals(3, tq.stepCount());
        // 循环队列
        var tc = new com.javadslab.trace.Tracer("array", "循环队列演示");
        CircularQueue<Integer> cq = new CircularQueue<>(2);
        cq.attachTracer(tc);
        cq.offer(1); cq.poll(); cq.offer(2); cq.offer(3);
        assertEquals(4, tc.stepCount());
        // 单调栈
        var tm = new com.javadslab.trace.Tracer("monostack", "单调栈演示");
        MonotonicStack ms = new MonotonicStack();
        ms.attachTracer(tm);
        ms.nextGreaterIndex(new int[]{3, 1, 4});
        assertTrue(tm.stepCount() > 5);
        // 所有 trace 都必须是合法 JSON 且每步含 before/after
        for (var t : List.of(ts, tq, tc, tm)) {
            Object parsed = com.javadslab.trace.Json.parse(com.javadslab.trace.Json.write(t.toTrace()));
            for (Object so : com.javadslab.trace.Json.arr(com.javadslab.trace.Json.obj(parsed).get("steps"))) {
                var st = com.javadslab.trace.Json.obj(so);
                assertNotNull(st.get("before"));
                assertNotNull(st.get("after"));
            }
        }
    }
}
