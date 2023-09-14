package org.lime.gp.module.mobs;

import com.google.gson.JsonObject;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.lime.gp.lime;
import org.lime.system.range.IRange;

import java.util.Optional;

public record DespawnData(IRange despawnSec, Optional<Double> deltaHealth, boolean isOnlyLight) {
    private static final NamespacedKey DESPAWN_SEC = new NamespacedKey(lime._plugin, "despawn_sec");
    private static final NamespacedKey DELTA_HEALTH = new NamespacedKey(lime._plugin, "delta_health");
    private static final NamespacedKey ONLY_LIGHT = new NamespacedKey(lime._plugin, "only_light");

    public static Optional<Integer> tickSecond(PersistentDataContainer container) {
        Integer sec = container.get(DESPAWN_SEC, PersistentDataType.INTEGER);
        if (sec == null) return Optional.empty();
        sec--;
        if (sec < 0) sec = 0;
        container.set(DESPAWN_SEC, PersistentDataType.INTEGER, sec);
        return Optional.of(sec);
    }
    public static Optional<Double> getDeltaHealth(PersistentDataContainer container) {
        return Optional.ofNullable(container.get(DELTA_HEALTH, PersistentDataType.DOUBLE));
    }
    public static boolean isOnlyLight(PersistentDataContainer container) {
        return container.getOrDefault(ONLY_LIGHT, PersistentDataType.BYTE, (byte)1) == 1;
    }

    public static void setupData(PersistentDataContainer container, int despawnSec, Double deltaHealth, boolean isOnlyLight) {
        container.set(DESPAWN_SEC, PersistentDataType.INTEGER, despawnSec);
        if (deltaHealth != null) container.set(DELTA_HEALTH, PersistentDataType.DOUBLE, deltaHealth);
        container.set(ONLY_LIGHT, PersistentDataType.BYTE, (byte)(isOnlyLight ? 1 : 0));
    }

    public static DespawnData parse(JsonObject json) {
        return new DespawnData(
                IRange.parse(json.get("sec").getAsString()),
                json.has("delta_health") ? Optional.of(json.get("delta_health").getAsDouble()) : Optional.empty(),
                !json.has("only_light") || json.get("only_light").getAsBoolean()
        );
    }

    public void setupData(PersistentDataContainer container) {
        setupData(container, despawnSec.getIntValue(200), deltaHealth.orElse(null), isOnlyLight);
    }
}














