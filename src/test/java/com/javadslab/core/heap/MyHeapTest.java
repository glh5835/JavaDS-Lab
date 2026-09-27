package com.javadslab.core.heap;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** MyHeap：正常 / 边界 / 异常 / 大规模随机对照（与 java.util.PriorityQueue 对照）。 */
class MyHeapTest {

    @Test
    void minHeapBasic() {
        MyHeap<Integer> h = new MyHeap<>();
        h.push(5); h.push(1); h.push(3);
        assertEquals(1, h.peek());
        assertEquals(1, h.pop());
        assertEquals(3, h.peek());
        assertEquals(3, h.pop());
        assertEquals(5, h.pop());
        assertTrue(h.isEmpty());
    }

    @Test
    void maxHeapWithComparator() {
        MyHeap<Integer> h = new MyHeap<>(Collections.reverseOrder());
        h.push(5); h.push(1); h.push(3);
        assertEquals(5, h.pop());
        assertEquals(3, h.pop());
        assertEquals(1, h.pop());
    }

    @Test
    void stringHeapNaturalOrder() {
        MyHeap<String> h = new MyHeap<>();
        h.push("pear"); h.push("apple"); h.push("banana");
        assertEquals("apple", h.pop());
        assertEquals("banana", h.pop());
        assertEquals("pear", h.pop());
    }

    @Test
    void heapifyConstructor() {
        Integer[] arr = {9, 4, 7, 1, 8, 2};
        MyHeap<Integer> h = new MyHeap<>(arr);
        List<Integer> sorted = new java.util.ArrayList<>();
        while (!h.isEmpty()) sorted.add(h.pop());
        assertEquals(List.of(1, 2, 4, 7, 8, 9), sorted);
    }

    @Test
    void boundaryAndExceptions() {
        MyHeap<Integer> h = new MyHeap<>(null, 0 + 1);
        assertThrows(NoSuchElementException.class, h::pop);
        assertThrows(NoSuchElementException.class, h::peek);
        assertThrows(IllegalArgumentException.class, () -> new MyHeap<Integer>(null, 0));
        h.push(1);
        h.pop();
        h.push(2); // 删空后可复用
        assertEquals(2, h.peek());
    }

    @Test
    void heapsortViaHeap() {
        Random rnd = new Random(3L);
        for (int trial = 0; trial < 50; trial++) {
            MyHeap<Integer> h = new MyHeap<>();
            int n = rnd.nextInt(500);
            Integer[] expected = new Integer[n];
            for (int i = 0; i < n; i++) {
                expected[i] = rnd.nextInt(10_000);
                h.push(expected[i]);
            }
            Arrays.sort(expected);
            Integer[] got = new Integer[n];
            for (int i = 0; i < n; i++) got[i] = h.pop();
            assertArrayEquals(expected, got);
        }
    }

    @Test
    void randomCrossCheckAgainstJdkPriorityQueue() {
        Random rnd = new Random(20260927L);
        MyHeap<Integer> mine = new MyHeap<>();
        PriorityQueue<Integer> ref = new PriorityQueue<>();
        for (int iter = 0; iter < 50_000; iter++) {
            int op = rnd.nextInt(3);
            if (op == 0) {
                int v = rnd.nextInt(100_000);
                mine.push(v);
                ref.add(v);
            } else if (op == 1 && !ref.isEmpty()) {
                assertEquals(ref.poll(), mine.pop());
            } else if (!ref.isEmpty()) {
                assertEquals(ref.peek(), mine.peek());
            }
            assertEquals(ref.size(), mine.size());
        }
    }
}
