package com.javadslab.core.tree;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/** BinaryTrees / BST / AVL / 红黑树 / B 树 测试。 */
class SearchTreesTest {

    /* ================= BinaryTrees ================= */

    @Test
    void traversalOrders() {
        //        1
        //       / \
        //      2   3
        //     / \    \
        //    4   5    6
        BinaryTrees.TreeNode root = BinaryTrees.build(new Integer[]{1, 2, 3, 4, 5, null, 6});
        assertEquals(List.of(1, 2, 4, 5, 3, 6), BinaryTrees.preOrder(root));
        assertEquals(List.of(4, 2, 5, 1, 3, 6), BinaryTrees.inOrder(root));
        assertEquals(List.of(4, 5, 2, 6, 3, 1), BinaryTrees.postOrder(root));
        assertEquals(List.of(1, 2, 3, 4, 5, 6), BinaryTrees.levelOrder(root));
        // 递归与迭代一致
        assertEquals(BinaryTrees.preOrder(root), BinaryTrees.preOrderIter(root));
        assertEquals(BinaryTrees.inOrder(root), BinaryTrees.inOrderIter(root));
        assertEquals(BinaryTrees.postOrder(root), BinaryTrees.postOrderIter(root));
        assertEquals(3, BinaryTrees.height(root));
    }

    @Test
    void buildWithNullsAndEmpty() {
        assertNull(BinaryTrees.build(new Integer[]{}));
        assertNull(BinaryTrees.build(new Integer[]{null}));
        BinaryTrees.TreeNode root = BinaryTrees.build(new Integer[]{1, null, 2, null, 3});
        assertEquals(List.of(1, 2, 3), BinaryTrees.inOrder(root)); // 右斜链
        assertTrue(BinaryTrees.preOrder(null).isEmpty());
        assertEquals(0, BinaryTrees.height(null));
    }

    @Test
    void iterativeTraversalOnRandomTree() {
        Random rnd = new Random(5L);
        for (int trial = 0; trial < 50; trial++) {
            Integer[] vals = new Integer[1 + rnd.nextInt(50)];
            for (int i = 0; i < vals.length; i++) vals[i] = rnd.nextInt(100);
            BinaryTrees.TreeNode root = BinaryTrees.build(vals);
            assertEquals(BinaryTrees.inOrder(root), BinaryTrees.inOrderIter(root));
            assertEquals(BinaryTrees.preOrder(root), BinaryTrees.preOrderIter(root));
            assertEquals(BinaryTrees.postOrder(root), BinaryTrees.postOrderIter(root));
        }
    }

    /* ================= BST ================= */

    @Test
    void bstNormalOperations() {
        BST t = new BST();
        assertTrue(t.insert(5));
        assertTrue(t.insert(2));
        assertTrue(t.insert(8));
        assertTrue(t.insert(3));
        assertFalse(t.insert(2)); // 重复
        assertEquals(4, t.size());
        assertTrue(t.contains(3));
        assertFalse(t.contains(7));
        assertEquals(2, t.min());
        assertEquals(8, t.max());
        assertEquals(List.of(2, 3, 5, 8), t.inOrder());
        assertTrue(t.remove(2)); // 双孩节点：中序后继 3 顶替
        assertEquals(List.of(3, 5, 8), t.inOrder());
        assertTrue(t.remove(8));
        assertTrue(t.remove(5));
        assertTrue(t.remove(3));
        assertTrue(t.isEmpty());
        assertFalse(t.remove(99)); // 空树删不存在
    }

    @Test
    void bstBoundaryAndException() {
        BST t = new BST();
        assertThrows(NoSuchElementException.class, t::min);
        assertThrows(NoSuchElementException.class, t::max);
        t.insert(1);
        assertEquals(1, t.height());
        t.remove(1);
        t.insert(9); // 复用
        assertTrue(t.contains(9));
        // 顺序插入退化为链
        BST chain = new BST();
        for (int i = 0; i < 100; i++) chain.insert(i);
        assertEquals(100, chain.height()); // 无平衡：最坏 O(n)
    }

    @Test
    void bstRandomCrossCheckWithTreeSet() {
        Random rnd = new Random(31L);
        BST mine = new BST();
        TreeSet<Integer> ref = new TreeSet<>();
        for (int iter = 0; iter < 20_000; iter++) {
            int key = rnd.nextInt(1_000);
            int op = rnd.nextInt(4);
            if (op == 0 || op == 1) {
                assertEquals(ref.add(key), mine.insert(key));
            } else if (op == 2) {
                assertEquals(ref.contains(key), mine.contains(key));
            } else {
                assertEquals(ref.remove(key), mine.remove(key));
            }
            assertEquals(ref.size(), mine.size(), "iter " + iter);
        }
        assertEquals(new ArrayList<>(ref), mine.inOrder());
    }

    /* ================= AVL ================= */

    @Test
    void avlRotationsTriggered() {
        AVLTree t = new AVLTree();
        // RR 情形：1,2,3
        t.insert(1); t.insert(2); t.insert(3);
        t.validate();
        assertEquals(2, t.height());
        assertEquals(List.of(1, 2, 3), t.inOrder());
        // LL 情形
        AVLTree t2 = new AVLTree();
        for (int k : new int[]{3, 2, 1}) t2.insert(k);
        t2.validate();
        assertEquals(List.of(1, 2, 3), t2.inOrder());
        // LR 情形
        AVLTree t3 = new AVLTree();
        for (int k : new int[]{3, 1, 2}) t3.insert(k);
        t3.validate();
        assertEquals(2, t3.height());
        // RL 情形
        AVLTree t4 = new AVLTree();
        for (int k : new int[]{1, 3, 2}) t4.insert(k);
        t4.validate();
        assertEquals(2, t4.height());
    }

    @Test
    void avlRandomCrossCheckAndValidate() {
        Random rnd = new Random(32L);
        AVLTree mine = new AVLTree();
        TreeSet<Integer> ref = new TreeSet<>();
        for (int iter = 0; iter < 30_000; iter++) {
            int key = rnd.nextInt(2_000);
            int op = rnd.nextInt(4);
            if (op == 0 || op == 1) assertEquals(ref.add(key), mine.insert(key));
            else if (op == 2) assertEquals(ref.contains(key), mine.contains(key));
            else assertEquals(ref.remove(key), mine.remove(key));
            assertEquals(ref.size(), mine.size(), "iter " + iter);
            if (iter % 500 == 0) {
                mine.validate();
                assertEquals(new ArrayList<>(ref), mine.inOrder());
                // 高度上界 1.44 * log2(n+2)
                assertTrue(mine.height() <= 1.45 * (Math.log(ref.size() + 2) / Math.log(2)) + 1,
                        "高度超出 AVL 上界: " + mine.height());
            }
        }
        mine.validate();
    }

    /* ================= 红黑树 ================= */

    @Test
    void rbInsertDeleteBasic() {
        RedBlackTree t = new RedBlackTree();
        for (int k : new int[]{10, 20, 30, 15, 25, 5, 1}) t.insert(k);
        assertEquals(7, t.size());
        t.validate(); // 根黑 / 无红红 / 黑高一致
        assertTrue(t.contains(15));
        assertTrue(t.remove(20));
        t.validate();
        assertTrue(t.remove(10));
        t.validate();
        assertFalse(t.remove(999));
        assertEquals(List.of(1, 5, 15, 25, 30), t.inOrder());
    }

    @Test
    void rbSequentialInsertStaysBalanced() {
        RedBlackTree t = new RedBlackTree();
        for (int i = 1; i <= 1000; i++) t.insert(i); // 顺序插入
        t.validate();
        assertTrue(t.height() <= 2 * (Math.log(1001) / Math.log(2)) + 1,
                "红黑树高度应 <= 2log2(n+1): " + t.height());
        for (int i = 1; i <= 1000; i += 2) t.remove(i); // 顺序删除一半
        t.validate();
        assertEquals(500, t.size());
    }

    @Test
    void rbRandomCrossCheckAndValidate() {
        Random rnd = new Random(33L);
        RedBlackTree mine = new RedBlackTree();
        TreeSet<Integer> ref = new TreeSet<>();
        for (int iter = 0; iter < 30_000; iter++) {
            int key = rnd.nextInt(2_000);
            int op = rnd.nextInt(4);
            if (op == 0 || op == 1) assertEquals(ref.add(key), mine.insert(key));
            else if (op == 2) assertEquals(ref.contains(key), mine.contains(key));
            else assertEquals(ref.remove(key), mine.remove(key));
            assertEquals(ref.size(), mine.size(), "iter " + iter);
            if (iter % 500 == 0) {
                mine.validate();
                assertEquals(new ArrayList<>(ref), mine.inOrder());
            }
        }
        mine.validate();
    }

    /* ================= B 树 ================= */

    @Test
    void btreeInsertSearchBasic() {
        BTree t = new BTree(2); // 2-3-4 树
        for (int k : new int[]{10, 20, 30, 40, 50, 60, 70, 80}) t.insert(k);
        assertEquals(8, t.size());
        t.validate();
        for (int k : new int[]{10, 20, 30, 40, 50, 60, 70, 80}) assertTrue(t.contains(k));
        assertFalse(t.contains(35));
        assertEquals(List.of(10, 20, 30, 40, 50, 60, 70, 80), t.inOrder());
    }

    @Test
    void btreeDeleteBorrowAndMerge() {
        BTree t = new BTree(2);
        for (int i = 10; i <= 100; i += 10) t.insert(i);
        assertTrue(t.remove(40));
        assertTrue(t.remove(50));
        t.validate();
        assertFalse(t.contains(40));
        assertEquals(8, t.size());
        // 全部删空
        for (int i = 10; i <= 100; i += 10) t.remove(i);
        assertEquals(0, t.size());
        assertTrue(t.isEmpty());
        assertTrue(t.rootLeafOrEmpty());
    }

    @Test
    void btreeRandomCrossCheckAndValidate() {
        Random rnd = new Random(34L);
        for (int tDegree : new int[]{2, 3, 4}) {
            BTree mine = new BTree(tDegree);
            TreeSet<Integer> ref = new TreeSet<>();
            for (int iter = 0; iter < 20_000; iter++) {
                int key = rnd.nextInt(1_500);
                int op = rnd.nextInt(4);
                if (op == 0 || op == 1) {
                    mine.insert(key);
                    ref.add(key);
                } else if (op == 2) {
                    assertEquals(ref.contains(key), mine.contains(key));
                } else {
                    assertEquals(ref.remove(key), mine.remove(key));
                }
                assertEquals(ref.size(), mine.size(), "t=" + tDegree + " iter " + iter);
                if (iter % 1_000 == 0) {
                    mine.validate();
                    assertEquals(new ArrayList<>(ref), mine.inOrder());
                }
            }
            mine.validate();
        }
    }

    @Test
    void btreeInvalidDegree() {
        assertThrows(IllegalArgumentException.class, () -> new BTree(1));
        assertThrows(IllegalArgumentException.class, () -> new BTree(0));
    }

    /* ================= step-mode 快照 ================= */

    @Test
    void treeTracersProduceValidSnapshots() {
        var tb = new com.javadslab.trace.Tracer("tree", "BST 演示");
        BST bst = new BST();
        bst.attachTracer(tb);
        bst.insert(5); bst.insert(3); bst.remove(3);
        assertTrue(tb.stepCount() >= 3);

        var ta = new com.javadslab.trace.Tracer("tree", "AVL 演示");
        AVLTree avl = new AVLTree();
        avl.attachTracer(ta);
        avl.insert(1); avl.insert(2); avl.insert(3); // 触发旋转
        assertTrue(ta.stepCount() >= 4);

        var tr = new com.javadslab.trace.Tracer("tree", "红黑树演示");
        RedBlackTree rb = new RedBlackTree();
        rb.attachTracer(tr);
        rb.insert(1); rb.insert(2); rb.insert(3);
        assertTrue(tr.stepCount() >= 3);

        var tt = new com.javadslab.trace.Tracer("tree", "B树演示");
        BTree bt = new BTree(2);
        bt.attachTracer(tt);
        for (int i = 1; i <= 5; i++) bt.insert(i);
        assertTrue(tt.stepCount() >= 5);

        for (var t : List.of(tb, ta, tr, tt)) {
            Object parsed = com.javadslab.trace.Json.parse(com.javadslab.trace.Json.write(t.toTrace()));
            assertEquals("tree", com.javadslab.trace.Json.obj(com.javadslab.trace.Json.obj(parsed).get("meta")).get("kind"));
        }
    }
}
