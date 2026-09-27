package com.javadslab.core.hash;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * 手写哈希表——开放寻址法（线性探测 Linear Probing）。
 * 核心：冲突后向后线性找空位；删除用「墓碑」标记，避免探测链断裂。
 * 负载因子 > 0.6 扩容（开放寻址对装载更敏感）。TOMBSTONE 参与探测、不计入 size。
 */
public class OpenAddressingHashMap<K, V> {

    private static final class Slot<K, V> {
        static final Slot<?, ?> TOMBSTONE = new Slot<>(null, null);

        K key;
        V value;

        Slot(K key, V value) { this.key = key; this.value = value; }

        @SuppressWarnings("unchecked")
        static <K, V> Slot<K, V> tombstone() { return (Slot<K, V>) TOMBSTONE; }

        boolean isTombstone() { return this == TOMBSTONE; }
    }

    private static final double LOAD_FACTOR = 0.6;

    private Slot<K, V>[] table;
    private int size;        // 实际键值对数量（不含墓碑）
    private int used;        // 占用槽数（含墓碑），决定扩容时机
    private Tracer tracer;

    @SuppressWarnings("unchecked")
    public OpenAddressingHashMap() { this(8); }

    @SuppressWarnings("unchecked")
    public OpenAddressingHashMap(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("容量必须为正: " + capacity);
        table = new Slot[capacity];
    }

    /* ---------------- step-mode ---------------- */

    public void attachTracer(Tracer t) { this.tracer = t; }

    public Object snapshot() {
        // 复用 hash 桶视图：每个桶为 0/1 个元素
        List<Object> bs = new ArrayList<>(table.length);
        for (Slot<K, V> s : table) {
            List<Object> chain = new ArrayList<>(1);
            if (s != null && !s.isTombstone()) chain.add(Snaps.bucketEntry(s.key, s.value));
            else if (s != null) chain.add(Snaps.bucketEntry("墓碑", "—"));
            bs.add(chain);
        }
        return Snaps.hash(bs, size, table.length);
    }

    private Tracer.Step begin(String op) {
        if (tracer == null) return Tracer.inert();
        return tracer.step(op).before(snapshot());
    }

    private void commit(Tracer.Step s, String hlKey, Object hlVal) {
        if (tracer == null) return;
        if (hlKey != null) s.hl(hlKey, hlVal);
        s.after(snapshot()).commit();
    }

    /* ---------------- 探测 ---------------- */

    private int firstIndex(K key) {
        return (key.hashCode() ^ (key.hashCode() >>> 16)) & (table.length - 1);
    }

    /** 找 key 所在槽位；未找到返回 -1。跳过墓碑继续探测。 */
    private int probeFor(K key) {
        int i = firstIndex(key);
        while (true) {
            Slot<K, V> s = table[i];
            if (s == null) return -1;                      // 空位：探测链终止，key 一定不存在
            if (!s.isTombstone() && Objects.equals(s.key, key)) return i;
            i = (i + 1) & (table.length - 1);
        }
    }

    /** 找可写入槽位：null 或墓碑；调用方保证有空间。 */
    private int probeForInsert(K key) {
        int i = firstIndex(key);
        while (true) {
            Slot<K, V> s = table[i];
            if (s == null || s.isTombstone()) return i;
            if (Objects.equals(s.key, key)) return i;
            i = (i + 1) & (table.length - 1);
        }
    }

    /* ---------------- 基本操作 ---------------- */

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    public V put(K key, V value) {
        Tracer.Step s = begin("put").arg("key", key).arg("value", value);
        if ((used + 1) > table.length * LOAD_FACTOR) {
            resize();
            if (tracer != null) {
                tracer.step("resize").arg("newCap", table.length).before(snapshot()).after(snapshot()).commit();
            }
        }
        int idx = probeForInsert(key);
        Slot<K, V> cur = table[idx];
        if (cur != null && !cur.isTombstone() && Objects.equals(cur.key, key)) {
            V old = cur.value;
            cur.value = value;
            commit(s.hl("slot", idx).arg("replaced", true), "slot", idx);
            return old;
        }
        table[idx] = new Slot<>(key, value);
        size++;
        used++;
        commit(s.hl("slot", idx), "slot", idx);
        return null;
    }

    public V get(K key) {
        int i = probeFor(key);
        return i < 0 ? null : table[i].value;
    }

    public boolean containsKey(K key) {
        return probeFor(key) >= 0;
    }

    public V remove(K key) {
        Tracer.Step s = begin("remove").arg("key", key);
        int i = probeFor(key);
        if (i < 0) {
            commit(s, null, null);
            throw new NoSuchElementException("键不存在: " + key);
        }
        V old = table[i].value;
        table[i] = Slot.tombstone(); // 墓碑：保持探测链完整
        size--;
        commit(s.hl("slot", i), "slot", i);
        return old;
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        Slot<K, V>[] nt = new Slot[table.length * 2];
        Slot<K, V>[] old = table;
        table = nt;
        used = size; // 墓碑在重建时被清除
        for (Slot<K, V> s : old) {
            if (s == null || s.isTombstone()) continue;
            int i = firstIndex(s.key);
            while (table[i] != null) i = (i + 1) & (table.length - 1);
            table[i] = s;
        }
    }

    public List<K> keys() {
        List<K> out = new ArrayList<>(size);
        for (Slot<K, V> s : table) {
            if (s != null && !s.isTombstone()) out.add(s.key);
        }
        return out;
    }
}
