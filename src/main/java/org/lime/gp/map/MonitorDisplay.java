package org.lime.gp.map;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.entity.decoration.EntityItemFrame;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.lime.display.DisplayManager;
import org.lime.display.ObjectDisplay;
import org.lime.gp.module.DrawMap;
import org.lime.gp.module.TimeoutData;

import java.util.Map;
import java.util.UUID;

public class MonitorDisplay extends ObjectDisplay<MapMonitor.MonitorData, EntityItemFrame> {
    @Override public double getDistance() { return data.distance; }
    @Override public Location location() { return data.position.getLocation().add(data.offset); }
    @Override public boolean isFilter(Player player) { return data.isShow; }

    private int lastMapID;
    private MapMonitor.MonitorData data;

    private boolean syncMapID() {
        if (lastMapID == data.mapID) return false;
        lastMapID = data.mapID;
        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta)map.getItemMeta();
        meta.setMapId(data.mapID);
        map.setItemMeta(meta);
        entity.setItem(CraftItemStack.asNMSCopy(map), true, false);
        return true;
    }

    protected MonitorDisplay(MapMonitor.MonitorData data) {
        this.data = data;
        postInit();
        syncMapID();
    }
    @Override protected EntityItemFrame createEntity(Location location) {
        EntityItemFrame frame = new EntityItemFrame(
                ((CraftWorld)location.getWorld()).getHandle(),
                new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                EnumDirection.byName(BlockFace.UP.name()));
        frame.setInvulnerable(true);
        return frame;
    }

    @Override protected void show(Player player) {
        super.show(player);
        DrawMap.sendMap(player, data.mapID, data.map);
    }

    @Override public void update(MapMonitor.MonitorData data, double delta) {
        this.data = data;
        int rotation = data.rotation.ordinal();
        boolean sync = syncMapID();
        if (entity.getRotation() != rotation) {
            sync = true;
            entity.setRotation(rotation);
        }
        if (sync) invokeAll(this::sendDataWatcher);
        super.update(data, delta);
    }

    public static class MonitorManager extends DisplayManager<UUID, MapMonitor.MonitorData, MonitorDisplay> {
        @Override public boolean isFast() { return true; }
        @Override public boolean isAsync() { return true; }

        @Override public Map<UUID, MapMonitor.MonitorData> getData() { return TimeoutData.map(MapMonitor.MonitorData.class); }
        @Override public MonitorDisplay create(UUID uuid, MapMonitor.MonitorData meta) { return new MonitorDisplay(meta); }
    }
    public static MonitorManager manager() { return new MonitorManager(); }
}
