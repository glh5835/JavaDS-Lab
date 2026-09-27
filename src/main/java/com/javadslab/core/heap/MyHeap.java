package com.javadslab.core.heap;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 手写二叉堆（默认小顶堆，可传 Comparator 反转）。
 * 数组表示：i 的孩子 2i+1 / 2i+2，父亲 (i-1)/2。
 * 上浮 siftUp / 下沉 siftDown；push/pop 均 O(log n)。
 * step-mode：push/pop 的每次比较交换都记录 trace（kind=heap，播放器画成完全二叉树）。
 */
public class MyHeap<E> {

    private Object[] data;
    private int size;
    private final Comparator<? super E> cmp;
    private Tracer tracer;

    public MyHeap() { this(null, 16); }

    public MyHeap(Comparator<? super E> comparator) { this(comparator, 16); }

    @SuppressWarnings("unchecked")
    public MyHeap(Comparator<? super E> comparator, int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("容量必须为正: " + capacity);
        this.cmp = comparator;
        this.data = new Object[capacity];
    }

    /** 自底向上建堆 O(n)：输入数组直接原地堆化。 */
    @SuppressWarnings("unchecked")
    public MyHeap(E[] items) {
        this.cmp = null;
        this.data = items.clone();
        this.size = items.length;
        for (int i = size / 2 - 1; i >= 0; i--) siftDown(i);
    }

    /* ---------------- step-mode ---------------- */

    public void attachTracer(Tracer t) { this.tracer = t; }

    public Object snapshot() {
        int[] arr = new int[size];
        boolean allInts = true;
        for (int i = 0; i < size; i++) {
            Object v = data[i];
            if (v instanceof Integer ii) arr[i] = ii;
            else { allInts = false; break; }
        }
        Map<Integer, String> marks = new java.util.HashMap<>();
        if (allInts) return Snaps.heap(arr, size, marks);
        // 非整型：退化为字符串单元
        List<Object> cells = new ArrayList<>(size);
        for (int i = 0; i < size; i++) cells.add(Snaps.cell(data[i], null));
        return Snaps.obj("kind", "heap", "cells", cells, "size", size);
    }

    private Tracer.Step begin(String op) {
        if (tracer == null) return Tracer.inert();
        return tracer.step(op).before(snapshot());
    }

    private void commit(Tracer.Step s) {
        if (tracer == null) return;
        s.after(snapshot()).commit();
    }

    /* ---------------- 基本操作 ---------------- */

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    @SuppressWarnings("unchecked")
    private int compare(int i, int j) {
        Comparator<? super E> c = cmp;
        if (c != null) return c.compare((E) data[i], (E) data[j]);
        return ((Comparable<? super E>) data[i]).compareTo((E) data[j]);
    }

    public E push(E element) {
        Tracer.Step s = begin("push").arg("value", element);
        if (size == data.length) data = Arrays.copyOf(data, size * 2);
        data[size] = element;
        int i = size++;
        s.after(snapshot()).commit(); // 入堆先记一步
        // 上浮：比父亲小（小顶堆）则交换
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (compare(i, parent) < 0) {
                swap(i, parent);
                if (tracer != null) {
                    tracer.step("sift-up").arg("from", i).arg("to", parent)
                            .before(snapshot()).after(snapshot()).hl("swapped", List.of(i, parent)).commit();
                }
                i = parent;
            } else break;
        }
        return element;
    }

    /** 弹出堆顶；空堆抛 NoSuchElementException。 */
    @SuppressWarnings("unchecked")
    public E pop() {
        if (size == 0) throw new NoSuchElementException("堆为空");
        Tracer.Step s = begin("pop");
        E top = (E) data[0];
        data[0] = data[--size];
        data[size] = null;
        s.after(snapshot()).commit();
        siftDown(0);
        return top;
    }

    @SuppressWarnings("unchecked")
    public E peek() {
        if (size == 0) throw new NoSuchElementException("堆为空");
        return (E) data[0];
    }

    /** 下沉：与较小的孩子交换，直到堆序满足。 */
    private void siftDown(int i) {
        while (true) {
            int l = 2 * i + 1, r = 2 * i + 2, smallest = i;
            if (l < size && compare(l, smallest) < 0) smallest = l;
            if (r < size && compare(r, smallest) < 0) smallest = r;
            if (smallest == i) break;
            swap(i, smallest);
            if (tracer != null) {
                tracer.step("sift-down").arg("from", i).arg("to", smallest)
                        .before(snapshot()).after(snapshot()).hl("swapped", List.of(i, smallest)).commit();
            }
            i = smallest;
        }
    }

    private void swap(int i, int j) {
        Object t = data[i];
        data[i] = data[j];
        data[j] = t;
    }
}
