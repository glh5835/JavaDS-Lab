package com.javadslab.core.linear;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 手写动态数组线性表。
 * 核心逻辑：倍增扩容（1.5 倍）、按下标插入/删除的元素搬移。
 * 开启 step-mode（attachTracer）后，add/insert/set/remove/clear 均产生 trace 步骤。
 */
public class MyArrayList<E> implements Iterable<E> {

    private static final int DEFAULT_CAPACITY = 8;

    private Object[] data;
    private int size;
    Tracer tracer; // 包内可见，便于测试断言

    public MyArrayList() { this(DEFAULT_CAPACITY); }

    public MyArrayList(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("初始容量不能为负: " + initialCapacity);
        this.data = new Object[Math.max(1, initialCapacity)];
    }

    /* ---------------- step-mode 接口 ---------------- */

    public void attachTracer(Tracer t) { this.tracer = t; }

    /** 结构可视化快照：数组单元 + 容量信息。 */
    @SuppressWarnings("unchecked")
    public Object snapshot() {
        java.util.List<Object> cells = new java.util.ArrayList<>(size);
        for (int i = 0; i < size; i++) cells.add(Snaps.cell(data[i], null));
        return Snaps.array("ArrayList(size=" + size + ", cap=" + data.length + ")", cells);
    }

    private Tracer.Step begin(String op) {
        if (tracer == null) return Tracer.inert();
        return tracer.step(op).before(snapshot());
    }

    private void commit(Tracer.Step s, String hlKey, Object hlVal) {
        if (tracer == null) return;
        if (hlKey != null) s.hl(hlKey, hlVal);
        s.after(snapshot()).commit();
    }

    /* ---------------- 基本查询 ---------------- */

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    private void checkIndex(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("下标越界: index=" + index + ", size=" + size);
    }

    @SuppressWarnings("unchecked")
    public E get(int index) {
        checkIndex(index);
        return (E) data[index];
    }

    @SuppressWarnings("unchecked")
    public E set(int index, E element) {
        checkIndex(index);
        Tracer.Step s = begin("set").arg("index", index).arg("value", element);
        E old = (E) data[index];
        data[index] = element;
        commit(s, "index", index);
        return old;
    }

    public boolean contains(Object o) { return indexOf(o) >= 0; }

    public int indexOf(Object o) {
        for (int i = 0; i < size; i++) {
            if (o == null ? data[i] == null : o.equals(data[i])) return i;
        }
        return -1;
    }

    /* ---------------- 增删 ---------------- */

    public boolean add(E element) {
        Tracer.Step s = begin("add").arg("value", element);
        ensureCapacity(size + 1);
        data[size] = element;
        size++;
        commit(s, "index", size - 1);
        return true;
    }

    /** 在指定下标插入，原 [index, size) 整体后移一位。 */
    public void add(int index, E element) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("插入下标越界: index=" + index + ", size=" + size);
        Tracer.Step s = begin("insert").arg("index", index).arg("value", element);
        ensureCapacity(size + 1);
        System.arraycopy(data, index, data, index + 1, size - index);
        data[index] = element;
        size++;
        commit(s, "index", index);
    }

    @SuppressWarnings("unchecked")
    public E remove(int index) {
        checkIndex(index);
        Tracer.Step s = begin("remove").arg("index", index);
        E old = (E) data[index];
        System.arraycopy(data, index + 1, data, index, size - index - 1);
        data[--size] = null; // 防内存泄漏
        commit(s, "index", index);
        return old;
    }

    public boolean remove(Object o) {
        int i = indexOf(o);
        if (i < 0) return false;
        remove(i);
        return true;
    }

    public void clear() {
        Tracer.Step s = begin("clear");
        Arrays.fill(data, 0, size, null);
        size = 0;
        commit(s, null, null);
    }

    /** 扩容：按 1.5 倍增长，保证至少容纳 minCapacity。 */
    private void ensureCapacity(int minCapacity) {
        if (minCapacity <= data.length) return;
        int newCap = data.length + (data.length >> 1);
        if (newCap < minCapacity) newCap = minCapacity;
        data = Arrays.copyOf(data, newCap);
    }

    /* ---------------- 遍历 ---------------- */

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {
        int cursor;

        @Override
        public boolean hasNext() { return cursor < size; }

        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            if (cursor >= size) throw new NoSuchElementException();
            return (E) data[cursor++];
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(", ");
            sb.append(data[i]);
        }
        return sb.append(']').toString();
    }
}
