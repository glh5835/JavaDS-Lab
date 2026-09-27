package com.javadslab.core.linear;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 手写单向链表：仅持 head 指针，尾插需 O(n) 遍历（教学重点：与 ArrayList 的复杂度对照）。
 * step-mode：addFirst/addLast/insert/removeFirst/removeAt/removeValue 均记录步骤。
 */
public class SinglyLinkedList<E> implements Iterable<E> {

    /** 链表节点。 */
    public static final class Node<E> {
        public E val;
        public Node<E> next;

        Node(E val) { this.val = val; }
    }

    private Node<E> head;
    private int size;
    private Tracer tracer;

    /* ---------------- step-mode 接口 ---------------- */

    public void attachTracer(Tracer t) { this.tracer = t; }

    /** 快照：把节点串成数组交给播放器（箭头由播放器绘制）。 */
    public Object snapshot() {
        java.util.List<Object> nodes = new java.util.ArrayList<>(size);
        for (Node<E> p = head; p != null; p = p.next) nodes.add(Snaps.cell(p.val, null));
        return Snaps.linked(nodes);
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

    /** O(n) 定位 + 取值。 */
    public E get(int index) {
        checkIndex(index);
        Node<E> p = nodeAt(index);
        return p.val;
    }

    public E set(int index, E element) {
        checkIndex(index);
        Tracer.Step s = begin("set").arg("index", index).arg("value", element);
        Node<E> p = nodeAt(index);
        E old = p.val;
        p.val = element;
        commit(s, "index", index);
        return old;
    }

    private Node<E> nodeAt(int index) {
        Node<E> p = head;
        for (int i = 0; i < index; i++) p = p.next;
        return p;
    }

    public int indexOf(Object o) {
        int i = 0;
        for (Node<E> p = head; p != null; p = p.next, i++) {
            if (o == null ? p.val == null : o.equals(p.val)) return i;
        }
        return -1;
    }

    public boolean contains(Object o) { return indexOf(o) >= 0; }

    /* ---------------- 增删 ---------------- */

    /** 头插，O(1)。 */
    public void addFirst(E element) {
        Tracer.Step s = begin("addFirst").arg("value", element);
        Node<E> node = new Node<>(element);
        node.next = head;
        head = node;
        size++;
        commit(s, "index", 0);
    }

    /** 尾插，O(n) 遍历。 */
    public void addLast(E element) {
        Tracer.Step s = begin("addLast").arg("value", element);
        Node<E> node = new Node<>(element);
        if (head == null) {
            head = node;
        } else {
            Node<E> p = head;
            while (p.next != null) p = p.next;
            p.next = node;
        }
        size++;
        commit(s, "index", size - 1);
    }

    /** 按下标插入：插到 index 位置，原元素后移。index==size 表示尾部追加。 */
    public void add(int index, E element) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("插入下标越界: index=" + index + ", size=" + size);
        if (index == 0) {
            addFirst(element);
            return;
        }
        Tracer.Step s = begin("insert").arg("index", index).arg("value", element);
        Node<E> prev = nodeAt(index - 1);
        Node<E> node = new Node<>(element);
        node.next = prev.next;
        prev.next = node;
        size++;
        commit(s, "index", index);
    }

    public E removeFirst() {
        if (head == null) throw new NoSuchElementException("链表为空");
        Tracer.Step s = begin("removeFirst");
        E old = head.val;
        head = head.next;
        size--;
        commit(s, "index", 0);
        return old;
    }

    /** 按下标删除，返回被删值。 */
    public E removeAt(int index) {
        checkIndex(index);
        if (index == 0) return removeFirst();
        Tracer.Step s = begin("removeAt").arg("index", index);
        Node<E> prev = nodeAt(index - 1);
        Node<E> target = prev.next;
        prev.next = target.next;
        size--;
        commit(s, "index", index);
        return target.val;
    }

    public boolean removeValue(Object o) {
        int i = indexOf(o);
        if (i < 0) return false;
        removeAt(i);
        return true;
    }

    public void clear() {
        Tracer.Step s = begin("clear");
        head = null;
        size = 0;
        commit(s, null, null);
    }

    /* ---------------- 遍历 ---------------- */

    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            Node<E> p = head;

            @Override
            public boolean hasNext() { return p != null; }

            @Override
            public E next() {
                if (p == null) throw new NoSuchElementException();
                E v = p.val;
                p = p.next;
                return v;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("head -> ");
        for (Node<E> p = head; p != null; p = p.next) sb.append(p.val).append(" -> ");
        return sb.append("null").toString();
    }
}
