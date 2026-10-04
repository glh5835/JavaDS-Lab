package com.javadslab.workbench;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 练习库：启动时解析 docs/notes/ 22 篇笔记中的"练习题（附答案）"小节，
 * 结构化为 110 题（笔记原文是唯一事实来源，不复制第二份题目文本）。
 * 每题：practiceId（<noteId>-<序号>）、所属笔记、关联功能（按笔记映射）、题目、参考答案。
 */
public final class PracticeLibrary {

    private static final Pattern ITEM_START = Pattern.compile("^(\\d+)[.、]\\s*(.*)$");
    private static final Pattern ANSWER_START = Pattern.compile("^\\s*答[：:]\\s*(.*)$");

    public record Practice(
            String practiceId,
            String noteId,
            String noteTitle,
            String featureId,          // 主功能（笔记映射的第一个）
            List<String> featureIds,   // 笔记覆盖的全部功能
            String question,
            String answer,
            int index) {}

    private final List<Practice> all = new ArrayList<>();
    private final Map<String, Practice> byId = new LinkedHashMap<>();
    private final Map<String, List<Practice>> byFeature = new LinkedHashMap<>();
    private final Map<String, String> noteTitles = new LinkedHashMap<>();

    public PracticeLibrary(Path notesDir) throws IOException {
        List<Path> files = new ArrayList<>();
        try (var stream = Files.list(notesDir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".md"))
                    .sorted().forEach(files::add);
        }
        for (Path f : files) {
            String fileName = f.getFileName().toString();
            String noteId = fileName.substring(0, fileName.length() - 3); // 去 .md
            String content = Files.readString(f, StandardCharsets.UTF_8);
            String title = extractTitle(content, fileName);
            noteTitles.put(noteId, title);
            List<String> featureIds = new ArrayList<>();
            for (FeatureCatalog.FeatureDef d : FeatureCatalog.byNote(noteId)) featureIds.add(d.id());
            String primary = featureIds.isEmpty() ? "" : featureIds.get(0);

            List<String[]> items = parsePracticeSection(content);
            int idx = 0;
            for (String[] qa : items) {
                idx++;
                String pid = noteId + "-" + idx;
                Practice p = new Practice(pid, noteId, title, primary,
                        List.copyOf(featureIds), qa[0], qa[1], idx);
                all.add(p);
                byId.put(pid, p);
                for (String fid : featureIds) {
                    byFeature.computeIfAbsent(fid, k -> new ArrayList<>()).add(p);
                }
            }
        }
    }

    private String extractTitle(String content, String fallback) {
        for (String line : content.split("\r?\n")) {
            if (line.startsWith("# ") && line.length() > 2) {
                return line.substring(2).trim();
            }
            if (line.startsWith("#")) break;
            if (!line.isBlank()) break;
        }
        return fallback;
    }

    /** 解析"## 练习题"小节为 [题目, 答案] 列表；小节不存在返回空表。 */
    static List<String[]> parsePracticeSection(String content) {
        List<String[]> out = new ArrayList<>();
        String[] lines = content.split("\r?\n");
        int i = 0;
        while (i < lines.length && !lines[i].startsWith("## 练习题")) i++;
        if (i >= lines.length) return out;
        i++; // 跳过标题行

        String curQ = null;
        StringBuilder curA = new StringBuilder();
        while (i < lines.length) {
            String line = lines[i];
            if (line.startsWith("## ") && !line.startsWith("## 练习题")) break; // 下一个小节
            Matcher item = ITEM_START.matcher(line);
            Matcher ans = ANSWER_START.matcher(line);
            if (item.matches()) {
                if (curQ != null) out.add(new String[]{curQ, curA.toString().trim()});
                String q = item.group(2).trim();
                // 剥掉题干中的 markdown 加粗标记，保留纯文本
                q = q.replace("**", "");
                curQ = q;
                curA = new StringBuilder();
            } else if (ans.matches() && curQ != null) {
                if (curA.length() > 0) curA.append('\n');
                curA.append(ans.group(1).trim());
            } else if (curQ != null && !line.isBlank()) {
                // 题干或答案的续行
                String t = line.trim();
                if (curA.length() == 0 && !t.startsWith("答")) {
                    curQ = curQ + "\n" + t;
                } else {
                    curA.append('\n').append(t);
                }
            }
            i++;
        }
        if (curQ != null) out.add(new String[]{curQ, curA.toString().trim()});
        return out;
    }

    public List<Practice> all() {
        return all;
    }

    public int count() {
        return all.size();
    }

    public java.util.Optional<Practice> find(String practiceId) {
        return java.util.Optional.ofNullable(byId.get(practiceId));
    }

    /** 按功能过滤题目（多篇笔记覆盖同一功能时合并）。 */
    public List<Practice> byFeature(String featureId) {
        return byFeature.getOrDefault(featureId, List.of());
    }

    public Map<String, String> noteTitles() {
        return noteTitles;
    }

    public Map<String, Object> toMap(Practice p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("practiceId", p.practiceId());
        m.put("noteId", p.noteId());
        m.put("noteTitle", p.noteTitle());
        m.put("featureId", p.featureId());
        m.put("featureIds", p.featureIds());
        m.put("index", p.index());
        return m;
    }
}
