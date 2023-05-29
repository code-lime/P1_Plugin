package org.lime.gp.block.component.display.display;

import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.joml.Vector3f;
import org.lime.display.DisplayManager;
import org.lime.display.EditedDataWatcher;
import org.lime.display.ObjectDisplay;
import org.lime.gp.block.component.display.instance.DisplayMap;
import org.lime.gp.block.component.display.instance.list.ItemDisplayObject;
import org.lime.gp.module.TimeoutData;

import com.mojang.math.Transformation;

import org.lime.system;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class BlockItemDisplay extends ObjectDisplay<ItemDisplayObject, Display.ItemDisplay> {
    @Override public double getDistance() { return Double.POSITIVE_INFINITY; }
    @Override public Location location() { return data.location(); }

    public final UUID block_uuid;
    public final UUID player_uuid;

    public ItemDisplayObject data;

    @Override public boolean isFilter(Player player) { return player_uuid.equals(player.getUniqueId()); }

    private BlockItemDisplay(UUID block_uuid, UUID player_uuid, ItemDisplayObject data) {
        super(data.location());

        this.block_uuid = block_uuid;
        this.player_uuid = player_uuid;
        this.data = data;
        postInit();
    }

    @Override public void update(ItemDisplayObject data, double delta) {
        super.update(data, delta);
        if (this.data.index().equals(data.index())) {
            this.data = data;
            return;
        }
        this.data = data;
        entity.setYRot(this.data.rotation().angle);
        invokeAll(this::sendDataWatcher);
    }

    @Override protected void editDataWatcher(Player player, EditedDataWatcher dataWatcher) {
        super.editDataWatcher(player, dataWatcher);
        dataWatcher.setCustom(EditedDataWatcher.DATA_ITEM_STACK_ID, data.item());
    }
    @Override protected Display.ItemDisplay createEntity(Location location) {
        Display.ItemDisplay itemFrame = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, ((CraftWorld)location.getWorld()).getHandle());
        itemFrame.setPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        itemFrame.setTransformation(new Transformation(new Vector3f(0.5f, 0.5f, 0.5f), null, null, null));
        return itemFrame;
    }

    public static class BlockItemManager extends DisplayManager<system.Toast2<UUID, UUID>, ItemDisplayObject, BlockItemDisplay> {
        @Override public boolean isAsync() { return true; }
        @Override public boolean isFast() { return true; }

        @Override public Map<system.Toast2<UUID, UUID>, ItemDisplayObject> getData() {
            return TimeoutData.stream(DisplayMap.class)
                    .flatMap(kv -> kv.getValue().viewMap.entrySet().stream().map(v -> system.toast(kv.getKey(), v.getKey(), v.getValue())))
                    .collect(Collectors.toMap(kv -> system.toast(kv.val0, kv.val1), kv -> kv.val2));
        }
        @Override public BlockItemDisplay create(system.Toast2<UUID, UUID> uuid, ItemDisplayObject display) { return new BlockItemDisplay(uuid.val0, uuid.val1, display); }
    }

    public static BlockItemManager manager() { return new BlockItemManager(); }
}