package org.lime.gp.block.component.display.event;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import net.minecraft.world.level.ChunkCoordIntPair;

public class PlayerChunkMoveEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    private final ChunkCoordIntPair chunkCoord;
    public PlayerChunkMoveEvent(@Nonnull Player who, @Nonnull ChunkCoordIntPair chunkCoord) {
        super(who);
        this.chunkCoord = chunkCoord;
    }
    public static void execute(@Nonnull Player who, @Nonnull ChunkCoordIntPair chunkCoord) {
        Bukkit.getPluginManager().callEvent(new PlayerChunkMoveEvent(who, chunkCoord));
    }


    public ChunkCoordIntPair getChunkCoord() { return chunkCoord; }

    @Override @Nonnull public HandlerList getHandlers() { return handlers; }
    @Nonnull public static HandlerList getHandlerList() { return handlers; }
}
