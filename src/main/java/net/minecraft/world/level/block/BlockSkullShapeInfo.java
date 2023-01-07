package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

//VoxelShape result
public record BlockSkullShapeInfo(BlockLimeSkull skull,
                                  IBlockData state,
                                  IBlockAccess world,
                                  BlockPosition pos,
                                  VoxelShapeCollision context) {
}
