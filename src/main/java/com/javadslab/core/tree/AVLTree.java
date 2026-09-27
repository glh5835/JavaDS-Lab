package com.javadslab.core.tree;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 手写 AVL 树：自平衡 BST，任意节点 |balanceFactor| <= 1。
 * 平衡因子 bf = height(left) - height(right)；插入/删除后回溯更新高度并旋转
 * （LL->右旋，RR->左旋，LR->左右双旋，RL->右左双旋）。增删查均 O(log n)。
 */
public class AVLTree {

    public static final class Node {
        public int key;
        public int height;      // 叶=1
        public Node left, right;

        Node(int key) { this.key = key; this.height = 1; }
    }

    private Node root;
    private int size;
    private Tracer tracer;

    public void attachTracer(Tracer t) { this.tracer = t; }

    public Object snapshot() {
        return Snaps.tree(nodeSnap(root, 0), "AVL(size=" + size + ", h=" + height(root) + ")");
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

    private static int height(Node n) { return n == null ? 0 : n.height; }

    private static void update(Node n) {
        n.height = 1 + Math.max(height(n.left), height(n.right));
    }

    private static int balanceFactor(Node n) {
        return height(n.left) - height(n.right);
    }

    /* -------------- 旋转 -------------- */

    private Node rotateRight(Node y) {
        Node x = y.left;
        y.left = x.right;
        x.right = y;
        update(y);
        update(x);
        if (tracer != null) {
            tracer.step("rotate-right").arg("pivot", x.key)
                    .before(snapshot()).after(snapshot()).commit();
        }
        return x;
    }

    private Node rotateLeft(Node x) {
        Node y = x.right;
        x.right = y.left;
        y.left = x;
        update(x);
        update(y);
        if (tracer != null) {
            tracer.step("rotate-left").arg("pivot", y.key)
                    .before(snapshot()).after(snapshot()).commit();
        }
        return y;
    }

    private Node rebalance(Node n) {
        update(n);
        int bf = balanceFactor(n);
        if (bf > 1) {
            if (balanceFactor(n.left) < 0) {
                n.left = rotateLeft(n.left);  // LR：先左旋左子
            }
            return rotateRight(n);            // LL
        }
        if (bf < -1) {
            if (balanceFactor(n.right) > 0) {
                n.right = rotateRight(n.right); // RL：先右旋右子
            }
            return rotateLeft(n);               // RR
        }
        return n;
    }

    /* -------------- 增删查 -------------- */

    /** 插入（重复键忽略）。 */
    public boolean insert(int key) {
        Tracer.Step s = begin("insert").arg("key", key);
        int before = size;
        root = insertRec(root, key);
        if (s != null && size != before) commit(s);
        else if (tracer != null) tracer.step("noop").arg("key", key).note("已存在").before(snapshot()).after(snapshot()).commit();
        return size != before;
    }

    private Node insertRec(Node n, int key) {
        if (n == null) {
            size++;
            return new Node(key);
        }
        if (key < n.key) n.left = insertRec(n.left, key);
        else if (key > n.key) n.right = insertRec(n.right, key);
        else return n;
        return rebalance(n);
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

    /** 删除（键不存在返回 false）。size 只在入口减一次，removeRec 不再维护 size。 */
    public boolean remove(int key) {
        if (!contains(key)) {
            if (tracer != null) {
                tracer.step("noop").arg("key", key).note("不存在")
                        .before(snapshot()).after(snapshot()).commit();
            }
            return false;
        }
        Tracer.Step s = begin("remove").arg("key", key);
        size--;
        root = removeRec(root, key);
        commit(s);
        return true;
    }

    private Node removeRec(Node n, int key) {
        if (n == null) return null;
        if (key < n.key) {
            n.left = removeRec(n.left, key);
        } else if (key > n.key) {
            n.right = removeRec(n.right, key);
        } else {
            if (n.left == null) return n.right;
            if (n.right == null) return n.left;
            // 双孩：用中序后继键替换，再删后继（后继必然存在）
            Node succ = n.right;
            while (succ.left != null) succ = succ.left;
            n.key = succ.key;
            n.right = removeRec(n.right, succ.key);
        }
        return rebalance(n);
    }

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
        return height(root);
    }

    /** 不变量校验：AVL 平衡性（|bf|<=1）+ BST 有序性。供测试调用。 */
    public void validate() {
        validateRec(root, null, null);
    }

    private void validateRec(Node n, Integer lo, Integer hi) {
        if (n == null) return;
        if (lo != null && n.key <= lo) throw new AssertionError("BST 有序性被破坏: " + n.key + " <= " + lo);
        if (hi != null && n.key >= hi) throw new AssertionError("BST 有序性被破坏: " + n.key + " >= " + hi);
        int bf = balanceFactor(n);
        if (Math.abs(bf) > 1) throw new AssertionError("AVL 平衡被破坏: key=" + n.key + " bf=" + bf);
        if (n.height != 1 + Math.max(height(n.left), height(n.right)))
            throw new AssertionError("高度信息错误: key=" + n.key);
        validateRec(n.left, lo, n.key);
        validateRec(n.right, n.key, hi);
    }
}
