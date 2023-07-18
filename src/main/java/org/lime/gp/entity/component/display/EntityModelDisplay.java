package org.lime.gp.entity.component.display;

import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.lime.display.DisplayManager;
import org.lime.display.ObjectDisplay;
import org.lime.display.models.ChildDisplay;
import org.lime.gp.module.TimeoutData;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class EntityModelDisplay extends ObjectDisplay<DisplayInstance.DisplayObject, Marker> {
    @Override public double getDistance() { return Double.POSITIVE_INFINITY; }
    @Override public Location location() { return data.location(); }

    public final EntityModelKey key;

    public DisplayInstance.DisplayObject data;
    public ChildDisplay<DisplayInstance.DisplayObject> model;

    @Override public boolean isFilter(Player player) { return data.hasViewer(player.getUniqueId()); }

    private EntityModelDisplay(EntityModelKey key, DisplayInstance.DisplayObject data) {
        this.key = key;
        this.data = data;
        model = preInitDisplay(data.model().display(this));
        postInit();
    }
    @Override public void update(DisplayInstance.DisplayObject data, double delta) {
        this.data = data;
        super.update(data, delta);
        data.model().animation.apply(model.js, data.data());
        this.invokeAll(this::sendData);
    }

    @Override protected net.minecraft.world.entity.Marker createEntity(Location location) {
        return new net.minecraft.world.entity.Marker(EntityTypes.MARKER, ((CraftWorld)location.getWorld()).getHandle());
    }

    public record EntityModelKey(UUID entity_uuid, UUID model_uuid, UUID element_uuid) { }
    public static class EntityModelManager extends DisplayManager<EntityModelKey, DisplayInstance.DisplayObject, EntityModelDisplay> {
        @Override public boolean isAsync() { return true; }
        @Override public boolean isFast() { return true; }

        @Override public Map<EntityModelKey, DisplayInstance.DisplayObject> getData() {
            return TimeoutData.values(DisplayInstance.DisplayMap.class)
                    .flatMap(kv -> kv.map.entrySet().stream())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        @Override public EntityModelDisplay create(EntityModelKey key, DisplayInstance.DisplayObject meta) { return new EntityModelDisplay(key, meta); }
    }
    public static EntityModelManager manager() { return new EntityModelManager(); }

    public static Optional<EntityModelDisplay> of(EntityModelKey key) {
        return Optional.ofNullable(EntityDisplay.MODEL_MANAGER.getDisplays().get(key));
    }
}



















