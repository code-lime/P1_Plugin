package net.minecraft.world;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.UUID;

public record LimeKey(UUID uuid, String type) {
    private static final PersistentDataType<String, LimeKey> DATA_TYPE = new PersistentDataType<>() {
        @Override public Class<String> getPrimitiveType() { return String.class; }
        @Override public Class<LimeKey> getComplexType() { return LimeKey.class; }
        @Override public String toPrimitive(LimeKey var1, PersistentDataAdapterContext var2) { return var1.uuid + " " + var1.type; }
        @Override public LimeKey fromPrimitive(String var1, PersistentDataAdapterContext var2) {
            String[] args = var1.split(" ", 2);
            return new LimeKey(UUID.fromString(args[0]), args[1]);
        }
    };

    public enum KeyType {
        CUSTOM_BLOCK(new NamespacedKey("lime", "custom_block")),
        CUSTOM_ENTITY(new NamespacedKey("lime", "custom_entity"));

        private final NamespacedKey key;

        KeyType(NamespacedKey key) {
            this.key = key;
        }
    }

    public void setKey(PersistentDataContainer container, KeyType type) { container.set(type.key, DATA_TYPE, this); }
    public static void removeKey(PersistentDataContainer container, KeyType type) { container.remove(type.key); }
    public static Optional<LimeKey> getKey(PersistentDataContainer container, KeyType type) { return Optional.ofNullable(container.get(type.key, LimeKey.DATA_TYPE)); }

    public static LimeKey of(String type) { return new LimeKey(UUID.randomUUID(), type); }
}




