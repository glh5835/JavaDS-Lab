package com.javadslab.core.tree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 二叉树遍历工具集：先序/中序/后序（递归 + 迭代）、层序、高度。
 * 节点按完全二叉树数组约定构建：values[i] 为 null 表示该位置无节点。
 */
public final class BinaryTrees {

    /** 二叉树节点（键为 int，教学简化）。 */
    public static final class TreeNode {
        public int val;
        public TreeNode left;
        public TreeNode right;

        public TreeNode(int val) { this.val = val; }
    }

    private BinaryTrees() {}

    /** 按层序数组建树（LeetCode 风格）：每个出队节点依次消耗两个槽位，null 表示该槽位为空。 */
    public static TreeNode build(Integer[] values) {
        if (values == null || values.length == 0 || values[0] == null) return null;
        TreeNode root = new TreeNode(values[0]);
        ArrayDeque<TreeNode> queue = new ArrayDeque<>();
        queue.add(root);
        int i = 1;
        while (!queue.isEmpty() && i < values.length) {
            TreeNode parent = queue.poll();
            if (i < values.length) {
                Integer lv = values[i++];
                if (lv != null) { parent.left = new TreeNode(lv); queue.add(parent.left); }
            }
            if (i < values.length) {
                Integer rv = values[i++];
                if (rv != null) { parent.right = new TreeNode(rv); queue.add(parent.right); }
            }
        }
        return root;
    }

    /* ---------------- 先序 ---------------- */

    public static List<Integer> preOrder(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        preRec(root, out);
        return out;
    }

    private static void preRec(TreeNode n, List<Integer> out) {
        if (n == null) return;
        out.add(n.val);
        preRec(n.left, out);
        preRec(n.right, out);
    }

    /** 迭代先序：根 -> 右 -> 左压栈。 */
    public static List<Integer> preOrderIter(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        Deque<TreeNode> stack = new ArrayDeque<>();
        if (root != null) stack.push(root);
        while (!stack.isEmpty()) {
            TreeNode n = stack.pop();
            out.add(n.val);
            if (n.right != null) stack.push(n.right);
            if (n.left != null) stack.push(n.left);
        }
        return out;
    }

    /* ---------------- 中序 ---------------- */

    public static List<Integer> inOrder(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        inRec(root, out);
        return out;
    }

    private static void inRec(TreeNode n, List<Integer> out) {
        if (n == null) return;
        inRec(n.left, out);
        out.add(n.val);
        inRec(n.right, out);
    }

    /** 迭代中序：一路向左压栈，弹出后转右。 */
    public static List<Integer> inOrderIter(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode cur = root;
        while (cur != null || !stack.isEmpty()) {
            while (cur != null) {
                stack.push(cur);
                cur = cur.left;
            }
            cur = stack.pop();
            out.add(cur.val);
            cur = cur.right;
        }
        return out;
    }

    /* ---------------- 后序 ---------------- */

    public static List<Integer> postOrder(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        postRec(root, out);
        return out;
    }

    private static void postRec(TreeNode n, List<Integer> out) {
        if (n == null) return;
        postRec(n.left, out);
        postRec(n.right, out);
        out.add(n.val);
    }

    /** 迭代后序：前序变形（根右左）取逆，即得左右根。 */
    public static List<Integer> postOrderIter(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        Deque<TreeNode> stack = new ArrayDeque<>();
        if (root != null) stack.push(root);
        while (!stack.isEmpty()) {
            TreeNode n = stack.pop();
            out.add(n.val);
            if (n.left != null) stack.push(n.left);
            if (n.right != null) stack.push(n.right);
        }
        java.util.Collections.reverse(out);
        return out;
    }

    /* ---------------- 层序 / 高度 ---------------- */

    /** 层序遍历（BFS）。 */
    public static List<Integer> levelOrder(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        if (root == null) return out;
        ArrayDeque<TreeNode> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            TreeNode n = queue.poll();
            out.add(n.val);
            if (n.left != null) queue.add(n.left);
            if (n.right != null) queue.add(n.right);
        }
        return out;
    }

    /** 树高（空树 0，单节点 1）。 */
    public static int height(TreeNode root) {
        if (root == null) return 0;
        return 1 + Math.max(height(root.left), height(root.right));
    }
}
