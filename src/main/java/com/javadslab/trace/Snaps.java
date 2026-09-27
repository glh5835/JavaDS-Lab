package com.javadslab.trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 快照构造助手：统一各数据结构 snapshot() 的 JSON 形状。 */
public final class Snaps {

    private Snaps() {}

    /* ---------- 通用 ---------- */

    public static Map<String, Object> obj(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i + 1]);
        return m;
    }

    public static List<Object> list(Object... vals) {
        List<Object> l = new ArrayList<>();
        for (Object v : vals) l.add(v);
        return l;
    }

    /** 单个可视化单元：值 + 状态标记。s 取值：null=普通, "cmp"/"swap"/"pivot"/"sorted"/"active"/"done" 等。 */
    public static Object cell(Object v, String s) {
        if (s == null) return obj("v", v);
        return obj("v", v, "s", s);
    }

    /* ---------- 线性数组（线性表/栈/队列/排序/KMP/DP一维） ---------- */

    public static Map<String, Object> array(String kindLabel, List<Object> cells) {
        return obj("kind", "array", "label", kindLabel, "cells", cells);
    }

    public static List<Object> intCells(int[] a, int[] marked, String mark) {
        List<Object> cells = new ArrayList<>(a.length);
        for (int x : a) cells.add(cell(x, null));
        for (int idx : marked) {
            if (idx >= 0 && idx < cells.size()) {
                cells.set(idx, cell(a[idx], mark));
            }
        }
        return cells;
    }

    /** 数组快照，把 a 中下标出现在 idx 状态映射里的元素打上对应标记。 */
    public static Map<String, Object> arrayMarked(String label, int[] a, Map<Integer, String> marks) {
        List<Object> cells = new ArrayList<>(a.length);
        for (int i = 0; i < a.length; i++) cells.add(cell(a[i], marks.get(i)));
        return array(label, cells);
    }

    public static Map<String, Object> arrayPlain(String label, int[] a) {
        return arrayMarked(label, a, Map.of());
    }

    public static Map<String, Object> arrayPlainStr(String label, List<String> items) {
        List<Object> cells = new ArrayList<>();
        for (String s : items) cells.add(cell(s, null));
        return array(label, cells);
    }

    /* ---------- 链表 ---------- */

    public static Map<String, Object> linked(List<Object> nodes) {
        return obj("kind", "linked", "nodes", nodes);
    }

    /* ---------- 哈希表 ---------- */

    public static Map<String, Object> hash(List<Object> buckets, int size, int capacity) {
        return obj("kind", "hash", "buckets", buckets, "size", size, "capacity", capacity);
    }

    /** 拉链法桶：bucket 内为若干 {"k":key,"v":value}。 */
    public static Object bucketEntry(Object k, Object v) {
        return obj("k", k, "v", v);
    }

    /* ---------- 树 ---------- */

    /** 通用树节点：keys 为主键列表（B树多键），c 为孩子，color 为 RB 红黑标记。 */
    public static Map<String, Object> treeNode(List<Object> keys, List<Object> children, String color, int hl) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("keys", keys);
        if (children != null) m.put("c", children);
        if (color != null) m.put("color", color);
        if (hl != 0) m.put("hl", hl);
        return m;
    }

    public static Map<String, Object> tree(Map<String, Object> root, String label) {
        return obj("kind", "tree", "label", label, "root", root);
    }

    /* ---------- 堆（数组表示，播放器按完全二叉树画） ---------- */

    public static Map<String, Object> heap(int[] a, int size, Map<Integer, String> marks) {
        List<Object> cells = new ArrayList<>(size);
        for (int i = 0; i < size; i++) cells.add(cell(a[i], marks.get(i)));
        return obj("kind", "heap", "cells", cells, "size", size);
    }

    /* ---------- 图 ---------- */

    /** 节点坐标用默认环形布局，播放器直接使用。 */
    public static Map<String, Object> graph(int n, boolean directed, int[][] edges, Map<Integer, String> nodeMarks,
                                            Map<String, Object> overlay) {
        List<Object> nodes = new ArrayList<>(n);
        double cx = 300, cy = 240, r = 190;
        for (int i = 0; i < n; i++) {
            double ang = 2 * Math.PI * i / Math.max(n, 1) - Math.PI / 2;
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("x", (int) Math.round(cx + r * Math.cos(ang)));
            node.put("y", (int) Math.round(cy + r * Math.sin(ang)));
            node.put("label", String.valueOf(i));
            if (nodeMarks != null && nodeMarks.get(i) != null) node.put("s", nodeMarks.get(i));
            nodes.add(node);
        }
        List<Object> es = new ArrayList<>(edges.length);
        for (int[] e : edges) {
            Map<String, Object> em = new LinkedHashMap<>();
            em.put("u", e[0]);
            em.put("v", e[1]);
            if (e.length > 2) em.put("w", e[2]);
            es.add(em);
        }
        Map<String, Object> g = obj("kind", "graph", "n", n, "directed", directed, "nodes", nodes, "edges", es);
        if (overlay != null) g.putAll(overlay);
        return g;
    }

    /* ---------- 矩阵（DP 二维表 / Floyd） ---------- */

    public static Map<String, Object> matrix(String label, int[][] a, String nullText) {
        List<Object> rows = new ArrayList<>(a.length);
        for (int[] row : a) {
            List<Object> cells = new ArrayList<>(row.length);
            for (int v : row) cells.add(v == Integer.MAX_VALUE / 2 ? cell(nullText, null) : cell(v, null));
            rows.add(cells);
        }
        return obj("kind", "matrix", "label", label, "rows", rows);
    }

    /* ---------- 并查集 ---------- */

    public static Map<String, Object> unionFind(int[] parent, int[] sizeOrRank, boolean bySize) {
        List<Object> pCells = new ArrayList<>();
        List<Object> sCells = new ArrayList<>();
        for (int i = 0; i < parent.length; i++) {
            pCells.add(cell(parent[i], parent[i] == i ? "root" : null));
            sCells.add(cell(sizeOrRank[i], null));
        }
        return obj("kind", "uf", "parent", pCells, "aux", sCells, "bySize", bySize);
    }
}
