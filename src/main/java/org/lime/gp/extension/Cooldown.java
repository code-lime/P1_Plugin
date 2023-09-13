package org.lime.gp.extension;

import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.lime;
import org.lime.system;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Cooldown {
    public static CoreElement create() {
        return CoreElement.create(Cooldown.class)
                .withInit(Cooldown::init);
    }
    public static void init() {
        lime.repeat(() -> cooldowns.values().removeIf(value -> value <= System.currentTimeMillis()), 5);
    }
    private static final ConcurrentHashMap<String, Long> cooldowns = new ConcurrentHashMap<>();
    private static String createKey(UUID uuid, String key) {
        return createKey(new UUID[]{uuid}, key);
    }
    private static String createKey(UUID[] uuid, String key) {
        return uuid.length + ":" + Arrays.stream(uuid).map(v -> v == null ? "NULL" : v.toString()).collect(Collectors.joining(":")) + ":" + key;
    }

    public static void setCooldown(UUID uuid, String key, double sec) {
        cooldowns.put(createKey(uuid, key), (long)(System.currentTimeMillis() + sec * 1000));
    }
    public static void resetCooldown(UUID uuid, String key) {
        cooldowns.remove(createKey(uuid, key));
    }
    public static boolean hasCooldown(UUID uuid, String key) {
        return cooldowns.getOrDefault(createKey(uuid, key), 0L) > System.currentTimeMillis();
    }
    public static double getCooldown(UUID uuid, String key) {
        Long cooldown = cooldowns.getOrDefault(createKey(uuid, key), null);
        if (cooldown == null) return 0;
        double sec = (cooldown - System.currentTimeMillis()) / 1000.0;
        if (sec <= 0) return 0;
        return sec;
    }
    public static Optional<Integer> getOptionalCooldown(UUID uuid, String key) {
        Long cooldown = cooldowns.getOrDefault(createKey(uuid, key), null);
        if (cooldown == null) return Optional.empty();
        cooldown -= System.currentTimeMillis();
        cooldown /= 1000;
        if (cooldown <= 0) return Optional.empty();
        return Optional.of((int)(long)cooldown);
    }
    public static boolean hasOrSetCooldown(UUID uuid, String key, double sec) {
        system.Toast1<Boolean> result = system.toast(false);
        cooldowns.compute(createKey(uuid, key), (_k,_v) -> {
            long curr = System.currentTimeMillis();
            if (_v != null && _v > curr) {
                result.val0 = true;
                return _v;
            }
            return (long)(curr + sec * 1000);
        });
        return result.val0;
    }

    public static void setCooldown(UUID[] uuid, String key, double sec) {
        cooldowns.put(createKey(uuid, key), (long)(System.currentTimeMillis() + sec * 1000));
    }
    public static void resetCooldown(UUID[] uuid, String key) {
        cooldowns.remove(createKey(uuid, key));
    }
    public static boolean hasCooldown(UUID[] uuid, String key) {
        return cooldowns.getOrDefault(createKey(uuid, key), 0L) > System.currentTimeMillis();
    }
    public static int getCooldown(UUID[] uuid, String key) {
        Long cooldown = cooldowns.getOrDefault(createKey(uuid, key), null);
        if (cooldown == null) return 0;
        cooldown -= System.currentTimeMillis();
        cooldown /= 1000;
        if (cooldown <= 0) return 0;
        return (int)(long)cooldown;
    }
    public static boolean hasOrSetCooldown(UUID[] uuid, String key, double sec) {
        system.Toast1<Boolean> result = system.toast(false);
        cooldowns.compute(createKey(uuid, key), (_k,_v) -> {
            long curr = System.currentTimeMillis();
            if (_v != null && _v > curr) {
                result.val0 = true;
                return _v;
            }
            return (long)(curr + sec * 1000);
        });
        return result.val0;
    }
}
