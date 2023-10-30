package net.minecraft.world.item;

import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.player.EntityHuman;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import javax.annotation.Nullable;

public class EggSpawnEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final EntityTypes<?> type;
    private final WorldServer world;
    private final @Nullable ItemStack item;
    private final @Nullable EntityHuman player;
    private final BlockPosition pos;
    private final EnumMobSpawn spawnReason;
    private final boolean alignPosition;
    private final boolean invertY;
    private @Nullable Entity override;

    private boolean cancelled = false;

    public EggSpawnEvent(EntityTypes<?> type, WorldServer world, @Nullable ItemStack item, @Nullable EntityHuman player, BlockPosition pos, EnumMobSpawn spawnReason, boolean alignPosition, boolean invertY) {
        this.type = type;
        this.world = world;
        this.item = item;
        this.player = player;
        this.pos = pos;
        this.spawnReason = spawnReason;
        this.alignPosition = alignPosition;
        this.invertY = invertY;
    }

    public static @Nullable <T extends Entity>T execute(EntityTypes<T> type, WorldServer world, @Nullable ItemStack item, @Nullable EntityHuman player, BlockPosition pos, EnumMobSpawn spawnReason, boolean alignPosition, boolean invertY) {
        EggSpawnEvent event = new EggSpawnEvent(type, world, item, player, pos, spawnReason, alignPosition, invertY);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return null;
        if (event.override != null) return (T)event.override;
        return type.spawn(world, item, player, pos, spawnReason, alignPosition, invertY);
    }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }

    public EntityTypes<?> getType() { return type; }
    public WorldServer getWorld() { return world; }

    public @Nullable ItemStack getItem() { return item; }
    public @Nullable EntityHuman getPlayer() { return player; }
    public BlockPosition getPos() { return pos; }
    public EnumMobSpawn getSpawnReason() { return spawnReason; }
    public boolean isAlignPosition() { return alignPosition; }
    public boolean isInvertY() { return invertY; }

    public void setOverride(Entity entity) { override = entity; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
