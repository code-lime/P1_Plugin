package net.minecraft.world.level;

import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.chunk.Chunk;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SnowAccumulationHeightEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private int height;
    private final WorldServer world;
    private final Chunk chunk;
    private final BlockPosition position;

    private SnowAccumulationHeightEvent(WorldServer world, Chunk chunk, BlockPosition position, int height) {
        this.world = world;
        this.chunk = chunk;
        this.position = position;
        this.height = height;
    }

    public static int execute(BlockPosition position, WorldServer world, Chunk chunk, int height) {
        SnowAccumulationHeightEvent event = new SnowAccumulationHeightEvent(world, chunk, position, height);
        Bukkit.getPluginManager().callEvent(event);
        return event.height;
    }

    public WorldServer getWorld() { return world; }
    public Chunk getChunk() { return chunk; }
    public BlockPosition getPosition() { return position; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }

}
