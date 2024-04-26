package org.lime.gp.block.component.display.instance;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class UnmodifiableMergeMap<K,V> extends AbstractMap<K,V> {
    private final Map<K,V> first;
    private final Map<K,V> second;

    public UnmodifiableMergeMap(Map<K, V> first, Map<K, V> second) {
        this.first = Objects.requireNonNull(first);
        this.second = Objects.requireNonNull(second);
    }

    public static <K,V>Map<K,V> of(Map<K, V> first, Map<K, V> second) {
        return new UnmodifiableMergeMap<>(first, second);
    }
    public static <K,V>Map<K,V> of(Stream<Map<K, V>> maps) {
        return Collections.unmodifiableMap(maps.reduce(UnmodifiableMergeMap::new).orElseGet(Collections::emptyMap));
    }
    public static <K,V>Map<K,V> of(Map<K, V>... maps) {
        return of(Stream.of(maps));
    }
    public static <K,V>Map<K,V> of(Collection<Map<K, V>> maps) {
        return of(maps.stream());
    }

    // mandatory methods

    final Set<Map.Entry<K, V>> entrySet = new AbstractSet<Map.Entry<K, V>>() {
        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return stream().iterator();
        }

        @Override
        public int size() {
            long size = stream().count();
            return size <= Integer.MAX_VALUE? (int)size: Integer.MAX_VALUE;
        }

        @Override
        public Stream<Entry<K, V>> stream() {
            return Stream.concat(first.entrySet().stream(), secondStream())
            .map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue()));
        }

        @Override
        public Stream<Entry<K, V>> parallelStream() {
            return stream().parallel();
        }

        @Override
        public Spliterator<Entry<K, V>> spliterator() {
            return stream().spliterator();
        }
    };

    Stream<Entry<K, V>> secondStream() {
        return second.entrySet().stream().filter(e -> !first.containsKey(e.getKey()));
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return entrySet;
    }

    // optimizations

    @Override
    public boolean containsKey(Object key) {
        return first.containsKey(key) || second.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return first.containsValue(value) ||
            secondStream().anyMatch(Predicate.isEqual(value));
    }

    @Override
    public V get(Object key) {
        V v = first.get(key);
        return v != null || first.containsKey(key)? v: second.get(key);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        V v = first.get(key);
        return v != null || first.containsKey(key)? v:
            second.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        first.forEach(action);
        second.forEach((k,v) -> { if(!first.containsKey(k)) action.accept(k, v); });
    }
}