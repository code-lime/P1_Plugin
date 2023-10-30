package org.lime.gp.item.elemental.step.action;

import com.mojang.math.Transformation;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.lime;
import org.lime.system.utils.MathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FakeBlockStep extends IBlockStep {
    private final Vector radius;
    private final boolean self;
    private final float undoSec;
    private final boolean force;

    public FakeBlockStep(Material material, Map<String, String> states, Vector radius, boolean self, float undoSec, boolean force) {
        super(material, states);
        this.radius = radius;
        this.self = self;
        this.undoSec = undoSec;
        this.force = force;
    }

    @Override public void execute(Player player, DataContext context, Transformation location) {
        if (!(player instanceof CraftPlayer cplayer)) return;
        EntityPlayer handler = cplayer.getHandle();
        Vector point = MathUtils.convert(location.getTranslation());
        WorldServer worldServer = handler.serverLevel();
        BlockPosition position = new BlockPosition(point.getBlockX(), point.getBlockY(), point.getBlockZ());
        PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange(position, block);

        IBlockData data = worldServer.getBlockState(position);
        if (!force && !data.canBeReplaced()) return;

        List<UUID> undoUUIDs = new ArrayList<>();
        if (radius.isZero()) {
            if (!self) return;
            PlayerConnection connection = handler.connection;
            if (connection == null) return;
            connection.send(packet);
            undoUUIDs.add(handler.getUUID());
        } else {
            World world = player.getWorld();
            world.getNearbyPlayers(point.toLocation(world), radius.getX(), radius.getY(), radius.getZ()).forEach(other -> {
                if (!self && other == player) return;
                if (!(other instanceof CraftPlayer cother)) return;
                PlayerConnection connection = cother.getHandle().connection;
                if (connection == null) return;
                connection.send(packet);
                undoUUIDs.add(cother.getUniqueId());
            });
        }
        lime.once(() -> {
            PacketPlayOutBlockChange undoPacket = new PacketPlayOutBlockChange(worldServer, position);
            undoUUIDs.forEach(uuid -> {
                if (!(Bukkit.getPlayer(uuid) instanceof CraftPlayer cother)) return;
                PlayerConnection connection = cother.getHandle().connection;
                if (connection == null) return;
                connection.send(undoPacket);
            });
        }, undoSec);
    }
}
