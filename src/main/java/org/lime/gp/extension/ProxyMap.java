package org.lime.gp.extension;

import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.*;

public class ProxyMap<K, V> implements Map<K, V> {
    private final Map<K, V> base;
    private final LockToast1<Boolean> dirty = Toast.lock(false);

    private ProxyMap(Map<K,V> base) { this.base = base; }
    public static <K,V>ProxyMap<K,V> of(Map<K,V> base) { return new ProxyMap<>(base); }

    @Override public int size() { return base.size(); }
    @Override public boolean isEmpty() { return base.isEmpty(); }
    @Override public boolean containsKey(Object key) { return base.containsKey(key); }
    @Override public boolean containsValue(Object value) { return base.containsValue(value); }
    @Override public V get(Object key) { return base.get(key); }

    @Nullable @Override public V put(K key, V value) {
        V ret = base.put(key, value);
        if (!Objects.equals(ret, value)) dirty.set0(true);
        return ret;
    }
    @Override public V remove(Object key) {
        V ret = base.remove(key);
        if (ret != null) dirty.set0(true);
        return ret;
    }
    @Override public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        base.putAll(m);
        dirty.set0(true);
    }
    @Override public void clear() {
        base.clear();
        dirty.set0(true);
    }

    @Deprecated @NotNull @Override public Set<K> keySet() throws UnsupportedOperationException { return Collections.emptySet(); /*throw new UnsupportedOperationException();*/ }
    @Deprecated @NotNull @Override public Collection<V> values() throws UnsupportedOperationException { return Collections.emptySet(); /*throw new UnsupportedOperationException();*/ }
    @Deprecated @NotNull @Override public Set<Entry<K, V>> entrySet() throws UnsupportedOperationException { throw new UnsupportedOperationException(); }

    @Override public boolean equals(Object o) { return base.equals(o); }
    @Override public int hashCode() { return base.hashCode(); }

    public boolean checkDirty(boolean reset) {
        return dirty.call(v -> {
            if (!v.val0) return false;
            if (reset) v.val0 = false;
            return true;
        });
    }
}
