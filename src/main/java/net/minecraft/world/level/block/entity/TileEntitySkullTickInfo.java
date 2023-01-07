package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntitySkullTickInfo {
    private final World world;
    private final BlockPosition pos;
    private final IBlockData state;
    private final TileEntityLimeSkull skull;

    protected TileEntitySkullTickInfo(World world, BlockPosition pos, IBlockData state, TileEntityLimeSkull skull) {
        this.skull = skull;
        this.world = world;
        this.pos = pos;
        this.state = state;
    }

    public World getWorld() { return world; }
    public BlockPosition getPos() { return pos; }
    public IBlockData getState() { return state; }
    public TileEntityLimeSkull getSkull() { return skull; }
}
