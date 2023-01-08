package org.lime.gp.entity;

import net.minecraft.world.entity.EntityLimeMarker;
import net.minecraft.world.entity.EntityMarkerEventDestroy;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.lime.gp.entity.event.EntityMarkerEventInteract;
import org.lime.gp.entity.event.EntityMarkerEventTick;

public abstract class EntityMetadata {
    public static EntityMetadata empty(EntityLimeMarker marker) {
        return new EntityMetadata(marker) {
            @Override public void onTick(EntityMarkerEventTick event) {}
            @Override public void onDestroy(EntityMarkerEventDestroy event) {}
            @Override public void onInteract(EntityMarkerEventInteract event) {}
            @Override public void onDamage(EntityMarkerEventInteract event) {}
        };
    }

    public final EntityLimeMarker marker;

    public EntityMetadata(EntityLimeMarker marker) {
        this.marker = marker;
    }
    public abstract void onTick(EntityMarkerEventTick event);
    public abstract void onDestroy(EntityMarkerEventDestroy event);
    public abstract void onInteract(EntityMarkerEventInteract event);
    public abstract void onDamage(EntityMarkerEventInteract event);

    public void destroy() {
        marker.discard();
    }
    public Location location() {
        return location(0,0,0);
    }
    public Location location(double x, double y, double z) { return new Vector(marker.getX() + x, marker.getY() + y, marker.getZ() + z).toLocation(marker.getLevel().getWorld(), marker.getBukkitYaw(), marker.getXRot()); }
    public void moveTo(Location location) { marker.moveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch()); }
}
