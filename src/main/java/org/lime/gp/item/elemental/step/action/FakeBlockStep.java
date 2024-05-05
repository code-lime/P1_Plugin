package org.lime.gp.item.elemental.step.action;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.settings.use.target.ILocationTarget;
import org.lime.gp.item.settings.use.target.PlayerTarget;
import org.lime.gp.lime;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.system.utils.MathUtils;

import javax.annotation.Nullable;
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

    @Override public void execute(ILocationTarget target, DataContext context, Transformation location) {
        if (!(target.getWorld() instanceof CraftWorld world)) return;
        @Nullable Player targetPlayer = target.castToPlayer().map(PlayerTarget::getPlayer).orElse(null);

        Vector point = MathUtils.convert(location.getTranslation());
        WorldServer worldServer = world.getHandle();
        BlockPosition position = new BlockPosition(point.getBlockX(), point.getBlockY(), point.getBlockZ());
        PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange(position, block);

        IBlockData data = worldServer.getBlockState(position);
        if (!force && !data.canBeReplaced()) return;

        List<UUID> undoUUIDs = new ArrayList<>();
        if (radius.isZero()) {
            if (!self || targetPlayer == null) return;
            if (PacketManager.sendPacket(targetPlayer, packet))
                undoUUIDs.add(targetPlayer.getUniqueId());
        } else {
            world.getNearbyPlayers(point.toLocation(world), radius.getX(), radius.getY(), radius.getZ()).forEach(other -> {
                if (!self && other == targetPlayer) return;
                if (PacketManager.sendPacket(other, packet))
                    undoUUIDs.add(other.getUniqueId());
            });
        }
        if (undoUUIDs.isEmpty()) return;
        lime.once(() -> {
            PacketPlayOutBlockChange undoPacket = new PacketPlayOutBlockChange(worldServer, position);
            undoUUIDs.forEach(uuid -> PacketManager.sendPacket(Bukkit.getPlayer(uuid), undoPacket));
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
    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
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
        ), IComment.text("Создает временный пакетный блок"));
    }
}
