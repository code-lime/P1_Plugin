package org.lime.gp.map;

import net.minecraft.world.level.block.entity.TileEntitySkullEventRemove;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.display.ObjectDisplay;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.module.DrawMap;
import org.lime.gp.module.TimeoutData;

import java.util.*;

public abstract class MonitorInstance extends BlockInstance implements CustomTileMetadata.Tickable, CustomTileMetadata.Removeable {
    public final int MapID = DrawMap.getNextMapID();

    public abstract MapMonitor.MapRotation rotation();
    public double distance() { return 10; }
    public boolean isShow() { return true; }
    public Vector offset() { return new Vector(); }

    public MonitorInstance(ComponentDynamic<?, ?> component, CustomTileMetadata metadata) {
        super(component, metadata);
    }

    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        TimeoutData.put(unique(), MapMonitor.MonitorData.class, new MapMonitor.MonitorData(this));
    }
    @Override public void onRemove(CustomTileMetadata metadata, TileEntitySkullEventRemove event) {
        TimeoutData.remove(unique(), MapMonitor.MonitorData.class);
    }

    public List<Player> viewers() {
        return MapMonitor.MONITOR_MANAGER
                .getDisplay(unique())
                .map(ObjectDisplay::getShowPlayers)
                .orElseGet(Collections::emptyList);
    }
    public boolean hasViewers() {
        return MapMonitor.MONITOR_MANAGER
                .getDisplay(unique())
                .map(ObjectDisplay::getShowCount)
                .orElse(0) > 0;
    }
    public abstract byte[] preMap();

    private final Set<Player> tickMouse = new HashSet<>();
    public final void onPostMouse(Collection<Player> lastMouse) {
        tickMouse.removeIf(player -> {
            if (lastMouse.contains(player)) return false;
            onMouseEnd(player);
            return true;
        });
        onPostTick(lastMouse, viewers());
    }
    public final void onMouse(Player player, ViewPosition position) {
        if (tickMouse.add(player)) onMouseStart(player, position);
        onMouseTick(player, position);
    }

    protected final static int SIZE = 128;
    public abstract void onPostTick(Collection<Player> tickable, Collection<Player> viewers);
    public abstract void onMouseTick(Player player, ViewPosition position);
    public abstract void onMouseStart(Player player, ViewPosition position);
    public abstract void onMouseEnd(Player player);
}

