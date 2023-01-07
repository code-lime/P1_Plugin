package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;

import javax.annotation.Nullable;
import java.util.Optional;

public record BlockSkullPlaceInfo(BlockLimeSkull skull,
                                  World world,
                                  BlockPosition pos,
                                  IBlockData state,
                                  Optional<EntityLiving> placer,
                                  ItemStack item) {
    public BlockSkullPlaceInfo(BlockLimeSkull skull, World world, BlockPosition pos, IBlockData state, @Nullable EntityLiving placer, ItemStack item) {
        this(skull, world, pos, state, Optional.ofNullable(placer), item);
    }
}
