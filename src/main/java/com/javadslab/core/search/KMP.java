package com.javadslab.core.search;

import java.util.ArrayList;
import java.util.List;

/**
 * KMP 字符串匹配。
 * next[i]（失败函数）= pattern[0..i] 的最长相等真前后缀长度。
 * 匹配失配时 j = next[j-1] 回退，主串指针永不回退。O(n+m)。
 */
public final class KMP {

    private KMP() {}

    /** 构造 next 数组。O(m)。 */
    public static int[] buildNext(String pattern) {
        if (pattern == null || pattern.isEmpty()) throw new IllegalArgumentException("模式串不能为空");
        int m = pattern.length();
        int[] next = new int[m];
        int len = 0; // 当前最长相等真前后缀长度
        for (int i = 1; i < m; i++) {
            while (len > 0 && pattern.charAt(i) != pattern.charAt(len)) {
                len = next[len - 1]; // 关键回退
            }
            if (pattern.charAt(i) == pattern.charAt(len)) len++;
            next[i] = len;
        }
        return next;
    }

    /** 返回 pattern 在 text 中的全部出现下标（可重叠）。O(n+m)。 */
    public static List<Integer> searchAll(String text, String pattern) {
        if (pattern == null || pattern.isEmpty()) throw new IllegalArgumentException("模式串不能为空");
        List<Integer> hits = new ArrayList<>();
        if (text == null || text.length() < pattern.length()) return hits;
        int[] next = buildNext(pattern);
        int j = 0; // 已匹配的模式串长度
        for (int i = 0; i < text.length(); i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) {
                j = next[j - 1];
            }
            if (text.charAt(i) == pattern.charAt(j)) j++;
            if (j == pattern.length()) {
                hits.add(i - j + 1);
                j = next[j - 1]; // 允许重叠匹配
            }
        }
        return hits;
    }

    /** 首次出现下标（无则 -1）。 */
    public static int indexOf(String text, String pattern) {
        List<Integer> hits = searchAll(text, pattern);
        return hits.isEmpty() ? -1 : hits.get(0);
    }
}
