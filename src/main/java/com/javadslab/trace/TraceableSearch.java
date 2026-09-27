package com.javadslab.trace;

import java.util.LinkedHashMap;
import java.util.Map;

/** 二分查找与 KMP 的 trace 演示。 */
public final class TraceableSearch {

    private TraceableSearch() {}

    /** 二分查找演示：每步记录 lo/mid/hi。 */
    public static Tracer binarySearch(int[] a, int key) {
        Tracer t = new Tracer("array", "二分查找 key=" + key);
        t.meta("algorithm", "binary-search").meta("input", a);
        int lo = 0, hi = a.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            Map<Integer, String> marks = new LinkedHashMap<>();
            marks.put(lo, "cmp");
            marks.put(mid, "pivot");
            marks.put(hi, "cmp");
            t.step("probe").arg("lo", lo).arg("mid", mid).arg("hi", hi).arg("a[mid]", a[mid])
                    .before(Snaps.arrayMarked("数组", a, marks))
                    .after(Snaps.arrayMarked("数组", a, marks))
                    .hl("mid", mid).commit();
            if (a[mid] == key) {
                t.step("found").arg("index", mid)
                        .before(Snaps.arrayMarked("数组", a, mapOf(mid, "sorted")))
                        .after(Snaps.arrayMarked("数组", a, mapOf(mid, "sorted")))
                        .commit();
                return t;
            }
            if (a[mid] < key) {
                lo = mid + 1;
                t.step("go-right").arg("newLo", lo)
                        .before(Snaps.arrayMarked("数组", a, mapOf(mid, "discard")))
                        .after(Snaps.arrayMarked("数组", a, mapOf(mid, "discard")))
                        .commit();
            } else {
                hi = mid - 1;
                t.step("go-left").arg("newHi", hi)
                        .before(Snaps.arrayMarked("数组", a, mapOf(mid, "discard")))
                        .after(Snaps.arrayMarked("数组", a, mapOf(mid, "discard")))
                        .commit();
            }
        }
        t.step("not-found").arg("key", key)
                .before(Snaps.arrayPlain("数组", a)).after(Snaps.arrayPlain("数组", a)).commit();
        return t;
    }

    /** KMP 匹配演示：记录主串指针 i、模式串指针 j 的移动。 */
    public static Tracer kmp(String text, String pattern) {
        Tracer t = new Tracer("monostack", "KMP 匹配 “" + pattern + "”");
        t.meta("algorithm", "kmp").meta("text", text).meta("pattern", pattern);
        int[] next = com.javadslab.core.search.KMP.buildNext(pattern);
        t.step("build-next").arg("next", next)
                .before(stateSnapshot(text, pattern, 0, 0, next))
                .after(stateSnapshot(text, pattern, 0, 0, next)).commit();
        int j = 0;
        for (int i = 0; i < text.length(); i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) {
                j = next[j - 1];
                t.step("fallback").arg("i", i).arg("newJ", j)
                        .before(stateSnapshot(text, pattern, i, j, next))
                        .after(stateSnapshot(text, pattern, i, j, next)).commit();
            }
            if (text.charAt(i) == pattern.charAt(j)) j++;
            t.step("advance").arg("i", i).arg("j", j).arg("match", String.valueOf(text.charAt(i)))
                    .before(stateSnapshot(text, pattern, i, j, next))
                    .after(stateSnapshot(text, pattern, i, j, next))
                    .hl("active", i).commit();
            if (j == pattern.length()) {
                t.step("hit").arg("start", i - j + 1)
                        .before(stateSnapshot(text, pattern, i, j, next))
                        .after(stateSnapshot(text, pattern, i, j, next)).commit();
                j = next[j - 1];
            }
        }
        return t;
    }

    private static Object stateSnapshot(String text, String pattern, int i, int j, int[] next) {
        return Snaps.obj("kind", "monostack",
                "text", cellRow(text, i),
                "pattern", cellRow(pattern, j),
                "next", Snaps.arrayPlainStr("next 数组",
                        java.util.Arrays.stream(next).mapToObj(String::valueOf).toList()),
                "i", i, "j", j);
    }

    private static Object cellRow(String s, int markIdx) {
        java.util.List<Object> cells = new java.util.ArrayList<>();
        for (int k = 0; k < s.length(); k++) cells.add(Snaps.cell(String.valueOf(s.charAt(k)), k == markIdx ? "active" : null));
        return Snaps.array("字符串", cells);
    }

    private static Map<Integer, String> mapOf(int k, String v) {
        Map<Integer, String> m = new LinkedHashMap<>();
        m.put(k, v);
        return m;
    }
}
