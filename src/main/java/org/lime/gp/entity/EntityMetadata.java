package org.lime.gp.entity;

import net.minecraft.world.entity.EntityLimeMarker;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityMarkerEventDestroy;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.lime.gp.entity.event.EntityMarkerEventInput;
import org.lime.gp.entity.event.EntityMarkerEventInteract;
import org.lime.gp.entity.event.EntityMarkerEventTick;
import org.lime.gp.module.loot.PopulateLootEvent;

public abstract class EntityMetadata {
    public static EntityMetadata empty(EntityLimeMarker marker) {
        return new EntityMetadata(marker) {
            @Override public void onTick(EntityMarkerEventTick event) {}
            @Override public void onDestroy(EntityMarkerEventDestroy event) {}
            @Override public void onInteract(EntityMarkerEventInteract event) {}
            @Override public void onInput(EntityMarkerEventInput event) {}
            @Override public void onDamage(EntityMarkerEventInteract event) {}
            @Override public void onLoot(PopulateLootEvent event) {}
            @Override public void onTickAsync(long tick) {}
        };
    }

    public final EntityLimeMarker marker;

    public EntityMetadata(EntityLimeMarker marker) { this.marker = marker; }

    public abstract void onTick(EntityMarkerEventTick event);
    public abstract void onDestroy(EntityMarkerEventDestroy event);
    public abstract void onInteract(EntityMarkerEventInteract event);
    public abstract void onInput(EntityMarkerEventInput event);
    public abstract void onDamage(EntityMarkerEventInteract event);
    public abstract void onLoot(PopulateLootEvent event);
    public abstract void onTickAsync(long tick);

    public void destroy() { marker.discard(); }

    public Location location() { return location(0,0,0); }
    public Location location(double x, double y, double z) { return new Vector(marker.getX() + x, marker.getY() + y, marker.getZ() + z).toLocation(marker.level().getWorld(), marker.getBukkitYaw(), marker.getXRot()); }
    public void moveTo(Location location) { marker.moveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch()); }
}
