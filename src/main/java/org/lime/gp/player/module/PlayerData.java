package org.lime.gp.player.module;

import com.google.common.collect.Streams;
import com.google.common.primitives.Primitives;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang.Validate;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.lime;
import org.lime.system;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PlayerData {
    public static CoreElement create() {
        return CoreElement.create(PlayerData.class)
                .withInit(PlayerData::init)
                .withUninit(PlayerData::uninit);
    }
    public static void init() {
        File folder = lime.getConfigFile("players");
        if (!folder.exists()) folder.mkdir();
        lime.repeat(PlayerData::tick, 5);
    }
    public static void tick() {
        cache.forEach((uuid, data) -> {
            if (data.dirty()) {
                lime.writeAllConfig(playerData(uuid), data.toJson().toString());
                data.dirty(false);
            }
        });
    }
    public static void uninit() {
        tick();
    }

    public static void clearPlayerData(UUID uuid) {
        cache.remove(uuid);
        lime.writeAllConfig(playerData(uuid), "{}");
    }

    private static String playerData(UUID uuid) { return "players/"+uuid; }
    private static final HashMap<UUID, DirtyJsonPersistentDataContainer> cache = new HashMap<>();

    public static JsonPersistentDataContainer getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }
    public static JsonPersistentDataContainer getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, _uuid -> {
            String playerData = playerData(_uuid);
            return lime.existConfig(playerData)
                    ? new DirtyJsonPersistentDataContainer(system.json.parse(lime.readAllConfig(playerData)).getAsJsonObject())
                    : new DirtyJsonPersistentDataContainer();
        });
    }

    private static final class DirtyJsonPersistentDataContainer extends JsonPersistentDataContainer {
        private boolean dirty;

        public DirtyJsonPersistentDataContainer() { }
        @SuppressWarnings("unused")
        public DirtyJsonPersistentDataContainer(Map<NamespacedKey, JsonElement> customTags) { super(customTags); }
        public DirtyJsonPersistentDataContainer(JsonObject json) { super(json); }

        public boolean dirty() { return this.dirty; }
        public void dirty(boolean dirty) { this.dirty = dirty; }
        @Override public <T, Z> void set(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
            super.set(key, type, value);
            this.dirty(true);
        }
        @Override public void remove(NamespacedKey key) {
            super.remove(key);
            this.dirty(true);
        }
        @Override public void setJson(NamespacedKey key, JsonElement value) {
            super.setJson(key, value);
            this.dirty(true);
        }
    }

    public static class JsonPersistentDataContainer implements PersistentDataContainer {
        private static final PersistentDataAdapterContext STATIC_ADAPTER = JsonPersistentDataContainer::new;
        private static final JsonPersistentDataTypeRegistry STATIC_REGISTRY = new JsonPersistentDataTypeRegistry();
        private final Map<NamespacedKey, JsonElement> customDataTags = new HashMap<>();

        public JsonPersistentDataContainer() { }
        public JsonPersistentDataContainer(Map<NamespacedKey, JsonElement> customTags) { this.customDataTags.putAll(customTags); }
        public JsonPersistentDataContainer(JsonObject json) { this(fromJson(json)); }
        public static Map<NamespacedKey, JsonElement> fromJson(JsonObject json) { return json.entrySet().stream().collect(Collectors.toMap(kv -> NamespacedKey.fromString(kv.getKey(), lime._plugin), Map.Entry::getValue)); }

        @Override public <T, Z> void set(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
            Validate.notNull(key, "The provided key for the custom value was null");
            Validate.notNull(type, "The provided type for the custom value was null");
            Validate.notNull(value, "The provided value for the custom value was null");
            this.customDataTags.put(key, STATIC_REGISTRY.wrap(type.getPrimitiveType(), type.toPrimitive(value, STATIC_ADAPTER)));
        }
        @Override public <T, Z> boolean has(NamespacedKey key, PersistentDataType<T, Z> type) {
            Validate.notNull(key, "The provided key for the custom value was null");
            Validate.notNull(type, "The provided type for the custom value was null");
            JsonElement value = this.customDataTags.get(key);
            if (value == null) return false;
            return STATIC_REGISTRY.isInstanceOf(type.getPrimitiveType(), value);
        }
        @Override public <T, Z> Z get(NamespacedKey key, PersistentDataType<T, Z> type) {
            Validate.notNull(key, "The provided key for the custom value was null");
            Validate.notNull(type, "The provided type for the custom value was null");
            JsonElement value = this.customDataTags.get(key);
            if (value == null) return null;
            return type.fromPrimitive(STATIC_REGISTRY.extract(type.getPrimitiveType(), value), STATIC_ADAPTER);
        }
        @Override public <T, Z> Z getOrDefault(NamespacedKey key, PersistentDataType<T, Z> type, Z defaultValue) {
            Z z2 = this.get(key, type);
            return z2 != null ? z2 : defaultValue;
        }

        @Override public Set<NamespacedKey> getKeys() {
            return new HashSet<>(this.customDataTags.keySet());
        }
        @Override public void remove(NamespacedKey key) {
            Validate.notNull(key, "The provided key for the custom value was null");
            this.customDataTags.remove(key);
        }
        @Override public boolean isEmpty() { return this.customDataTags.isEmpty(); }
        @Override public PersistentDataAdapterContext getAdapterContext() { return STATIC_ADAPTER; }

        public JsonElement getJson(NamespacedKey key) { return customDataTags.get(key); }
        public void setJson(NamespacedKey key, JsonElement value) { customDataTags.put(key, value); }

        public boolean equals(Object obj) {
            if (!(obj instanceof JsonPersistentDataContainer _obj)) return false;
            return Objects.equals(this.customDataTags, _obj.customDataTags);
        }
        public JsonObject toJson() { return system.json.object().add(this.customDataTags, NamespacedKey::toString, v -> v).build(); }
        public int hashCode() {
            int hashCode = 3;
            return hashCode += this.customDataTags.hashCode();
        }

        @Override public boolean has(NamespacedKey key) {
            Validate.notNull(key, "The provided key for the custom value was null");
            return this.customDataTags.containsKey(key);
        }
        @Override public void readFromBytes(byte[] bytes, boolean clear) throws IOException {
            if (clear) this.customDataTags.clear();
            fromJson(system.json.parse(new String(bytes)).getAsJsonObject()).forEach(this::setJson);
        }

        @Override public byte[] serializeToBytes() throws IOException {
            return toJson().toString().getBytes();
        }
    }
    private static final class JsonPersistentDataTypeRegistry {
        private final Function<Class<?>, TagAdapter<?,?>> CREATE_ADAPTER = this::createAdapter;
        private final Map<Class<?>, TagAdapter<?,?>> adapters = new HashMap<>();

        private <T>TagAdapter<?,?> createAdapter(Class<T> type) {
            if (!Primitives.isWrapperType(type)) type = Primitives.wrap(type);
            if (Objects.equals(Byte.class, type)) return this.createAdapter(Byte.class, JsonPrimitive.class, JsonPrimitive::new, JsonPrimitive::getAsByte);
            if (Objects.equals(Short.class, type)) return this.createAdapter(Short.class, JsonPrimitive.class, JsonPrimitive::new, JsonPrimitive::getAsShort);
            if (Objects.equals(Integer.class, type)) return this.createAdapter(Integer.class, JsonPrimitive.class, JsonPrimitive::new, JsonPrimitive::getAsInt);
            if (Objects.equals(Long.class, type)) return this.createAdapter(Long.class, JsonPrimitive.class, JsonPrimitive::new, JsonPrimitive::getAsLong);
            if (Objects.equals(Float.class, type)) return this.createAdapter(Float.class, JsonPrimitive.class, JsonPrimitive::new, JsonPrimitive::getAsFloat);
            if (Objects.equals(Double.class, type)) return this.createAdapter(Double.class, JsonPrimitive.class, JsonPrimitive::new, JsonPrimitive::getAsDouble);
            if (Objects.equals(String.class, type)) return this.createAdapter(String.class, JsonPrimitive.class, JsonPrimitive::new, JsonPrimitive::getAsString);
            if (Objects.equals(byte[].class, type)) return this.createAdapter(byte[].class, JsonPrimitive.class, array -> new JsonPrimitive(Base64.getEncoder().encodeToString(array)), n2 -> Base64.getDecoder().decode(n2.getAsString()));
            if (Objects.equals(int[].class, type)) return this.createAdapter(int[].class, JsonArray.class, array -> system.json.array().add(List.of(array)).build(), n2 -> Streams.stream(n2.iterator()).mapToInt(JsonElement::getAsInt).toArray());
            if (Objects.equals(long[].class, type)) return this.createAdapter(long[].class, JsonArray.class, array -> system.json.array().add(List.of(array)).build(), n2 -> Streams.stream(n2.iterator()).mapToLong(JsonElement::getAsInt).toArray());
            if (Objects.equals(PersistentDataContainer[].class, type)) return this.createAdapter(PersistentDataContainer[].class, JsonArray.class,
                    containerArray -> system.json.array().add(List.of(containerArray), v -> ((JsonPersistentDataContainer)v).toJson()).build(),
                    tag -> Streams.stream(tag.iterator()).map(JsonElement::getAsJsonObject).map(JsonPersistentDataContainer::new).toArray(PersistentDataContainer[]::new));
            if (Objects.equals(PersistentDataContainer.class, type)) return this.createAdapter(JsonPersistentDataContainer.class, JsonObject.class, JsonPersistentDataContainer::toJson, JsonPersistentDataContainer::new);
            throw new IllegalArgumentException("Could not find a valid TagAdapter implementation for the requested type " + type.getSimpleName());
        }

        private <T, Z extends JsonElement> TagAdapter<T, Z> createAdapter(Class<T> primitiveType, Class<Z> nbtBaseType, Function<T, Z> builder, Function<Z, T> extractor) {
            return new TagAdapter<>(primitiveType, nbtBaseType, builder, extractor);
        }
        public <T> JsonElement wrap(Class<T> type, T value) {
            return this.adapters.computeIfAbsent(type, this.CREATE_ADAPTER).build(value);
        }
        public <T> boolean isInstanceOf(Class<T> type, JsonElement base) {
            return this.adapters.computeIfAbsent(type, this.CREATE_ADAPTER).isInstance(base);
        }
        public <T> T extract(Class<T> type, JsonElement tag) throws ClassCastException, IllegalArgumentException {
            TagAdapter<?, ?> adapter = this.adapters.computeIfAbsent(type, this.CREATE_ADAPTER);
            if (!adapter.isInstance(tag)) throw new IllegalArgumentException(String.format("`The found tag instance cannot store %s as it is a %s", type.getSimpleName(), tag.getClass().getSimpleName()));
            Object foundValue = adapter.extract(tag);
            if (!type.isInstance(foundValue)) throw new IllegalArgumentException(String.format("The found object is of the type %s. Expected type %s", foundValue.getClass().getSimpleName(), type.getSimpleName()));
            return type.cast(foundValue);
        }

        private record TagAdapter<T, Z extends JsonElement>(Class<T> primitiveType, Class<Z> jsonBaseType, Function<T, Z> builder, Function<Z, T> extractor) {
            private T extract(JsonElement base) {
                if (!this.jsonBaseType.isInstance(base)) throw new IllegalArgumentException(String.format("The provided JsonElement was of the type %s. Expected type %s", base.getClass().getSimpleName(), this.jsonBaseType.getSimpleName()));
                return this.extractor.apply(this.jsonBaseType.cast(base));
            }
            private Z build(Object value) {
                if (!this.primitiveType.isInstance(value)) throw new IllegalArgumentException(String.format("The provided value was of the type %s. Expected type %s", value.getClass().getSimpleName(), this.primitiveType.getSimpleName()));
                return this.builder.apply(this.primitiveType.cast(value));
            }
            boolean isInstance(JsonElement base) { return this.jsonBaseType.isInstance(base); }
        }
    }
}


















