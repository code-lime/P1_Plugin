package org.lime.gp.item.elemental.step.action;

import com.google.gson.JsonObject;
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
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.lime;
import org.lime.system.utils.MathUtils;

import java.util.*;
import java.util.stream.Collectors;

@Step(name = "block.fake")
public final class FakeBlockStep extends IBlockStep<FakeBlockStep> {
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

    public FakeBlockStep parse(JsonObject json) {
        return new FakeBlockStep(
                Material.valueOf(json.get("material").getAsString()),
                json.has("states")
                        ? json.getAsJsonObject("states")
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, kv -> kv.getValue().getAsString()))
                        : Collections.emptyMap(),
                MathUtils.getVector(json.get("radius").getAsString()),
                json.get("self").getAsBoolean(),
                json.get("undo_sec").getAsFloat(),
                json.get("force").getAsBoolean()
        );
    }
    /*@Override public JObject docs(IDocsLink docs) {
        return JObject.of(
                JProperty.require(IName.raw("material"), IJElement.link(docs.vanillaMaterial()), IComment.text("Тип блока")),
                JProperty.optional(IName.raw("states"), IJElement.anyObject(
                        JProperty.require(IName.raw("KEY"), IJElement.raw("VALUE"))
                ), IComment.text("Параметры блока")),
                JProperty.require(IName.raw("radius"), IJElement.link(docs.vector()), IComment.join(
                        IComment.text("Игроки, находящиеся в данном радиус увидят влияние. Если радиус равен "),
                        IComment.raw("0 0 0"),
                        IComment.text(" то влияние увидит только текущий игрок")
                )),
                JProperty.require(IName.raw("self"), IJElement.bool(), IComment.text("Видит ли текущий игрок влияние")),
                JProperty.require(IName.raw("undo_sec"), IJElement.raw(5.5), IComment.text("Время, через которое влияние пропадет")),
                JProperty.require(IName.raw("force"), IJElement.bool(), IComment.text("Влияет ли влияние на незаменяемые блоки (камень, земля)"))
        );
    }*/
}
