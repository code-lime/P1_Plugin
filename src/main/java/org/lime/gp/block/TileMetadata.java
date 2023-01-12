package org.lime.gp.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ITickMetadata;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.entity.TileEntitySkullEventRemove;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.util.Vector;
import org.lime.Position;
import org.lime.gp.lime;
import org.lime.gp.module.PopulateLootEvent;

import javax.annotation.Nullable;

public abstract class TileMetadata implements ITickMetadata {
    public static TileMetadata empty(TileEntityLimeSkull skull) {
        return new TileMetadata(skull) {
            @Override public boolean isLoaded() { return lime._plugin.isEnabled(); }
            @Override public void onTick(TileEntitySkullTickInfo event) {}
            @Override public void onTickAsync(long tick) {}
            @Override public void onRemove(TileEntitySkullEventRemove event) {}
            @Override public @Nullable EnumInteractionResult onInteract(BlockSkullInteractInfo event) { return null; }
            @Override public @Nullable VoxelShape onShape(BlockSkullShapeInfo event) { return null; }
            @Override public @Nullable IBlockData onState(BlockSkullStateInfo info) { return null; }
            @Override public void onLoot(PopulateLootEvent event) {}
            @Override public void onDamage(BlockDamageEvent event) {}
            @Override public void onDestroy(BlockSkullDestroyInfo event) {}
            @Override public @Nullable IBlockData onPlace(BlockSkullPlaceInfo event) { return null; }
            @Override public String toString() { return "EMPTY_TILE_METADATA:"+skull.getBlockPos(); }
        };
    }

    public final TileEntityLimeSkull skull;

    public TileMetadata(TileEntityLimeSkull skull) {
        this.skull = skull;
    }
    @Override public boolean isLoaded() { return lime._plugin.isEnabled(); }
    @Override public abstract void onTick(TileEntitySkullTickInfo event);
    public abstract void onTickAsync(long tick);
    public abstract void onRemove(TileEntitySkullEventRemove event);
    @Override public abstract @Nullable EnumInteractionResult onInteract(BlockSkullInteractInfo event);
    @Override public abstract @Nullable VoxelShape onShape(BlockSkullShapeInfo event);
    @Override public abstract @Nullable IBlockData onState(BlockSkullStateInfo event);
    public abstract void onLoot(PopulateLootEvent event);
    public abstract void onDamage(BlockDamageEvent event);
    @Override public abstract void onDestroy(BlockSkullDestroyInfo event);
    @Override public abstract @Nullable IBlockData onPlace(BlockSkullPlaceInfo event);

    public void setAir() {
        skull.getLevel().setBlock(skull.getBlockPos(), Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
    }
    public Block block() {
        BlockPosition pos = skull.getBlockPos();
        return skull.getLevel().getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
    }
    public Location location() {
        return location(0,0,0);
    }
    public Location location(double x, double y, double z) {
        BlockPosition pos = skull.getBlockPos();
        return new Vector(pos.getX() + x, pos.getY() + y, pos.getZ() + z).toLocation(skull.getLevel().getWorld());
    }
    public Position position() {
        BlockPosition pos = skull.getBlockPos();
        return new Position(skull.getLevel().getWorld(), pos.getX(), pos.getY(), pos.getZ());
    }
}
