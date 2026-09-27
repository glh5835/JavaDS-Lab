package com.javadslab.trace;

import com.javadslab.core.tree.BinaryTrees;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 二叉树遍历的 trace 演示：先序/中序/后序/层序，
 * 每步记录当前访问节点（红色高亮）与已访问集合（深色标记）。
 */
public final class TraceableTrees {

    private TraceableTrees() {}

    public static Tracer fourTraversals(Integer[] levelOrder) {
        BinaryTrees.TreeNode root = BinaryTrees.build(levelOrder);
        Tracer t = new Tracer("tree", "二叉树四种遍历");
        t.meta("algorithm", "tree-traversal");
        Set<BinaryTrees.TreeNode> visited = new LinkedHashSet<>();

        t.step("init").arg("order", "先序（根左右）")
                .before(snapshot(root, null, visited)).after(snapshot(root, null, visited)).commit();
        preOrder(root, root, t, visited);

        visited.clear();
        t.step("switch").arg("order", "中序（左根右）")
                .before(snapshot(root, null, visited)).after(snapshot(root, null, visited)).commit();
        inOrder(root, root, t, visited);

        visited.clear();
        t.step("switch").arg("order", "后序（左右根）")
                .before(snapshot(root, null, visited)).after(snapshot(root, null, visited)).commit();
        postOrder(root, root, t, visited);

        visited.clear();
        t.step("switch").arg("order", "层序（BFS）")
                .before(snapshot(root, null, visited)).after(snapshot(root, null, visited)).commit();
        levelOrder(root, t, visited);

        t.step("done").before(snapshot(root, null, visited)).after(snapshot(root, null, visited)).commit();
        return t;
    }

    private static void preOrder(BinaryTrees.TreeNode n, BinaryTrees.TreeNode root, Tracer t, Set<BinaryTrees.TreeNode> visited) {
        if (n == null) return;
        visitNode(n, root, "先序", t, visited);
        preOrder(n.left, root, t, visited);
        preOrder(n.right, root, t, visited);
    }

    private static void inOrder(BinaryTrees.TreeNode n, BinaryTrees.TreeNode root, Tracer t, Set<BinaryTrees.TreeNode> visited) {
        if (n == null) return;
        inOrder(n.left, root, t, visited);
        visitNode(n, root, "中序", t, visited);
        inOrder(n.right, root, t, visited);
    }

    private static void postOrder(BinaryTrees.TreeNode n, BinaryTrees.TreeNode root, Tracer t, Set<BinaryTrees.TreeNode> visited) {
        if (n == null) return;
        postOrder(n.left, root, t, visited);
        postOrder(n.right, root, t, visited);
        visitNode(n, root, "后序", t, visited);
    }

    private static void levelOrder(BinaryTrees.TreeNode root, Tracer t, Set<BinaryTrees.TreeNode> visited) {
        java.util.ArrayDeque<BinaryTrees.TreeNode> queue = new java.util.ArrayDeque<>();
        if (root != null) queue.add(root);
        while (!queue.isEmpty()) {
            BinaryTrees.TreeNode n = queue.poll();
            visitNode(n, root, "层序", t, visited);
            if (n.left != null) queue.add(n.left);
            if (n.right != null) queue.add(n.right);
        }
    }

    private static void visitNode(BinaryTrees.TreeNode n, BinaryTrees.TreeNode root,
                                  String order, Tracer t, Set<BinaryTrees.TreeNode> visited) {
        visited.add(n);
        t.step("visit").arg("order", order).arg("node", n.val).arg("visitSeq", visited.size())
                .before(snapshot(root, n, visited))
                .after(snapshot(root, n, visited))
                .hl("active", n.val).commit();
    }

    /** 树快照：已访问节点深色、当前节点红色高亮。 */
    private static Object snapshot(BinaryTrees.TreeNode root, BinaryTrees.TreeNode current, Set<BinaryTrees.TreeNode> visited) {
        return Snaps.tree(nodeSnap(root, current, visited), "二叉树遍历（深=已访问, 红=当前）");
    }

    private static Object nodeSnap(BinaryTrees.TreeNode n, BinaryTrees.TreeNode current, Set<BinaryTrees.TreeNode> visited) {
        if (n == null) return null;
        List<Object> children = new ArrayList<>(2);
        children.add(nodeSnap(n.left, current, visited));
        children.add(nodeSnap(n.right, current, visited));
        String color = (current != n && visited.contains(n)) ? "B" : null;
        int hl = current == n ? 1 : 0;
        return Snaps.treeNode(List.of(n.val), children, color, hl);
    }
}
