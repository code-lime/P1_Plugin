package net.minecraft.world.level.block.entity;

import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public interface ITickMetadata {
    boolean isLoaded();
    void onTick(TileEntitySkullTickInfo info);
    void onDestroy(BlockSkullDestroyInfo info);
    @Nullable EnumInteractionResult onInteract(BlockSkullInteractInfo info);
    @Nullable IBlockData onPlace(BlockSkullPlaceInfo info);
    @Nullable VoxelShape onShape(BlockSkullShapeInfo info);
    @Nullable IBlockData onState(BlockSkullStateInfo info);
}
