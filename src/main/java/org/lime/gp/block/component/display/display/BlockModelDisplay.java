package org.lime.gp.block.component.display.display;

import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.lime.Position;
import org.lime.display.DisplayManager;
import org.lime.display.ObjectDisplay;
import org.lime.display.models.display.BaseChildDisplay;
import org.lime.gp.block.component.display.BlockDisplay;
import org.lime.gp.block.component.display.instance.list.ModelDisplayObject;
import org.lime.gp.module.TimeoutData;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class BlockModelDisplay extends ObjectDisplay<ModelDisplayObject, Marker> {
    @Override public double getDistance() { return data.distance(); }
    @Override public Location location() { return data.location(); }

    public final BlockModelKey key;

    public ModelDisplayObject data;
    public BaseChildDisplay<?, ModelDisplayObject, ?> model;

    @Override public boolean isFilter(Player player) {
        return data.hasViewer(player.getUniqueId());
    }

    private BlockModelDisplay(BlockModelKey key, ModelDisplayObject data) {
        this.key = key;
        this.data = data;
        model = data.model().display(this);
        preInitDisplay(model);
        postInit();
    }
    @Override public void update(ModelDisplayObject data, double delta) {
        this.data = data;
        super.update(data, delta);
        /*TODO*///data.model().animation().apply(model.js, data.data());
        this.invokeAll(this::sendData);
    }

    @Override protected Marker createEntity(Location location) {
        return new Marker(EntityTypes.MARKER, ((CraftWorld)location.getWorld()).getHandle());
    }

    public record BlockModelKey(UUID block_uuid, Position block_position, UUID model_uuid, UUID element_uuid) { }
    public static class EntityModelManager extends DisplayManager<BlockModelKey, ModelDisplayObject, BlockModelDisplay> {
        @Override public boolean isAsync() { return true; }
        @Override public boolean isFast() { return true; }

        @Override public Map<BlockModelKey, ModelDisplayObject> getData() {
            return TimeoutData.values(org.lime.gp.block.component.display.instance.DisplayMap.class)
                    .flatMap(kv -> kv.modelMap.entrySet().stream())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        @Override public BlockModelDisplay create(BlockModelKey key, ModelDisplayObject meta) { return new BlockModelDisplay(key, meta); }
    }
    public static EntityModelManager manager() { return new EntityModelManager(); }

    public static Optional<BlockModelDisplay> of(BlockModelKey key) {
        return Optional.ofNullable(BlockDisplay.MODEL_MANAGER.getDisplays().get(key));
    }
}
