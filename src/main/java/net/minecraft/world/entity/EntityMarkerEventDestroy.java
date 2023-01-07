package net.minecraft.world.entity;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EntityMarkerEventDestroy extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final EntityLimeMarker marker;
    private final Entity.RemovalReason reason;

    protected EntityMarkerEventDestroy(EntityLimeMarker marker, Entity.RemovalReason reason) {
        this.marker = marker;
        this.reason = reason;
    }
    public static void execute(EntityLimeMarker marker, Entity.RemovalReason reason) {
        EntityMarkerEventDestroy event = new EntityMarkerEventDestroy(marker, reason);
        Bukkit.getPluginManager().callEvent(event);
    }

    public EntityLimeMarker getMarker() { return marker; }
    public Entity.RemovalReason getReason() { return reason; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
