package org.lime.gp.extension;

import com.google.gson.*;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.lime.gp.lime;

import java.util.concurrent.ConcurrentHashMap;

public class JManager {
    private static final ConcurrentHashMap<String, NamespacedKey> keys = new ConcurrentHashMap<>();
    public static NamespacedKey key(String key) {
        return keys.computeIfAbsent(key, _key -> new NamespacedKey(lime._plugin, _key));
    }

    public static boolean has(PersistentDataContainer container, NamespacedKey key) {
        return container.has(key, PersistentDataType.STRING);
    }
    public static boolean has(PersistentDataContainer container, String key) {
        return has(container, key(key));
    }
    public static void del(PersistentDataContainer container, NamespacedKey key) {
        container.remove(key);
    }
    public static void del(PersistentDataContainer container, String key) {
        del(container, key(key));
    }

    public static void set(PersistentDataContainer container, NamespacedKey key, JsonElement json) {
        container.set(key, PersistentDataType.STRING, json.toString());
    }
    public static void set(PersistentDataContainer container, String key, JsonElement json) {
        set(container, key(key), json);
    }
    @SuppressWarnings("unchecked")
    public static <IJson extends JsonElement> IJson get(Class<IJson> tClass, PersistentDataContainer container, NamespacedKey key, IJson _default) {
        String str = container.getOrDefault(key, PersistentDataType.STRING, _default == null ? "" : _default.toString());
        if (str.isEmpty()) return null;
        return (IJson)JsonParser.parseString(str);
    }
    public static <IJson extends JsonElement> IJson get(Class<IJson> tClass, PersistentDataContainer container, String key, IJson _default) {
        return get(tClass, container, key(key), _default);
    }
}
