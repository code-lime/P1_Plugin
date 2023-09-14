package org.lime.gp.entity.component.display;

import net.minecraft.world.entity.EntityLimeMarker;
import net.minecraft.world.entity.EntityMarkerEventDestroy;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.display.models.shadow.IBuilder;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityInstance;
import org.lime.gp.entity.component.Components;
import org.lime.gp.entity.event.EntityMarkerEventTick;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.module.TimeoutData;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DisplayInstance extends EntityInstance implements CustomEntityMetadata.Tickable, EntityDisplay.Displayable, CustomEntityMetadata.Destroyable {
    private final ConcurrentHashMap<String, String> variables = new ConcurrentHashMap<>();

    @Override public Components.DisplayComponent component() { return (Components.DisplayComponent)super.component(); }

    public DisplayInstance(Components.DisplayComponent component, CustomEntityMetadata metadata) {
        super(component, metadata);
    }
    public DisplayInstance set(String key, String value) {
        variables.put(key, value);
        saveData();
        return this;
    }
    public Optional<String> get(String key) { return Optional.ofNullable(variables.get(key)); }
    public Map<String, String> getAll() { return new HashMap<>(variables); }

    public Optional<DisplayPartial.Partial> getPartial(double distanceSquared) {
        for (DisplayPartial.Partial partial : component().partials) {
            if (partial.distanceSquared <= distanceSquared)
                return Optional.of(partial.partial(variables));
        }
        return Optional.empty();
    }

    public static final class DisplayMap extends TimeoutData.ITimeout {
        public final Map<EntityModelDisplay.EntityModelKey, DisplayObject> map = new HashMap<>();
        public DisplayMap(Map<EntityModelDisplay.EntityModelKey, DisplayObject> map) {
            super(3);
            this.map.putAll(map);
        }
    }

    public record DisplayObject(Location location, List<UUID> viewers, IBuilder model, Map<String, Object> data) {
        public boolean hasViewer(UUID uuid) { return viewers.contains(uuid); }
        public static DisplayObject of(Location location, List<UUID> viewers, IBuilder model, Map<String, Object> data) {
            return new DisplayObject(location, viewers, model, data);
        }
    }

    @Override public void read(JsonObjectOptional json) {
        variables.clear();
        json.forEach((key, value) -> value.getAsString().ifPresent(v -> variables.put(key, v)));
    }
    @Override public json.builder.object write() { return json.object().add(variables, k -> k, v -> v); }

    private final ConcurrentHashMap<String, Object> animationData = new ConcurrentHashMap<>();
    @Override public void onTick(CustomEntityMetadata metadata, EntityMarkerEventTick event) {
        EntityLimeMarker marker = event.getMarker();
        Vector pos = new Vector(marker.getX(), marker.getY(), marker.getZ());
        float yaw = marker.getBukkitYaw();
        float pitch = marker.getXRot();
        World world = event.getWorld().getWorld();
        HashMap<EntityModelDisplay.EntityModelKey, DisplayObject> map = new HashMap<>();
        component().animationTick(new HashMap<>(variables), animationData);
        EntityPosition.playerLocations.forEach((player, location) ->
                metadata.list(EntityDisplay.Displayable.class)
                        .forEach(displayable -> displayable.onDisplay(player, marker)
                                .flatMap(EntityDisplay.IEntity::data)
                                .map(model -> map.computeIfAbsent(
                                        new EntityModelDisplay.EntityModelKey(marker.getUUID(), model.unique(), displayable.unique()),
                                        k -> DisplayObject.of(pos.toLocation(world, yaw, pitch), new ArrayList<>(), model, animationData))
                                )
                                .ifPresent(v -> v.viewers.add(player.getUniqueId()))
                        )
        );
        if (map.size() == 0) TimeoutData.remove(unique(), DisplayMap.class);
        else TimeoutData.put(unique(), DisplayMap.class, new DisplayMap(map));
    }
    @Override public Optional<EntityDisplay.IEntity> onDisplay(Player player, EntityLimeMarker marker) {
        return player.getWorld() != marker.getLevel().getWorld()
                ? Optional.empty()
                : getPartial(player.getLocation().toVector().distanceSquared(new Vector(marker.getX(), marker.getY(), marker.getZ())))
                .map(v -> v instanceof EntityDisplay.Displayable displayable ? displayable : null)
                .flatMap(displayable -> displayable.onDisplay(player, marker));
    }
    @Override public void onDestroy(CustomEntityMetadata metadata, EntityMarkerEventDestroy event) {
        TimeoutData.remove(unique(), DisplayMap.class);
    }
}



























