package com.javadslab.core.hash;

import com.javadslab.trace.Snaps;
import com.javadslab.trace.Tracer;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * 手写哈希表——拉链法（Separate Chaining）。
 * 核心：hash = (key.hashCode() ^ unsigned shift) & (cap-1)；负载因子 > 0.75 扩容为 2 倍并重新拉链。
 * step-mode：put/remove/resize 记录 trace（快照展示每个桶的链）。
 */
public class ChainingHashMap<K, V> {

    private static final class Entry<K, V> {
        K key;
        V value;
        Entry<K, V> next;

        Entry(K key, V value, Entry<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private static final double LOAD_FACTOR = 0.75;

    private Entry<K, V>[] buckets;
    private int size;
    private Tracer tracer;

    @SuppressWarnings("unchecked")
    public ChainingHashMap() { this(8); }

    @SuppressWarnings("unchecked")
    public ChainingHashMap(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("容量必须为正: " + capacity);
        buckets = new ChainingHashMap.Entry[capacity];
    }

    /* ---------------- step-mode ---------------- */

    public void attachTracer(Tracer t) { this.tracer = t; }

    public Object snapshot() {
        List<Object> bs = new ArrayList<>(buckets.length);
        for (Entry<K, V> b : buckets) {
            List<Object> chain = new ArrayList<>();
            for (Entry<K, V> e = b; e != null; e = e.next) chain.add(Snaps.bucketEntry(e.key, e.value));
            bs.add(chain);
        }
        return Snaps.hash(bs, size, buckets.length);
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

    /* ---------------- 哈希与定位 ---------------- */

    private int indexOf(K key) {
        return spread(key.hashCode()) & (buckets.length - 1);
    }

    /** 扰动：高低位异或，减少低位聚集。容量恒为 2 的幂。 */
    private static int spread(int h) {
        return h ^ (h >>> 16);
    }

    /* ---------------- 基本操作 ---------------- */

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    /** 放入键值；旧值存在则覆盖并返回旧值。 */
    public V put(K key, V value) {
        Tracer.Step s = begin("put").arg("key", key).arg("value", value);
        if (size + 1 > buckets.length * LOAD_FACTOR) {
            resize();
            if (tracer != null) {
                tracer.step("resize").arg("newCap", buckets.length)
                        .before(snapshot()).after(snapshot()).commit();
            }
        }
        int idx = indexOf(key);
        for (Entry<K, V> e = buckets[idx]; e != null; e = e.next) {
            if (Objects.equals(e.key, key)) {
                V old = e.value;
                e.value = value;
                commit(s.hl("bucket", idx).arg("replaced", true), "bucket", idx);
                return old;
            }
        }
        // 头插法：新节点插到桶链头部
        buckets[idx] = new Entry<>(key, value, buckets[idx]);
        size++;
        commit(s.hl("bucket", idx), "bucket", idx);
        return null;
    }

    public V get(K key) {
        int idx = indexOf(key);
        for (Entry<K, V> e = buckets[idx]; e != null; e = e.next) {
            if (Objects.equals(e.key, key)) return e.value;
        }
        return null;
    }

    public boolean containsKey(K key) {
        return nodeOf(key) != null;
    }

    private Entry<K, V> nodeOf(K key) {
        int idx = indexOf(key);
        for (Entry<K, V> e = buckets[idx]; e != null; e = e.next) {
            if (Objects.equals(e.key, key)) return e;
        }
        return null;
    }

    /** 删除键；键不存在抛 NoSuchElementException，教学上更明确。 */
    public V remove(K key) {
        Tracer.Step s = begin("remove").arg("key", key);
        int idx = indexOf(key);
        Entry<K, V> prev = null;
        for (Entry<K, V> e = buckets[idx]; e != null; prev = e, e = e.next) {
            if (Objects.equals(e.key, key)) {
                if (prev == null) buckets[idx] = e.next;
                else prev.next = e.next;
                size--;
                commit(s.hl("bucket", idx), "bucket", idx);
                return e.value;
            }
        }
        commit(s, null, null);
        throw new NoSuchElementException("键不存在: " + key);
    }

    /** 扩容：2 倍容量，所有节点按新下标重新拉链。 */
    @SuppressWarnings("unchecked")
    private void resize() {
        Entry<K, V>[] nb = new Entry[buckets.length * 2];
        for (Entry<K, V> head : buckets) {
            for (Entry<K, V> e = head; e != null; ) {
                Entry<K, V> nxt = e.next;
                int idx = spread(e.key.hashCode()) & (nb.length - 1);
                e.next = nb[idx];
                nb[idx] = e;
                e = nxt;
            }
        }
        buckets = nb;
    }

    public List<K> keys() {
        List<K> out = new ArrayList<>(size);
        for (Entry<K, V> head : buckets)
            for (Entry<K, V> e = head; e != null; e = e.next) out.add(e.key);
        return out;
    }
}
