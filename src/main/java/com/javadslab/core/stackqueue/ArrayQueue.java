package com.javadslab.core.stackqueue;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * 手写队列：环形缓冲区实现，head/tail 指针循环前进，满时 2 倍扩容并重排。
 * step-mode：offer/poll/clear 产生 trace 步骤（快照展示逻辑顺序的元素）。
 */
public class ArrayQueue<E> {

    private Object[] data;
    private int head; // 队头下标
    private int size;
    private Tracer tracer;

    public ArrayQueue() { data = new Object[8]; }

    /* ---------------- step-mode ---------------- */

    public void attachTracer(Tracer t) { this.tracer = t; }

    /** 快照按「从队头到队尾」的逻辑顺序展示，隐藏环形物理布局细节。 */
    public Object snapshot() {
        java.util.List<Object> cells = new java.util.ArrayList<>(size);
        for (int i = 0; i < size; i++) cells.add(Snaps.cell(data[(head + i) % data.length], null));
        return Snaps.array("ArrayQueue(size=" + size + ", cap=" + data.length + ", head=" + head + ")", cells);
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

    /** 入队：环形写入 tail=(head+size)%cap。 */
    public boolean offer(E element) {
        Tracer.Step s = begin("offer").arg("value", element);
        if (size == data.length) grow();
        data[(head + size) % data.length] = element;
        size++;
        commit(s);
        return true;
    }

    /** 出队：空队抛 NoSuchElementException。 */
    @SuppressWarnings("unchecked")
    public E poll() {
        if (size == 0) throw new NoSuchElementException("队列为空");
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
        if (size == 0) throw new NoSuchElementException("队列为空");
        return (E) data[head];
    }

    /** 扩容为 2 倍并把元素按逻辑顺序搬到新数组开头（head 归零）。 */
    private void grow() {
        Object[] nd = new Object[data.length * 2];
        for (int i = 0; i < size; i++) nd[i] = data[(head + i) % data.length];
        data = nd;
        head = 0;
    }

    public void clear() {
        Tracer.Step s = begin("clear");
        Arrays.fill(data, null);
        head = 0;
        size = 0;
        commit(s);
    }
}
