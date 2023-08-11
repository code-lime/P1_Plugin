package org.lime.gp.item.elemental.step.action;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.display.transform.LocalLocation;

import java.util.Map;

public class FakeBlockStep extends IBlockStep {
    private final Vector radius;
    private final boolean self;

    public FakeBlockStep(IBlockData block, Vector radius, boolean self) {
        super(block);
        this.radius = radius;
        this.self = self;
    }
    public FakeBlockStep(Material material, Map<String, String> states, Vector radius, boolean self) {
        super(material, states);
        this.radius = radius;
        this.self = self;
    }

    @Override public void execute(Player player, LocalLocation location) {
        if (!(player instanceof CraftPlayer cplayer)) return;
        EntityPlayer handler = cplayer.getHandle();
        PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange(new BlockPosition(location.blockX(), location.blockY(), location.blockZ()), block);

        if (radius.isZero()) {
            if (!self) return;

            PlayerConnection connection = handler.connection;
            if (connection == null) return;
            connection.send(packet);
        }

        World world = player.getWorld();
        world.getNearbyPlayers(location.position().toLocation(world), radius.getX(), radius.getY(), radius.getZ()).forEach(other -> {
            if (!self && other == player) return;
            if (!(other instanceof CraftPlayer cother)) return;
            PlayerConnection connection = cother.getHandle().connection;
            if (connection == null) return;
            connection.send(packet);
        });
    }
}
