package org.lime.gp.block.component.display.display;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.world.entity.decoration.EntityItemFrame;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.lime.display.DisplayManager;
import org.lime.display.EditedDataWatcher;
import org.lime.display.ObjectDisplay;
import org.lime.gp.block.component.display.instance.DisplayMap;
import org.lime.gp.block.component.display.instance.list.ItemFrameDisplayObject;
import org.lime.gp.module.TimeoutData;
import org.lime.system.toast.*;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class BlockItemFrameDisplay extends ObjectDisplay<ItemFrameDisplayObject, EntityItemFrame> {
    private static final DataWatcherObject<ItemStack> DATA_ITEM = EditedDataWatcher.getDataObject(EntityItemFrame.class, "DATA_ITEM");
    private static final DataWatcherObject<Integer> DATA_ROTATION = EditedDataWatcher.getDataObject(EntityItemFrame.class, "DATA_ROTATION");


    @Override public double getDistance() { return Double.POSITIVE_INFINITY; }
    @Override public Location location() { return data.location(); }

    public final UUID block_uuid;
    public final UUID player_uuid;

    public ItemFrameDisplayObject data;

    @Override public boolean isFilter(Player player) { return player_uuid.equals(player.getUniqueId()); }

    private BlockItemFrameDisplay(UUID block_uuid, UUID player_uuid, ItemFrameDisplayObject data) {
        super(data.location());

        this.block_uuid = block_uuid;
        this.player_uuid = player_uuid;
        this.data = data;
        postInit();
    }

    @Override public void update(ItemFrameDisplayObject data, double delta) {
        super.update(data, delta);
        if (this.data.index().equals(data.index())) {
            this.data = data;
            return;
        }
        this.data = data;
        invokeAll(this::sendDataWatcher);
    }

    @Override protected void editDataWatcher(Player player, EditedDataWatcher dataWatcher) {
        super.editDataWatcher(player, dataWatcher);
        dataWatcher.setCustom(DATA_ITEM, data.item());
        dataWatcher.setCustom(DATA_ROTATION, data.rotation().ordinal());
    }
    @Override protected EntityItemFrame createEntity(Location location) {
        EntityItemFrame itemFrame = new EntityItemFrame(
                ((CraftWorld)location.getWorld()).getHandle(),
                new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                EnumDirection.UP);
        itemFrame.setInvisible(true);
        itemFrame.setInvulnerable(true);
        return itemFrame;
    }

    public static class BlockItemFrameManager extends DisplayManager<Toast2<UUID, UUID>, ItemFrameDisplayObject, BlockItemFrameDisplay> {
        @Override public boolean isAsync() { return true; }
        @Override public boolean isFast() { return true; }

        @Override public Map<Toast2<UUID, UUID>, ItemFrameDisplayObject> getData() {
            return TimeoutData.stream(DisplayMap.class)
                    .flatMap(kv -> kv.getValue().frameMap.entrySet().stream().map(v -> Toast.of(kv.getKey(), v.getKey(), v.getValue())))
                    .collect(Collectors.toMap(kv -> Toast.of(kv.val0, kv.val1), kv -> kv.val2));
        }
        @Override public BlockItemFrameDisplay create(Toast2<UUID, UUID> uuid, ItemFrameDisplayObject display) { return new BlockItemFrameDisplay(uuid.val0, uuid.val1, display); }
    }

    public static BlockItemFrameManager manager() { return new BlockItemFrameManager(); }
}