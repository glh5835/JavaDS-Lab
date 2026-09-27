package com.javadslab.core.tree;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 手写红黑树（CLRS 实现，null 视为黑哨兵）。
 * 五条性质：①节点红或黑 ②根黑 ③叶(null)黑 ④红节点的孩子必黑 ⑤任一节点到叶的黑高相同。
 * insert/remove O(log n)：插入红节点后「叔红变色上溯 / 叔黑旋转」；删除用后继顶替 + 双黑修复。
 */
public class RedBlackTree {

    public static final boolean RED = true;
    public static final boolean BLACK = false;

    public static final class Node {
        public int key;
        public boolean color = RED;
        public Node left, right, parent;

        Node(int key) { this.key = key; }
    }

    private Node root;
    private int size;
    private Tracer tracer;

    public void attachTracer(Tracer t) { this.tracer = t; }

    public Object snapshot() {
        return Snaps.tree(nodeSnap(root), "RBTree(size=" + size + ")");
    }

    private Object nodeSnap(Node n) {
        if (n == null) return null;
        List<Object> children = new ArrayList<>(2);
        children.add(nodeSnap(n.left));
        children.add(nodeSnap(n.right));
        return Snaps.treeNode(List.of(n.key), children, n.color == RED ? "R" : "B", 0);
    }

    private Tracer.Step begin(String op) {
        if (tracer == null) return Tracer.inert();
        return tracer.step(op).before(snapshot());
    }

    private void commit(Tracer.Step s) {
        if (tracer == null) return;
        s.after(snapshot()).commit();
    }

    private static boolean isRed(Node n) { return n != null && n.color == RED; }

    private static boolean isBlack(Node n) { return n == null || n.color == BLACK; }

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    /* -------------- 旋转（带 parent 维护） -------------- */

    private void rotateLeft(Node x) {
        Node y = x.right;
        x.right = y.left;
        if (y.left != null) y.left.parent = x;
        y.parent = x.parent;
        if (x.parent == null) root = y;
        else if (x == x.parent.left) x.parent.left = y;
        else x.parent.right = y;
        y.left = x;
        x.parent = y;
        if (tracer != null) tracer.step("rotate-left").arg("pivot", y.key).before(snapshot()).after(snapshot()).commit();
    }

    private void rotateRight(Node x) {
        Node y = x.left;
        x.left = y.right;
        if (y.right != null) y.right.parent = x;
        y.parent = x.parent;
        if (x.parent == null) root = y;
        else if (x == x.parent.right) x.parent.right = y;
        else x.parent.left = y;
        y.right = x;
        x.parent = y;
        if (tracer != null) tracer.step("rotate-right").arg("pivot", y.key).before(snapshot()).after(snapshot()).commit();
    }

    /* -------------- 插入 -------------- */

    public boolean insert(int key) {
        Tracer.Step s = begin("insert").arg("key", key);
        Node parent = null, cur = root;
        while (cur != null) {
            parent = cur;
            if (key == cur.key) {
                commit(s);
                return false;
            }
            cur = key < cur.key ? cur.left : cur.right;
        }
        Node z = new Node(key);
        z.parent = parent;
        if (parent == null) root = z;
        else if (key < parent.key) parent.left = z;
        else parent.right = z;
        size++;
        insertFixup(z);
        commit(s);
        return true;
    }

    /** 插入修复：叔红 -> 变色上溯；叔黑 -> 旋转（LL/RR 单旋，LR/RL 双旋）。 */
    private void insertFixup(Node z) {
        while (isRed(z.parent)) {
            Node parent = z.parent;
            Node grand = parent.parent;
            if (parent == grand.left) {
                Node uncle = grand.right;
                if (isRed(uncle)) {                       // 情形1：叔红
                    parent.color = BLACK;
                    uncle.color = BLACK;
                    grand.color = RED;
                    z = grand;
                } else {
                    if (z == parent.right) {              // 情形2：LR -> 转为 LL
                        z = parent;
                        rotateLeft(z);
                        parent = z.parent;
                    }
                    parent.color = BLACK;                 // 情形3：LL
                    grand.color = RED;
                    rotateRight(grand);
                }
            } else { // 镜像
                Node uncle = grand.left;
                if (isRed(uncle)) {
                    parent.color = BLACK;
                    uncle.color = BLACK;
                    grand.color = RED;
                    z = grand;
                } else {
                    if (z == parent.left) {
                        z = parent;
                        rotateRight(z);
                        parent = z.parent;
                    }
                    parent.color = BLACK;
                    grand.color = RED;
                    rotateLeft(grand);
                }
            }
        }
        root.color = BLACK;
    }

    /* -------------- 删除 -------------- */

    public boolean remove(int key) {
        Tracer.Step s = begin("remove").arg("key", key);
        Node z = findNode(key);
        if (z == null) {
            commit(s);
            return false;
        }
        Node y = z;
        boolean yOriginalColor = y.color;
        Node x, xParent;
        if (z.left == null) {
            x = z.right;
            xParent = z.parent;
            transplant(z, z.right);
        } else if (z.right == null) {
            x = z.left;
            xParent = z.parent;
            transplant(z, z.left);
        } else {
            y = minNode(z.right);
            yOriginalColor = y.color;
            if (y.parent == z) {
                x = y.right;
                xParent = y;
            } else {
                x = y.right;
                xParent = y.parent;
                transplant(y, y.right);
                y.right = z.right;
                y.right.parent = y;
            }
            transplant(z, y);
            y.left = z.left;
            y.left.parent = y;
            y.color = z.color;
        }
        size--;
        if (yOriginalColor == BLACK) deleteFixup(x, xParent);
        commit(s);
        return true;
    }

    /** 删除修复：双黑情形 1-4（兄弟红旋转降级；兄全黑上溯；近侄红远侄黑转远侄红；远侄红终结）。 */
    private void deleteFixup(Node x, Node xParent) {
        while (x != root && isBlack(x)) {
            if (x == xParent.left) {
                Node w = xParent.right; // 兄弟（必存在：黑高约束）
                if (isRed(w)) {                              // 情形1：兄红 -> 旋为兄黑
                    w.color = BLACK;
                    xParent.color = RED;
                    rotateLeft(xParent);
                    w = xParent.right;
                }
                if (isBlack(w.left) && isBlack(w.right)) {   // 情形2：兄全黑 -> 上溯
                    w.color = RED;
                    x = xParent;
                    xParent = x.parent;
                } else {
                    if (isBlack(w.right)) {                  // 情形3：近侄红远侄黑
                        if (w.left != null) w.left.color = BLACK;
                        w.color = RED;
                        rotateRight(w);
                        w = xParent.right;
                    }
                    w.color = xParent.color;                 // 情形4：远侄红
                    xParent.color = BLACK;
                    if (w.right != null) w.right.color = BLACK;
                    rotateLeft(xParent);
                    x = root;
                    xParent = null;
                }
            } else { // 镜像
                Node w = xParent.left;
                if (isRed(w)) {
                    w.color = BLACK;
                    xParent.color = RED;
                    rotateRight(xParent);
                    w = xParent.left;
                }
                if (isBlack(w.right) && isBlack(w.left)) {
                    w.color = RED;
                    x = xParent;
                    xParent = x.parent;
                } else {
                    if (isBlack(w.left)) {
                        if (w.right != null) w.right.color = BLACK;
                        w.color = RED;
                        rotateLeft(w);
                        w = xParent.left;
                    }
                    w.color = xParent.color;
                    xParent.color = BLACK;
                    if (w.left != null) w.left.color = BLACK;
                    rotateRight(xParent);
                    x = root;
                    xParent = null;
                }
            }
        }
        if (x != null) x.color = BLACK;
    }

    private void transplant(Node u, Node v) {
        if (u.parent == null) root = v;
        else if (u == u.parent.left) u.parent.left = v;
        else u.parent.right = v;
        if (v != null) v.parent = u.parent;
    }

    private Node minNode(Node n) {
        while (n.left != null) n = n.left;
        return n;
    }

    private Node findNode(int key) {
        Node cur = root;
        while (cur != null) {
            if (key == cur.key) return cur;
            cur = key < cur.key ? cur.left : cur.right;
        }
        return null;
    }

    public boolean contains(int key) {
        return findNode(key) != null;
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
        return heightRec(root);
    }

    private int heightRec(Node n) {
        return n == null ? 0 : 1 + Math.max(heightRec(n.left), heightRec(n.right));
    }

    public int min() {
        if (root == null) throw new NoSuchElementException("空树");
        return minNode(root).key;
    }

    /** 不变量校验（供测试）：性质 2/4/5 + BST 有序。 */
    public void validate() {
        if (root != null && root.color != BLACK) throw new AssertionError("性质2：根必须黑");
        blackHeight(root);
        List<Integer> keys = inOrder();
        for (int i = 1; i < keys.size(); i++)
            if (keys.get(i - 1) >= keys.get(i)) throw new AssertionError("BST 有序性被破坏");
    }

    /** 返回黑高，并沿途检查性质 4。 */
    private int blackHeight(Node n) {
        if (n == null) return 1;
        if (n.color == RED && (isRed(n.left) || isRed(n.right)))
            throw new AssertionError("性质4：红节点 " + n.key + " 有红孩子");
        int lh = blackHeight(n.left);
        int rh = blackHeight(n.right);
        if (lh != rh) throw new AssertionError("性质5：节点 " + n.key + " 黑高不等 " + lh + " vs " + rh);
        return lh + (n.color == BLACK ? 1 : 0);
    }
}
