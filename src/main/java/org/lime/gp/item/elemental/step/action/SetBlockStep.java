package org.lime.gp.item.elemental.step.action;

import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.lime.display.transform.LocalLocation;

import java.util.Map;

public class SetBlockStep extends IBlockStep {
    private final boolean force;
    public SetBlockStep(IBlockData block, boolean force) {
        super(block);
        this.force = force;
    }
    public SetBlockStep(Material material, Map<String, String> states, boolean force) {
        super(material, states);
        this.force = force;
    }

    @Override public void execute(Player player, LocalLocation location) {
        if (!(player instanceof CraftPlayer cplayer)) return;
        BlockPosition pos = new BlockPosition(location.blockX(), location.blockY(), location.blockZ());
        EntityPlayer handler = cplayer.getHandle();
        World world = handler.level();
        IBlockData data = world.getBlockState(pos);
        if (!force && !data.canBeReplaced()) return;
        world.setBlock(pos, block, Block.UPDATE_ALL);
    }
}
