package org.lime.gp.module.biome;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.HashMap;

class ProxyObject2IntOpenHashMap<T> extends Object2IntOpenHashMap<T> {
    public final HashMap<T, Integer> append = new HashMap<>();

    public ProxyObject2IntOpenHashMap(Object2IntMap<T> map) {
        super(map);
        defaultReturnValue(map.defaultReturnValue());
    }

    @Override
    public int getInt(Object k) {
        Integer ret = append.get(k);
        return ret == null ? super.getInt(k) : ret;
    }

    public static <T> ProxyObject2IntOpenHashMap<T> proxy(Object2IntMap<T> map) {
        return new ProxyObject2IntOpenHashMap<>(map);
    }
}
