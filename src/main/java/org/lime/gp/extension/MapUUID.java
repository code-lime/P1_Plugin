package org.lime.gp.extension;

import org.lime.system;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MapUUID {
    private static final ConcurrentHashMap<system.IToast, UUID> mapToUuid = new ConcurrentHashMap<>();

    public static UUID of(String key, UUID first, UUID second) {
        return mapToUuid.computeIfAbsent(system.toast(key, first, second), v -> UUID.randomUUID());
    }
    public static UUID of(String key, UUID first) {
        return of(key, first, null);
    }
}
