package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TileEntitySkullPreTickEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final World world;
    private final BlockPosition pos;
    private final IBlockData state;
    private final TileEntityLimeSkull skull;

    protected TileEntitySkullPreTickEvent(World world, BlockPosition pos, IBlockData state, TileEntityLimeSkull skull) {
        this.skull = skull;
        this.world = world;
        this.pos = pos;
        this.state = state;
    }
    public static void execute(World world, BlockPosition pos, IBlockData state, TileEntityLimeSkull skull) {
        TileEntitySkullPreTickEvent event = new TileEntitySkullPreTickEvent(world, pos, state, skull);
        Bukkit.getPluginManager().callEvent(event);
    }

    public TileEntitySkullTickInfo info() {
        return new TileEntitySkullTickInfo(world, pos, state, skull);
    }

    public World getWorld() { return world; }
    public BlockPosition getPos() { return pos; }
    public IBlockData getState() { return state; }
    public TileEntityLimeSkull getSkull() { return skull; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
