package org.lime.gp.module;

import org.lime.core;
import org.lime.gp.lime;
import org.lime.reflection;
import org.lime.system;

import javax.annotation.Nullable;
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
        timeouts.values().forEach(map -> map.values().removeIf(ITimeout::isRemove));
    }

    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<UUID, ? extends ITimeout>> timeouts = new ConcurrentHashMap<>();
    public static abstract class ITimeout {
        private int ticks;
        public ITimeout(int ticks) { this.ticks = ticks; }
        public ITimeout() { this(20); }

        private boolean isRemove() {
            ticks--;
            return ticks <= 0;
        }
    }

    private static  <T extends ITimeout>ConcurrentHashMap<UUID, T> ofClass(Class<T> tClass) {
        return (ConcurrentHashMap<UUID, T>)timeouts.computeIfAbsent(tClass, v -> new ConcurrentHashMap<UUID, T>());
    }

    public static <T extends ITimeout>boolean put(UUID uuid, Class<T> tClass, T timeout) {
        ConcurrentHashMap<UUID, T> timeouts = ofClass(tClass);
        return timeout == null
                ? timeouts.remove(uuid) != null
                : timeouts.put(uuid, timeout) == null;
    }
    public static <T extends ITimeout>Optional<T> get(UUID uuid, Class<T> tClass) {
        return Optional.ofNullable(ofClass(tClass).get(uuid));
    }
    public static <T extends ITimeout>boolean has(UUID uuid, Class<T> tClass) {
        ConcurrentHashMap<UUID, T> map = ofClass(tClass);
        return map != null && map.containsKey(uuid);
    }
    public static void remove(UUID uuid, Class<? extends ITimeout> tClass) {
        ofClass(tClass).remove(uuid);
    }
    public static <T extends ITimeout> Map<UUID, T> map(Class<T> tClass) {
        return stream(tClass).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    public static <T extends ITimeout> Stream<Map.Entry<UUID, T>> stream(Class<T> tClass) {
        return ofClass(tClass).entrySet().stream();
    }
    public static <T extends ITimeout> Stream<UUID> keys(Class<T> tClass) {
        return ofClass(tClass).keySet().stream();
    }
    public static <T extends ITimeout> Stream<T> values(Class<T> tClass) {
        return ofClass(tClass).values().stream();
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





































