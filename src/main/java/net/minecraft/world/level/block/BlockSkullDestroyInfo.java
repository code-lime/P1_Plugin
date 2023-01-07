package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;

import java.util.Optional;

public record BlockSkullDestroyInfo(BlockLimeSkull skull, IBlockData state, World world, BlockPosition pos, Optional<EntityHuman> player) {
    public boolean isPlayer() { return player.isPresent(); }
}
