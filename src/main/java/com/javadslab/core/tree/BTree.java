package com.javadslab.core.tree;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 手写 B 树（最小度 t，CLRS 定义：节点键数 [t-1, 2t-1]，根除外下限）。
 * 全部叶同深度；insert 预分裂（自顶向下，一次遍历）；delete 借位/合并回溯。
 * 典型 t=2 退化为 2-3-4 树。
 */
public class BTree {

    public static final class Node {
        public final List<Integer> keys = new ArrayList<>();
        public final List<Node> children = new ArrayList<>();
        public boolean leaf = true;
    }

    private final int t; // 最小度 ≥ 2
    private Node root = new Node();
    private int size;
    private Tracer tracer;

    public BTree(int minDegree) {
        if (minDegree < 2) throw new IllegalArgumentException("最小度 t 必须 >= 2: " + minDegree);
        this.t = minDegree;
    }

    public BTree() { this(2); }

    /* ---------------- step-mode ---------------- */

    public void attachTracer(Tracer t) { this.tracer = t; }

    public Object snapshot() {
        return Snaps.tree(nodeSnap(root), "BTree(t=" + t + ", size=" + size + ")");
    }

    private Object nodeSnap(Node n) {
        if (n == null) return null;
        List<Object> children = new ArrayList<>();
        if (!n.leaf) for (Node c : n.children) children.add(nodeSnap(c));
        return Snaps.treeNode(new ArrayList<>(n.keys), children, null, 0);
    }

    private Tracer.Step begin(String op) {
        if (tracer == null) return Tracer.inert();
        return tracer.step(op).before(snapshot());
    }

    private void commit(Tracer.Step s, String note) {
        if (tracer == null) return;
        if (note != null) s.note(note);
        s.after(snapshot()).commit();
    }

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    /** 根是否已回落为空叶节点（删除全部元素后成立）。 */
    public boolean rootLeafOrEmpty() {
        return root.leaf && root.keys.isEmpty();
    }

    /* ---------------- 查找 ---------------- */

    public boolean contains(int key) {
        return search(root, key) != null;
    }

    private Node search(Node n, int key) {
        int i = 0;
        while (i < n.keys.size() && key > n.keys.get(i)) i++;
        if (i < n.keys.size() && key == n.keys.get(i)) return n;
        if (n.leaf) return null;
        return search(n.children.get(i), key);
    }

    /* ---------------- 插入（自顶向下预分裂） ---------------- */

    public void insert(int key) {
        Tracer.Step s = begin("insert").arg("key", key);
        if (contains(key)) {
            commit(s, "已存在，忽略");
            return;
        }
        Node r = root;
        if (r.keys.size() == 2 * t - 1) {          // 根满：分裂升根
            Node newRoot = new Node();
            newRoot.leaf = false;
            newRoot.children.add(r);
            splitChild(newRoot, 0);
            root = newRoot;
            if (tracer != null) tracer.step("split-root").before(snapshot()).after(snapshot()).commit();
        }
        insertNonFull(root, key);
        size++;
        commit(s, null);
    }

    /** 分裂 children[i]（满节点）：中间键上移到父，右半成新节点。 */
    private void splitChild(Node parent, int i) {
        Node full = parent.children.get(i);
        Node right = new Node();
        right.leaf = full.leaf;
        int mid = t - 1;
        Integer upKey = full.keys.get(mid);
        right.keys.addAll(full.keys.subList(mid + 1, full.keys.size()));
        full.keys.subList(mid, full.keys.size()).clear(); // 含被上移键
        if (!full.leaf) {
            right.children.addAll(full.children.subList(mid + 1, full.children.size()));
            full.children.subList(mid + 1, full.children.size()).clear();
        }
        parent.keys.add(i, upKey);
        parent.children.add(i + 1, right);
        if (tracer != null) {
            tracer.step("split-child").arg("upKey", upKey)
                    .before(snapshot()).after(snapshot()).commit();
        }
    }

    private void insertNonFull(Node n, int key) {
        int i = n.keys.size() - 1;
        if (n.leaf) {
            n.keys.add(0);
            while (i >= 0 && key < n.keys.get(i)) {
                n.keys.set(i + 1, n.keys.get(i));
                i--;
            }
            n.keys.set(i + 1, key);
        } else {
            while (i >= 0 && key < n.keys.get(i)) i--;
            i++;
            if (n.children.get(i).keys.size() == 2 * t - 1) {
                splitChild(n, i);
                if (key > n.keys.get(i)) i++;
            }
            insertNonFull(n.children.get(i), key);
        }
    }

    /* ---------------- 删除（借位 / 合并回溯） ---------------- */

    public boolean remove(int key) {
        Tracer.Step s = begin("remove").arg("key", key);
        if (!contains(key)) {
            commit(s, "不存在");
            return false;
        }
        removeRec(root, key);
        if (root.keys.isEmpty() && !root.leaf) root = root.children.get(0); // 降根
        size--;
        commit(s, null);
        return true;
    }

    private void removeRec(Node n, int key) {
        int idx = lowerBound(n.keys, key);
        if (idx < n.keys.size() && n.keys.get(idx) == key) {
            if (n.leaf) {
                n.keys.remove(idx);               // 情形1：叶直接删
                return;
            }
            // 情形3：内部节点
            Node left = n.children.get(idx), right = n.children.get(idx + 1);
            if (left.keys.size() >= t) {           // 3a：左子树前驱顶替
                int pred = rightmost(left);
                n.keys.set(idx, pred);
                removeRec(left, pred);
            } else if (right.keys.size() >= t) {   // 3b：右子树后继顶替
                int succ = leftmost(right);
                n.keys.set(idx, succ);
                removeRec(right, succ);
            } else {                               // 3c：合并后递归删
                mergeChildren(n, idx);
                removeRec(left, key);
            }
            return;
        }
        if (n.leaf) return; // 键不在树中（外层已 contains 校验）
        // 情形2：下潜前保证子节点至少 t 个键
        Node child = n.children.get(idx);
        if (child.keys.size() < t) {
            Node prevSibling = idx > 0 ? n.children.get(idx - 1) : null;
            Node nextSibling = idx < n.children.size() - 1 ? n.children.get(idx + 1) : null;
            if (prevSibling != null && prevSibling.keys.size() >= t) {
                borrowFromPrev(n, idx);
            } else if (nextSibling != null && nextSibling.keys.size() >= t) {
                borrowFromNext(n, idx);
            } else {
                Node merged = mergeChildren(n, idx > 0 ? idx - 1 : idx);
                removeRec(merged, key);
                return;
            }
        }
        removeRec(n.children.get(idx), key);
    }

    private int lowerBound(List<Integer> keys, int key) {
        int lo = 0, hi = keys.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (keys.get(mid) < key) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    private int rightmost(Node n) {
        while (!n.leaf) n = n.children.get(n.children.size() - 1);
        return n.keys.get(n.keys.size() - 1);
    }

    private int leftmost(Node n) {
        while (!n.leaf) n = n.children.get(0);
        return n.keys.get(0);
    }

    /** 把 keys[idx] 与 children[idx], children[idx+1] 合并进 children[idx] 节点，返回该节点。 */
    private Node mergeChildren(Node parent, int idx) {
        Node left = parent.children.get(idx);
        Node right = parent.children.get(idx + 1);
        Integer downKey = parent.keys.get(idx);
        left.keys.add(downKey);
        left.keys.addAll(right.keys);
        if (!left.leaf) left.children.addAll(right.children);
        parent.keys.remove(idx);
        parent.children.remove(idx + 1);
        if (tracer != null) {
            tracer.step("merge").arg("downKey", downKey)
                    .before(snapshot()).after(snapshot()).commit();
        }
        return left;
    }

    private void borrowFromPrev(Node parent, int idx) {
        Node child = parent.children.get(idx);
        Node prev = parent.children.get(idx - 1);
        child.keys.add(0, parent.keys.get(idx - 1));
        parent.keys.set(idx - 1, prev.keys.get(prev.keys.size() - 1));
        if (!child.leaf) {
            child.children.add(0, prev.children.get(prev.children.size() - 1));
            prev.children.remove(prev.children.size() - 1);
        }
        prev.keys.remove(prev.keys.size() - 1);
        if (tracer != null) tracer.step("borrow-prev").arg("childIdx", idx).before(snapshot()).after(snapshot()).commit();
    }

    private void borrowFromNext(Node parent, int idx) {
        Node child = parent.children.get(idx);
        Node next = parent.children.get(idx + 1);
        child.keys.add(parent.keys.get(idx));
        parent.keys.set(idx, next.keys.get(0));
        if (!child.leaf) {
            child.children.add(next.children.get(0));
            next.children.remove(0);
        }
        next.keys.remove(0);
        if (tracer != null) tracer.step("borrow-next").arg("childIdx", idx).before(snapshot()).after(snapshot()).commit();
    }

    /* ---------------- 遍历与校验 ---------------- */

    /** 中序遍历（有序输出）。 */
    public List<Integer> inOrder() {
        List<Integer> out = new ArrayList<>(size);
        inOrderRec(root, out);
        return out;
    }

    private void inOrderRec(Node n, List<Integer> out) {
        for (int i = 0; i < n.keys.size(); i++) {
            if (!n.leaf) inOrderRec(n.children.get(i), out);
            out.add(n.keys.get(i));
        }
        if (!n.leaf) inOrderRec(n.children.get(n.children.size() - 1), out);
    }

    /** 不变量校验：键数范围 / 有序 / 叶同深 / 孩子数。 */
    public void validate() {
        if (root.keys.size() > 2 * t - 1) throw new AssertionError("根键数超上限");
        int leafDepth = -1;
        java.util.Deque<Object[]> stack = new java.util.ArrayDeque<>();
        stack.push(new Object[]{root, 0});
        while (!stack.isEmpty()) {
            Object[] e = stack.pop();
            Node n = (Node) e[0];
            int depth = (int) e[1];
            if (n != root && n.keys.size() < t - 1) throw new AssertionError("键数低于下限: " + n.keys);
            if (n.keys.size() > 2 * t - 1) throw new AssertionError("键数超上限: " + n.keys);
            for (int i = 1; i < n.keys.size(); i++)
                if (n.keys.get(i - 1) >= n.keys.get(i)) throw new AssertionError("节点内键无序");
            if (n.leaf) {
                if (leafDepth == -1) leafDepth = depth;
                else if (leafDepth != depth) throw new AssertionError("叶深度不同");
            } else {
                if (n.children.size() != n.keys.size() + 1) throw new AssertionError("孩子数 = 键数+1 不成立");
                for (Node c : n.children) stack.push(new Object[]{c, depth + 1});
            }
        }
    }
}
