package org.lime.gp.entity.component.display.display;

import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.lime.display.DisplayManager;
import org.lime.display.ObjectDisplay;
import org.lime.display.models.display.BaseChildDisplay;
import org.lime.gp.entity.component.display.EntityDisplay;
import org.lime.gp.entity.component.display.instance.DisplayInstance;
import org.lime.gp.entity.component.display.instance.DisplayMap;
import org.lime.gp.entity.component.display.instance.DisplayObject;
import org.lime.gp.module.TimeoutData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntityModelDisplay extends ObjectDisplay<DisplayObject, Marker> {
    @Override public double getDistance() { return Double.POSITIVE_INFINITY; }
    @Override public Location location() { return data.location(); }

    public final EntityModelKey key;

    public DisplayObject data;
    public BaseChildDisplay<?, DisplayObject, ?> model;

    @Override public boolean isFilter(Player player) { return data.hasViewer(player.getUniqueId()); }

    private EntityModelDisplay(EntityModelKey key, DisplayObject data) {
        this.key = key;
        this.data = data;
        model = preInitDisplay(data.model().<DisplayObject>display(this));
        postInit();
    }
    @Override public void update(DisplayObject data, double delta) {
        this.data = data;
        super.update(data, delta);
        data.model().animation().apply(model.js, data.data());
        this.invokeAll(this::sendData);
    }

    @Override protected net.minecraft.world.entity.Marker createEntity(Location location) {
        return new net.minecraft.world.entity.Marker(EntityTypes.MARKER, ((CraftWorld)location.getWorld()).getHandle());
    }

    public record EntityModelKey(UUID entity, UUID model, UUID element) { }
    public static class EntityModelManager extends DisplayManager<EntityModelKey, DisplayObject, EntityModelDisplay> {
        @Override public boolean isAsync() { return true; }
        @Override public boolean isFast() { return true; }

        @Override public Map<EntityModelKey, DisplayObject> getData() {
            return TimeoutData.values(DisplayMap.class)
                    .flatMap(kv -> kv.map.entrySet().stream())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        @Override public EntityModelDisplay create(EntityModelKey key, DisplayObject meta) { return new EntityModelDisplay(key, meta); }
    }
    public static EntityModelManager manager() { return new EntityModelManager(); }

    public static Optional<EntityModelDisplay> of(EntityModelKey key) {
        return Optional.ofNullable(EntityDisplay.MODEL_MANAGER.getDisplays().get(key));
    }
    public static Stream<EntityModelDisplay> of(Stream<EntityModelKey> keys) {
        ConcurrentHashMap<EntityModelKey, EntityModelDisplay> displays = EntityDisplay.MODEL_MANAGER.getDisplays();
        return keys.map(displays::get).filter(Objects::nonNull);
    }
    public static Stream<EntityModelDisplay> of(DisplayInstance instance) {
        return EntityModelDisplay.of(TimeoutData.get(instance.unique(), DisplayMap.class)
                .stream()
                .flatMap(v -> v.map.keySet().stream()));
    }
}



















