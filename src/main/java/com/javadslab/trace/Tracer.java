package com.javadslab.trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作追踪器：数据结构在开启 step-mode 后，每个变更操作通过这里记录成一条步骤。
 * 每条步骤 = 操作类型 + 参数 + 操作前快照 + 操作后快照 + 高亮标注 + 可选说明。
 * 整个 Tracer 可序列化为 {"meta":{...}, "steps":[...]} 供前端播放器回放。
 *
 * {@link #inert()} 返回一个全程空操作的 Step，供未开启追踪的数据结构链式调用，
 * 避免调用方判空，也让「关追踪」时零开销。
 */
public final class Tracer {

    private final Map<String, Object> meta = new LinkedHashMap<>();
    private final List<Object> steps = new ArrayList<>();
    /** trace 步数上限，防止误开在大数据量上生成巨型文件。 */
    private static final int MAX_STEPS = 200_000;

    /** 全程空操作的惰性步骤（owner 为 null）。 */
    private static final Step INERT = new Step(null, "inert", -1);

    public Tracer(String kind, String title) {
        meta.put("kind", kind);
        meta.put("title", title);
    }

    public Tracer meta(String key, Object value) {
        meta.put(key, value);
        return this;
    }

    public int stepCount() { return steps.size(); }

    /** 开始一条新步骤，链式填充内容后调用 commit()。 */
    public Step step(String op) {
        if (steps.size() >= MAX_STEPS) throw new IllegalStateException("trace 步数超过上限 " + MAX_STEPS);
        return new Step(this, op, steps.size());
    }

    /** 未开启追踪时使用的惰性步骤：所有方法都是 no-op。 */
    public static Step inert() {
        return INERT;
    }

    /** 组装完整 trace JSON。 */
    public Object toTrace() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("meta", meta);
        root.put("steps", steps);
        return root;
    }

    public static final class Step {
        private final Tracer owner;
        private final Map<String, Object> m;

        private Step(Tracer owner, String op, int index) {
            this.owner = owner;
            if (owner == null) {
                this.m = null;
            } else {
                this.m = new LinkedHashMap<>();
                m.put("i", index);
                m.put("op", op);
            }
        }

        public Step arg(String key, Object value) {
            if (owner == null) return this;
            m.computeIfAbsent("args", k -> new LinkedHashMap<String, Object>());
            Json.obj(m.get("args")).put(key, value);
            return this;
        }

        public Step before(Object snapshot) {
            if (owner == null) return this;
            m.put("before", snapshot);
            return this;
        }

        public Step after(Object snapshot) {
            if (owner == null) return this;
            m.put("after", snapshot);
            return this;
        }

        /** 高亮信息，如 hl("active", 3)、hl("path", List.of(1,2))。 */
        public Step hl(String key, Object value) {
            if (owner == null) return this;
            m.computeIfAbsent("hl", k -> new LinkedHashMap<String, Object>());
            Json.obj(m.get("hl")).put(key, value);
            return this;
        }

        public Step note(String text) {
            if (owner == null) return this;
            m.put("note", text);
            return this;
        }

        public void commit() {
            if (owner == null) return;
            owner.steps.add(m);
        }
    }
}
