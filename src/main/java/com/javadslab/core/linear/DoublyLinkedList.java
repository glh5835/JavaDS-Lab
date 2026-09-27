package com.javadslab.core.linear;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 手写双向链表：带 dummy 头尾哨兵，头尾操作均 O(1)，按下标访问可从近端出发（≤ n/2）。
 * step-mode：addFirst/addLast/insert/removeFirst/removeLast/removeAt/clear 记录步骤。
 */
public class DoublyLinkedList<E> implements Iterable<E> {

    public static final class Node<E> {
        public E val;
        public Node<E> prev;
        public Node<E> next;

        Node(E val) { this.val = val; }
    }

    private final Node<E> head = new Node<>(null); // 哨兵
    private final Node<E> tail = new Node<>(null); // 哨兵
    private int size;
    private Tracer tracer;

    public DoublyLinkedList() {
        head.next = tail;
        tail.prev = head;
    }

    /* ---------------- step-mode 接口 ---------------- */

    public void attachTracer(Tracer t) { this.tracer = t; }

    /** 快照：真实节点串（不含哨兵）。 */
    public Object snapshot() {
        java.util.List<Object> nodes = new java.util.ArrayList<>(size);
        for (Node<E> p = head.next; p != tail; p = p.next) nodes.add(Snaps.cell(p.val, null));
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

    /** 智能方向查找：index 在前半段从头走，后半段从尾走，最多 n/2 步。 */
    private Node<E> nodeAt(int index) {
        if (index < size / 2) {
            Node<E> p = head.next;
            for (int i = 0; i < index; i++) p = p.next;
            return p;
        }
        Node<E> p = tail.prev;
        for (int i = size - 1; i > index; i--) p = p.prev;
        return p;
    }

    public E get(int index) {
        checkIndex(index);
        return nodeAt(index).val;
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

    public E getFirst() {
        if (size == 0) throw new NoSuchElementException("链表为空");
        return head.next.val;
    }

    public E getLast() {
        if (size == 0) throw new NoSuchElementException("链表为空");
        return tail.prev.val;
    }

    public int indexOf(Object o) {
        int i = 0;
        for (Node<E> p = head.next; p != tail; p = p.next, i++) {
            if (o == null ? p.val == null : o.equals(p.val)) return i;
        }
        return -1;
    }

    public boolean contains(Object o) { return indexOf(o) >= 0; }

    /* ---------------- 增删 ---------------- */

    private void linkBefore(Node<E> succ, Node<E> node) {
        node.prev = succ.prev;
        node.next = succ;
        succ.prev.next = node;
        succ.prev = node;
        size++;
    }

    private E unlink(Node<E> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        size--;
        return node.val;
    }

    public void addFirst(E element) {
        Tracer.Step s = begin("addFirst").arg("value", element);
        linkBefore(head.next, new Node<>(element));
        commit(s, "index", 0);
    }

    public void addLast(E element) {
        Tracer.Step s = begin("addLast").arg("value", element);
        linkBefore(tail, new Node<>(element));
        commit(s, "index", size - 1);
    }

    public void add(int index, E element) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("插入下标越界: index=" + index + ", size=" + size);
        if (index == size) {
            addLast(element);
            return;
        }
        Tracer.Step s = begin("insert").arg("index", index).arg("value", element);
        linkBefore(nodeAt(index), new Node<>(element));
        commit(s, "index", index);
    }

    public E removeFirst() {
        if (size == 0) throw new NoSuchElementException("链表为空");
        Tracer.Step s = begin("removeFirst");
        E old = unlink(head.next);
        commit(s, "index", 0);
        return old;
    }

    public E removeLast() {
        if (size == 0) throw new NoSuchElementException("链表为空");
        Tracer.Step s = begin("removeLast");
        E old = unlink(tail.prev);
        commit(s, "index", size);
        return old;
    }

    public E removeAt(int index) {
        checkIndex(index);
        Tracer.Step s = begin("removeAt").arg("index", index);
        E old = unlink(nodeAt(index));
        commit(s, "index", index);
        return old;
    }

    public boolean removeValue(Object o) {
        int i = indexOf(o);
        if (i < 0) return false;
        removeAt(i);
        return true;
    }

    public void clear() {
        Tracer.Step s = begin("clear");
        head.next = tail;
        tail.prev = head;
        size = 0;
        commit(s, null, null);
    }

    /* ---------------- 遍历 ---------------- */

    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            Node<E> p = head.next;

            @Override
            public boolean hasNext() { return p != tail; }

            @Override
            public E next() {
                if (p == tail) throw new NoSuchElementException();
                E v = p.val;
                p = p.next;
                return v;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("head <-> ");
        for (Node<E> p = head.next; p != tail; p = p.next) sb.append(p.val).append(" <-> ");
        return sb.append("tail").toString();
    }
}
