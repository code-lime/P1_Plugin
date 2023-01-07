package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.MovingObjectPositionBlock;

//EnumInteractionResult result = EnumInteractionResult.PASS
public record BlockSkullInteractInfo(BlockLimeSkull skull,
                                     IBlockData state,
                                     World world,
                                     BlockPosition pos,
                                     EntityHuman player,
                                     EnumHand hand,
                                     MovingObjectPositionBlock hit) {
}
