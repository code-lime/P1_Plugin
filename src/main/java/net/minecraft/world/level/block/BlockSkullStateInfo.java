package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.IBlockAccess;

//IBlockData state
public record BlockSkullStateInfo(BlockLimeSkull skull,
                                  IBlockAccess world,
                                  BlockPosition pos,
                                  EntityHuman player) {
}
