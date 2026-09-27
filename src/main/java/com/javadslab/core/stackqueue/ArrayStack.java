package com.javadslab.core.stackqueue;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.Arrays;
import java.util.EmptyStackException;

/**
 * 手写顺序栈：数组 + 栈顶指针，push/pop 均 O(1) 摊还（扩容 1.5 倍）。
 * step-mode：push/pop/clear 产生 trace 步骤。
 */
public class ArrayStack<E> {

    private Object[] data;
    private int top; // 指向下一个可写位置，即栈中元素个数
    private Tracer tracer;

    public ArrayStack() { data = new Object[8]; }

    /* ---------------- step-mode ---------------- */

    public void attachTracer(Tracer t) { this.tracer = t; }

    public Object snapshot() {
        java.util.List<Object> cells = new java.util.ArrayList<>(top);
        for (int i = 0; i < top; i++) cells.add(Snaps.cell(data[i], null));
        return Snaps.array("ArrayStack(top=" + top + ")", cells);
    }

    private Tracer.Step begin(String op) {
        if (tracer == null) return Tracer.inert();
        return tracer.step(op).before(snapshot());
    }

    private void commit(Tracer.Step s) {
        if (tracer == null) return;
        s.after(snapshot()).commit();
    }

    /* ---------------- 栈操作 ---------------- */

    public int size() { return top; }

    public boolean isEmpty() { return top == 0; }

    /** 入栈：O(1) 摊还。 */
    public E push(E element) {
        Tracer.Step s = begin("push").arg("value", element);
        if (top == data.length) data = Arrays.copyOf(data, data.length + (data.length >> 1));
        data[top++] = element;
        commit(s.hl("top", top - 1));
        return element;
    }

    /** 出栈：O(1)。空栈抛 EmptyStackException。 */
    @SuppressWarnings("unchecked")
    public E pop() {
        if (top == 0) throw new EmptyStackException();
        Tracer.Step s = begin("pop");
        E val = (E) data[--top];
        data[top] = null;
        commit(s.hl("top", top));
        return val;
    }

    /** 查看栈顶但不弹出。 */
    @SuppressWarnings("unchecked")
    public E peek() {
        if (top == 0) throw new EmptyStackException();
        return (E) data[top - 1];
    }

    public void clear() {
        Tracer.Step s = begin("clear");
        Arrays.fill(data, 0, top, null);
        top = 0;
        commit(s);
    }
}
