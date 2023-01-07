package org.lime.gp.entity.event;

import net.minecraft.world.entity.EntityLimeMarker;
import net.minecraft.world.level.World;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EntityMarkerEventTick extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final World world;
    private final EntityLimeMarker marker;
    private final double delta;

    protected EntityMarkerEventTick(World world, EntityLimeMarker marker, double delta) {
        this.world = world;
        this.marker = marker;
        this.delta = delta;
    }
    public static void execute(World world, EntityLimeMarker marker, double delta) {
        EntityMarkerEventTick event = new EntityMarkerEventTick(world, marker, delta);
        Bukkit.getPluginManager().callEvent(event);
    }

    public World getWorld() { return world; }
    public EntityLimeMarker getMarker() { return marker; }
    public double getDelta() { return delta; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
