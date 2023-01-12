package org.lime.gp.block.component.display.event;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerChunkMoveEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    private final ChunkCoordCache.Cache chunkCoord;
    public PlayerChunkMoveEvent(@Nonnull Player who, @Nonnull ChunkCoordCache.Cache chunkCoord) {
        super(who);
        this.chunkCoord = chunkCoord;
    }
    public static void execute(@Nonnull Player who, @Nonnull ChunkCoordCache.Cache chunkCoord) {
        Bukkit.getPluginManager().callEvent(new PlayerChunkMoveEvent(who, chunkCoord));
    }


    public ChunkCoordCache.Cache getChunkCoord() { return chunkCoord; }

    @Override @Nonnull public HandlerList getHandlers() { return handlers; }
    @Nonnull public static HandlerList getHandlerList() { return handlers; }
}
