package com.javadslab.core.stackqueue;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.NoSuchElementException;

/**
 * 手写固定容量循环队列：head/tail 指针 + 取模回绕，不扩容。
 * 满与空判据：size==cap 为满，size==0 为空（牺牲一个单元判满的经典写法之外，用计数器更直观）。
 * step-mode：offer/poll 产生 trace 步骤（快照含物理环形布局与头尾指针）。
 */
public class CircularQueue<E> {

    private final Object[] data;
    private int head;       // 队头下标
    private int tail;       // 下一个写入位置
    private int size;
    private Tracer tracer;

    public CircularQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("容量必须为正: " + capacity);
        this.data = new Object[capacity];
    }

    public int capacity() { return data.length; }

    /* ---------------- step-mode ---------------- */

    public void attachTracer(Tracer t) { this.tracer = t; }

    /** 快照：物理下标布局 + H/T 指针标记，教学展示「回绕」。 */
    public Object snapshot() {
        java.util.List<Object> cells = new java.util.ArrayList<>(data.length);
        for (int i = 0; i < data.length; i++) {
            String mark = null;
            if (i < size) mark = "occupied";
            if (i == head) mark = (mark == null ? "" : mark + ",") + "H";
            if (i == tail) mark = (mark == null ? "" : mark + ",") + "T";
            Object v = data[i];
            cells.add(Snaps.cell(v == null ? "·" : v, mark));
        }
        return Snaps.array("CircularQueue(size=" + size + "/" + data.length + ", head=" + head + ", tail=" + tail + ")", cells);
    }

    private Tracer.Step begin(String op) {
        if (tracer == null) return Tracer.inert();
        return tracer.step(op).before(snapshot());
    }

    private void commit(Tracer.Step s) {
        if (tracer == null) return;
        s.after(snapshot()).commit();
    }

    /* ---------------- 队列操作 ---------------- */

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    public boolean isFull() { return size == data.length; }

    /** 入队；满时抛 IllegalStateException（不静默丢数据）。 */
    public boolean offer(E element) {
        if (isFull()) throw new IllegalStateException("循环队列已满: capacity=" + data.length);
        Tracer.Step s = begin("offer").arg("value", element);
        data[tail] = element;
        tail = (tail + 1) % data.length;
        size++;
        commit(s);
        return true;
    }

    /** 出队；空队抛 NoSuchElementException。 */
    @SuppressWarnings("unchecked")
    public E poll() {
        if (isEmpty()) throw new NoSuchElementException("循环队列为空");
        Tracer.Step s = begin("poll");
        E val = (E) data[head];
        data[head] = null;
        head = (head + 1) % data.length;
        size--;
        commit(s);
        return val;
    }

    @SuppressWarnings("unchecked")
    public E peek() {
        if (isEmpty()) throw new NoSuchElementException("循环队列为空");
        return (E) data[head];
    }
}
