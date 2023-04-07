package org.lime.gp.module;

import org.lime.core;
import org.lime.gp.lime;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TimeoutData {
    public static core.element create() {
        return core.element.create(TimeoutData.class)
                .withInit(TimeoutData::init);
    }
    public static void init() {
        lime.repeatTicks(TimeoutData::update, 1);
    }
    public static void update() {
        groupTimeouts.values().forEach(groups -> groups.values().forEach(map -> map.values().removeIf(IRemoveable::isRemove)));
    }

    private static final ConcurrentHashMap<Class<?>, Map<Long, ConcurrentHashMap<UUID, ? extends IRemoveable>>> groupTimeouts = new ConcurrentHashMap<>();
    public static abstract class IRemoveable {
        private int ticks;
        public IRemoveable(int ticks) { this.ticks = ticks; }

        private boolean isRemove() {
            ticks--;
            return ticks <= 0;
        }
    }
    public static abstract class ITimeout extends IRemoveable {
        public ITimeout(int ticks) { super(ticks); }
        public ITimeout() { super(20); }
    }
    public static abstract class IGroupTimeout extends IRemoveable {
        public IGroupTimeout(int ticks) { super(ticks); }
        public IGroupTimeout() { super(20); }
    }
    
    private static final class SingleGroupKey implements TGroup {
        private SingleGroupKey() {}

        public static final TGroup INSTANCE = new SingleGroupKey();

        @Override public long groupID() { return 0; }
    }
    
    public static interface TGroup {
        long groupID();
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends IRemoveable>Map<UUID, T> ofClass(TGroup group, Class<T> tClass) {
        return (ConcurrentHashMap<UUID, T>)groupTimeouts
            .computeIfAbsent(tClass, v ->  new ConcurrentHashMap<Long, ConcurrentHashMap<UUID, ? extends IRemoveable>>()/*Collections.synchronizedMap(new Long2ObjectOpenHashMap<ConcurrentHashMap<UUID, ? extends IRemoveable>>())*/)
            .computeIfAbsent(group.groupID(), v -> new ConcurrentHashMap<UUID, T>());
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends IRemoveable> Stream<T> allValues(Class<T> tClass) {
        Map<Long, ConcurrentHashMap<UUID, ? extends IRemoveable>> data = groupTimeouts.get(tClass);
        if (data == null) return Stream.empty();
        return data.values().stream().flatMap(v -> v.values().stream()).map(v -> (T)v);
    }
    
    private static <T extends IRemoveable>boolean _put(TGroup group, UUID uuid, Class<T> tClass, T timeout) {
        Map<UUID, T> timeouts = ofClass(group, tClass);
        return timeout == null
                ? timeouts.remove(uuid) != null
                : timeouts.put(uuid, timeout) == null;
    }
    private static <T extends IRemoveable>Optional<T> _get(TGroup group, UUID uuid, Class<T> tClass) {
        return Optional.ofNullable(ofClass(group, tClass).get(uuid));
    }
    private static <T extends IRemoveable>boolean _has(TGroup group, UUID uuid, Class<T> tClass) {
        return ofClass(group, tClass).containsKey(uuid);
    }
    private static void _remove(TGroup group, UUID uuid, Class<? extends IRemoveable> tClass) {
        ofClass(group, tClass).remove(uuid);
    }
    private static <T extends IRemoveable> Map<UUID, T> _map(TGroup group, Class<T> tClass) {
        return _stream(group, tClass).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    private static <T extends IRemoveable> Stream<Map.Entry<UUID, T>> _stream(TGroup group, Class<T> tClass) {
        return ofClass(group, tClass).entrySet().stream();
    }
    private static <T extends IRemoveable> Stream<UUID> _keys(TGroup group, Class<T> tClass) {
        return ofClass(group, tClass).keySet().stream();
    }
    private static <T extends IRemoveable> Stream<T> _values(TGroup group, Class<T> tClass) {
        return ofClass(group, tClass).values().stream();
    }

    public static <T extends IGroupTimeout>boolean put(TGroup group, UUID uuid, Class<T> tClass, T timeout) {
        return _put(group, uuid, tClass, timeout);
    }
    public static <T extends IGroupTimeout>Optional<T> get(TGroup group, UUID uuid, Class<T> tClass) {
        return _get(group, uuid, tClass);
    }
    public static <T extends IGroupTimeout>boolean has(TGroup group, UUID uuid, Class<T> tClass) {
        return _has(group, uuid, tClass);
    }
    public static void remove(TGroup group, UUID uuid, Class<? extends IGroupTimeout> tClass) {
        _remove(group, uuid, tClass);
    }
    public static <T extends IGroupTimeout> Map<UUID, T> map(TGroup group, Class<T> tClass) {
        return _map(group, tClass);
    }
    public static <T extends IGroupTimeout> Stream<Map.Entry<UUID, T>> stream(TGroup group, Class<T> tClass) {
        return _stream(group, tClass);
    }
    public static <T extends IGroupTimeout> Stream<UUID> keys(TGroup group, Class<T> tClass) {
        return _keys(group, tClass);
    }
    public static <T extends IGroupTimeout> Stream<T> values(TGroup group, Class<T> tClass) {
        return _values(group, tClass);
    }


    public static <T extends ITimeout>boolean put(UUID uuid, Class<T> tClass, T timeout) {
        return _put(SingleGroupKey.INSTANCE, uuid, tClass, timeout);
    }
    public static <T extends ITimeout>Optional<T> get(UUID uuid, Class<T> tClass) {
        return _get(SingleGroupKey.INSTANCE, uuid, tClass);
    }
    public static <T extends ITimeout>boolean has(UUID uuid, Class<T> tClass) {
        return _has(SingleGroupKey.INSTANCE, uuid, tClass);
    }
    public static void remove(UUID uuid, Class<? extends ITimeout> tClass) {
        _remove(SingleGroupKey.INSTANCE, uuid, tClass);
    }
    public static <T extends ITimeout> Map<UUID, T> map(Class<T> tClass) {
        return _map(SingleGroupKey.INSTANCE, tClass);
    }
    public static <T extends ITimeout> Stream<Map.Entry<UUID, T>> stream(Class<T> tClass) {
        return _stream(SingleGroupKey.INSTANCE, tClass);
    }
    public static <T extends ITimeout> Stream<UUID> keys(Class<T> tClass) {
        return _keys(SingleGroupKey.INSTANCE, tClass);
    }
    public static <T extends ITimeout> Stream<T> values(Class<T> tClass) {
        return _values(SingleGroupKey.INSTANCE, tClass);
    }

    /*
    public static void update() {
        timeouts.values().removeIf(ITimeout::isRemove);
    }

    private static final ConcurrentHashMap<TimeoutKey, ITimeout> timeouts = new ConcurrentHashMap<>();
    public static abstract class ITimeout {
        private int ticks;
        public ITimeout(int ticks) { this.ticks = ticks; }
        public ITimeout() { this(20); }

        private boolean isRemove() {
            ticks--;
            return ticks <= 0;
        }
    }
    private static final class TimeoutKey {
        private final UUID first_uuid;
        private final Class<?> tClass;

        public TimeoutKey(UUID uuid, Class<?> tClass) {
            this.first_uuid = uuid;
            this.tClass = tClass;
        }

        @Override public int hashCode() { return Objects.hash(first_uuid, tClass); }
        @Override public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof TimeoutKey _obj)) return false;
            return Objects.equals(first_uuid, _obj.first_uuid)
                    && Objects.equals(tClass, _obj.tClass);
        }
    }

    public static <T extends ITimeout>boolean put(UUID uuid, Class<T> tClass, T timeout) {
        if (timeout == null) return timeouts.remove(new TimeoutKey(uuid, tClass)) != null;
        else return timeouts.put(new TimeoutKey(uuid, tClass), timeout) == null;
    }
    public static <T extends ITimeout>Optional<T> get(UUID uuid, Class<T> tClass) {
        return Optional.ofNullable(timeouts.get(new TimeoutKey(uuid, tClass)))
                .filter(tClass::isInstance)
                .map(v -> (T)v);
    }
    public static void remove(UUID uuid, Class<? extends ITimeout> tClass) {
        timeouts.remove(new TimeoutKey(uuid, tClass));
    }
    public static <T extends ITimeout> Map<UUID, T> map(Class<T> tClass) {
        return stream(tClass).collect(Collectors.toMap(kv -> kv.val0, kv -> (T)kv.val1));
    }
    public static <T extends ITimeout> Stream<system.Toast2<UUID, T>> stream(Class<T> tClass) {
        return timeouts.entrySet()
                .stream()
                .filter(kv -> tClass.isInstance(kv.getValue()))
                .map(kv -> system.toast(kv.getKey().first_uuid, (T)kv.getValue()));
    }
    */
}





































