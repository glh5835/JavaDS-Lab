package com.javadslab.core.tree;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 手写二叉搜索树（BST）：左 < 根 < 右。
 * insert/search O(h)；remove 三种情形（叶/单孩/双孩——用中序后继替换）。
 * step-mode：insert/remove 记录 trace。最坏退化为链（O(n)），平衡版见 AVL。
 */
public class BST {

    public static final class Node {
        public int key;
        public Node left, right;

        Node(int key) { this.key = key; }
    }

    private Node root;
    private int size;
    private Tracer tracer;

    public void attachTracer(Tracer t) { this.tracer = t; }

    public Object snapshot() {
        return Snaps.tree(nodeSnap(root, 0), "BST(size=" + size + ")");
    }

    private Object nodeSnap(Node n, int hl) {
        if (n == null) return null;
        List<Object> children = new ArrayList<>(2);
        children.add(nodeSnap(n.left, hl));
        children.add(nodeSnap(n.right, hl));
        return Snaps.treeNode(List.of(n.key), children, null, hl);
    }

    private Tracer.Step begin(String op) {
        if (tracer == null) return Tracer.inert();
        return tracer.step(op).before(snapshot());
    }

    private void commit(Tracer.Step s) {
        if (tracer == null) return;
        s.after(snapshot()).commit();
    }

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    /** 插入（重复键忽略，集合语义）。 */
    public boolean insert(int key) {
        Tracer.Step s = begin("insert").arg("key", key);
        if (root == null) {
            root = new Node(key);
            size++;
            commit(s);
            return true;
        }
        Node cur = root;
        while (true) {
            if (key == cur.key) {
                commit(s);
                return false; // 已存在
            } else if (key < cur.key) {
                if (cur.left == null) {
                    cur.left = new Node(key);
                    size++;
                    commit(s);
                    return true;
                }
                cur = cur.left;
            } else {
                if (cur.right == null) {
                    cur.right = new Node(key);
                    size++;
                    commit(s);
                    return true;
                }
                cur = cur.right;
            }
        }
    }

    public boolean contains(int key) {
        Node cur = root;
        while (cur != null) {
            if (key == cur.key) return true;
            cur = key < cur.key ? cur.left : cur.right;
        }
        return false;
    }

    public int min() {
        if (root == null) throw new NoSuchElementException("空树");
        Node cur = root;
        while (cur.left != null) cur = cur.left;
        return cur.key;
    }

    public int max() {
        if (root == null) throw new NoSuchElementException("空树");
        Node cur = root;
        while (cur.right != null) cur = cur.right;
        return cur.key;
    }

    /** 删除：叶直接删；单孩顶替；双孩用中序后继（右子树最小）替换键值后删后继。 */
    public boolean remove(int key) {
        Tracer.Step s = begin("remove").arg("key", key);
        Node parent = null, cur = root;
        while (cur != null && cur.key != key) {
            parent = cur;
            cur = key < cur.key ? cur.left : cur.right;
        }
        if (cur == null) {
            commit(s);
            return false;
        }
        if (cur.left != null && cur.right != null) {
            // 找右子树最小（中序后继）
            Node succParent = cur;
            Node succ = cur.right;
            while (succ.left != null) {
                succParent = succ;
                succ = succ.left;
            }
            cur.key = succ.key; // 后继键上移
            cur = succ;         // 转为删除后继节点（至多一个右孩）
            parent = succParent;
        }
        Node child = cur.left != null ? cur.left : cur.right;
        if (parent == null) root = child;
        else if (parent.left == cur) parent.left = child;
        else parent.right = child;
        size--;
        commit(s);
        return true;
    }

    /** 中序遍历 = 升序序列（BST 正确性的直接体现）。 */
    public List<Integer> inOrder() {
        List<Integer> out = new ArrayList<>(size);
        inOrderRec(root, out);
        return out;
    }

    private void inOrderRec(Node n, List<Integer> out) {
        if (n == null) return;
        inOrderRec(n.left, out);
        out.add(n.key);
        inOrderRec(n.right, out);
    }

    public int height() {
        return heightRec(root);
    }

    private int heightRec(Node n) {
        return n == null ? 0 : 1 + Math.max(heightRec(n.left), heightRec(n.right));
    }
}
