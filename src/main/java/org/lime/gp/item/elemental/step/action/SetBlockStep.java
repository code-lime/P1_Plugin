package org.lime.gp.item.elemental.step.action;

import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.gp.item.elemental.step.IStep;

public record SetBlockStep(IBlockData block) implements IStep {
    @Override public void execute(Player player, Vector position) {
        if (!(player instanceof CraftPlayer cplayer)) return;
        BlockPosition pos = new BlockPosition(position.getBlockX(), position.getBlockY(), position.getBlockZ());
        EntityPlayer handler = cplayer.getHandle();
        World world = handler.level;
        IBlockData data = world.getBlockState(pos);
        if (!data.canBeReplaced()) return;
        world.setBlock(pos, block, Block.UPDATE_ALL);
    }
}
