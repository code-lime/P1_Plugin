package org.lime.gp.entity;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.EntityLimeMarker;
import org.bukkit.Location;
import org.lime.gp.entity.component.ComponentStatic;
import org.lime.system.execute.*;

import java.util.*;

public final class EntityInfo {
    public final LinkedHashMap<String, ComponentStatic<?>> components = new LinkedHashMap<>();
    public final double distance;

    private final String _key;
    public String getKey() { return _key; }

    private final long loadIndex = System.currentTimeMillis();
    public long getLoadIndex() { return loadIndex; }

    public EntityInfo(String key) {
        this(key, 32);
    }
    public EntityInfo(String key, double distance) {
        this._key = key;
        this.distance = distance;
    }
    public EntityInfo(String key, JsonObject json) {
        this(key, json.has("distance") ? json.get("distance").getAsDouble() : 32);
        if (json.has("components")) json.get("components").getAsJsonObject().entrySet().forEach(kv -> {
            ComponentStatic<?> setting = ComponentStatic.parse(kv.getKey(), this, kv.getValue());
            components.put(setting.name(), setting);
        });
    }

    public EntityInfo add(ComponentStatic<?> component) {
        this.components.put(component.name(), component);
        return this;
    }
    public EntityInfo add(Func1<EntityInfo, ComponentStatic<?>> component) {
        return add(component.invoke(this));
    }
    @SuppressWarnings("unchecked")
    public <T extends ComponentStatic<?>> Optional<T> component(Class<T> tClass) {
        for (ComponentStatic<?> component : components.values()) {
            if (tClass.isInstance(component))
                return Optional.of((T) component);
        }
        return Optional.empty();
    }

    public EntityLimeMarker spawn(Location location) {
        return spawn(location, Collections.emptyMap());
    }
    public EntityLimeMarker spawn(Location location, Map<String, JsonObject> data) {
        return Entities.spawn(location, this, data);
    }

    @Override public String toString() {
        return "EntityInfo[" + Optional.ofNullable(getKey()).orElse("NULLABLE") + "]";
    }
}


















