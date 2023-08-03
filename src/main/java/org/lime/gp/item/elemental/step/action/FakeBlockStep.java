package org.lime.gp.item.elemental.step.action;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.gp.item.elemental.step.IStep;

public record FakeBlockStep(IBlockData block) implements IStep {
    @Override public void execute(Player player, Vector position) {
        if (!(player instanceof CraftPlayer cplayer)) return;
        EntityPlayer handler = cplayer.getHandle();
        PlayerConnection connection = handler.connection;
        if (connection == null) return;
        PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange(new BlockPosition(position.getBlockX(), position.getBlockY(), position.getBlockZ()), block);
        connection.send(packet);
    }
}
