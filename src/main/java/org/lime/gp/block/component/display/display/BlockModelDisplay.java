package org.lime.gp.block.component.display.display;

import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.lime.Position;
import org.lime.display.DisplayManager;
import org.lime.display.Models;
import org.lime.display.ObjectDisplay;
import org.lime.gp.block.component.display.BlockDisplay;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.module.TimeoutData;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class BlockModelDisplay extends ObjectDisplay<DisplayInstance.ModelDisplayObject, Marker> {
    @Override public double getDistance() { return Double.POSITIVE_INFINITY; }
    @Override public Location location() { return data.location(); }

    public final BlockModelKey key;

    public DisplayInstance.ModelDisplayObject data;
    public Models.Model.ChildDisplay<DisplayInstance.ModelDisplayObject> model;

    @Override public boolean isFilter(Player player) {
        return data.hasViewer(player.getUniqueId());
    }

    private BlockModelDisplay(BlockModelKey key, DisplayInstance.ModelDisplayObject data) {
        this.key = key;
        this.data = data;
        model = preInitDisplay(data.model().display(this));
        postInit();
    }
    @Override public void update(DisplayInstance.ModelDisplayObject data, double delta) {
        this.data = data;
        super.update(data, delta);
        data.model().animation.apply(model.js, data.data());
        this.invokeAll(this::sendData);
    }

    @Override protected Marker createEntity(Location location) {
        return new Marker(EntityTypes.MARKER, ((CraftWorld)location.getWorld()).getHandle());
    }

    public record BlockModelKey(UUID block_uuid, Position block_position, UUID model_uuid, UUID element_uuid) { }
    public static class EntityModelManager extends DisplayManager<BlockModelKey, DisplayInstance.ModelDisplayObject, BlockModelDisplay> {
        @Override public boolean isAsync() { return true; }
        @Override public boolean isFast() { return true; }

        @Override public Map<BlockModelKey, DisplayInstance.ModelDisplayObject> getData() {
            return TimeoutData.values(org.lime.gp.block.component.display.instance.DisplayInstance.DisplayMap.class)
                    .flatMap(kv -> kv.modelMap.entrySet().stream())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        @Override public BlockModelDisplay create(BlockModelKey key, DisplayInstance.ModelDisplayObject meta) { return new BlockModelDisplay(key, meta); }
    }
    public static EntityModelManager manager() { return new EntityModelManager(); }

    public static Optional<BlockModelDisplay> of(BlockModelKey key) {
        return Optional.ofNullable(BlockDisplay.MODEL_MANAGER.getDisplays().get(key));
    }
}



















