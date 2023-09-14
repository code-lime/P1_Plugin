package org.lime.gp.extension;

import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MapUUID {
    private static final ConcurrentHashMap<IToast, UUID> mapToUuid = new ConcurrentHashMap<>();

    public static UUID of(String key, UUID first, UUID second) {
        return mapToUuid.computeIfAbsent(Toast.of(key, first, second), v -> UUID.randomUUID());
    }
    public static UUID of(String key, UUID first) {
        return of(key, first, null);
    }
}
