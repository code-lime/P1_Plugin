package org.lime.gp.module;

import net.minecraft.server.MinecraftServer;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TimeoutData {
    public static CoreElement create() {
        return CoreElement.create(TimeoutData.class)
                .withInit(TimeoutData::init)
                .withUninit(TimeoutData::uninit);
    }
    private static int lastMinecraftTick = 0;

    private static final List<Thread> threads = new ArrayList<>();
    public static void init() {
        lastMinecraftTick = MinecraftServer.currentTick;

        threads.add(new Thread(TimeoutData::threadUpdate));
        threads.forEach(Thread::start);
    }
    public static void threadUpdate() {
        while (true) {
            try { Thread.sleep(50); }
            catch (InterruptedException e) { throw new RuntimeException(e); }
            try {
                update();
            } catch (Throwable e) {
                lime.logStackTrace(e);
            }
        }
    }
    @SuppressWarnings("deprecation")
    public static void uninit() {
        threads.forEach(Thread::stop);
    }
    public static void update() {
        int currentMinecraftTick = MinecraftServer.currentTick;
        int deltaTicks = currentMinecraftTick - lastMinecraftTick;
        lastMinecraftTick = currentMinecraftTick;
        groupTimeouts.values().forEach(groups -> groups.values().forEach(map -> map.values().removeIf(v -> v.tickRemove(deltaTicks))));
    }

    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Object, ConcurrentHashMap<UUID, ? extends ITicksRemovable>>> groupTimeouts = new ConcurrentHashMap<>();
    public static abstract class ITicksRemovable {
        private int ticks;
        public ITicksRemovable(int ticks) { this.ticks = ticks; }

        public final boolean tickRemove(int deltaTick) {
            ticks -= deltaTick;
            return ticks <= 0;
        }
    }
    public static abstract class ITimeout extends ITicksRemovable {
        public ITimeout(int ticks) { super(ticks); }
        public ITimeout() { super(20); }
    }
    public static abstract class IGroupTimeout extends ITicksRemovable {
        public IGroupTimeout(int ticks) { super(ticks); }
        public IGroupTimeout() { super(20); }
    }
    
    private static final class SingleGroupKey implements TKeyedGroup<Long> {
        private SingleGroupKey() {}

        public static final TKeyedGroup<?> INSTANCE = new SingleGroupKey();

        @Override public Long groupID() { return 0L; }
    }

    public interface TKeyedGroup<T> {
        T groupID();
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends ITicksRemovable>Map<UUID, T> ofClass(TKeyedGroup<?> group, Class<T> tClass) {
        return (ConcurrentHashMap<UUID, T>)groupTimeouts
            .computeIfAbsent(tClass, v -> new ConcurrentHashMap<>())
            .computeIfAbsent(group.groupID(), v -> new ConcurrentHashMap<UUID, T>());
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends ITicksRemovable> Stream<T> allValues(Class<T> tClass) {
        Map<Object, ConcurrentHashMap<UUID, ? extends ITicksRemovable>> data = groupTimeouts.get(tClass);
        if (data == null) return Stream.empty();
        return data.values().stream().flatMap(v -> v.values().stream()).map(v -> (T)v);
    }
    
    private static <T extends ITicksRemovable>boolean _put(TKeyedGroup<?> group, UUID uuid, Class<T> tClass, T timeout) {
        Map<UUID, T> timeouts = ofClass(group, tClass);
        return timeout == null
                ? timeouts.remove(uuid) != null
                : timeouts.put(uuid, timeout) == null;
    }
    private static <T extends ITicksRemovable>Optional<T> _get(TKeyedGroup<?> group, UUID uuid, Class<T> tClass) {
        return Optional.ofNullable(ofClass(group, tClass).get(uuid));
    }
    private static <T extends ITicksRemovable>boolean _has(TKeyedGroup<?> group, UUID uuid, Class<T> tClass) {
        return ofClass(group, tClass).containsKey(uuid);
    }
    private static void _remove(TKeyedGroup<?> group, UUID uuid, Class<? extends ITicksRemovable> tClass) {
        ofClass(group, tClass).remove(uuid);
    }
    private static <T extends ITicksRemovable> Map<UUID, T> _map(TKeyedGroup<?> group, Class<T> tClass) {
        return _stream(group, tClass).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    private static <T extends ITicksRemovable> Stream<Map.Entry<UUID, T>> _stream(TKeyedGroup<?> group, Class<T> tClass) {
        return ofClass(group, tClass).entrySet().stream();
    }
    private static <T extends ITicksRemovable> Stream<UUID> _keys(TKeyedGroup<?> group, Class<T> tClass) {
        return ofClass(group, tClass).keySet().stream();
    }
    private static <T extends ITicksRemovable> Stream<T> _values(TKeyedGroup<?> group, Class<T> tClass) {
        return ofClass(group, tClass).values().stream();
    }
    private static <T extends ITicksRemovable> int _count(TKeyedGroup<?> group, Class<T> tClass) {
        return ofClass(group, tClass).values().size();
    }

    public static <T extends IGroupTimeout>boolean put(TKeyedGroup<?> group, UUID uuid, Class<T> tClass, T timeout) {
        return _put(group, uuid, tClass, timeout);
    }
    public static <T extends IGroupTimeout>Optional<T> get(TKeyedGroup<?> group, UUID uuid, Class<T> tClass) {
        return _get(group, uuid, tClass);
    }
    public static <T extends IGroupTimeout>boolean has(TKeyedGroup<?> group, UUID uuid, Class<T> tClass) {
        return _has(group, uuid, tClass);
    }
    public static void remove(TKeyedGroup<?> group, UUID uuid, Class<? extends IGroupTimeout> tClass) {
        _remove(group, uuid, tClass);
    }
    public static <T extends IGroupTimeout> Map<UUID, T> map(TKeyedGroup<?> group, Class<T> tClass) {
        return _map(group, tClass);
    }
    public static <T extends IGroupTimeout> Stream<Map.Entry<UUID, T>> stream(TKeyedGroup<?> group, Class<T> tClass) {
        return _stream(group, tClass);
    }
    public static <T extends IGroupTimeout> Stream<UUID> keys(TKeyedGroup<?> group, Class<T> tClass) {
        return _keys(group, tClass);
    }
    public static <T extends IGroupTimeout> Stream<T> values(TKeyedGroup<?> group, Class<T> tClass) {
        return _values(group, tClass);
    }
    public static <T extends IGroupTimeout> int count(TKeyedGroup<?> group, Class<T> tClass) {
        return _count(group, tClass);
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
    public static <T extends ITimeout> int count(Class<T> tClass) {
        return _count(SingleGroupKey.INSTANCE, tClass);
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
    public static <T extends ITimeout> Stream<Toast2<UUID, T>> stream(Class<T> tClass) {
        return timeouts.entrySet()
                .stream()
                .filter(kv -> tClass.isInstance(kv.getValue()))
                .map(kv -> Toast.of(kv.getKey().first_uuid, (T)kv.getValue()));
    }
    */
}





































