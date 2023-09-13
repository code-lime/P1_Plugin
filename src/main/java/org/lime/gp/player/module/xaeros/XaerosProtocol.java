package org.lime.gp.player.module.xaeros;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketDataSerializer;
import org.bukkit.entity.Player;
import org.lime.gp.player.module.xaeros.packet.*;
import org.lime.plugin.CoreElement;
import org.lime.system;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class XaerosProtocol {
    public static final PluginChannel WorldMap = new PluginChannel("xaeroworldmap:main");
    public static final PluginChannel MiniMap = new PluginChannel("xaerominimap:main");

    public static Optional<PluginChannel> findChannel(String key) {
        return WorldMap.is(key) ? Optional.of(WorldMap) : MiniMap.is(key) ? Optional.of(MiniMap) : Optional.empty();
    }

    private static final HashMap<Class<? extends IOutPacket>, Integer> packetToId = new HashMap<>();
    private static final HashMap<Integer, system.Func1<PacketDataSerializer, IInPacket>> idToPacket = new HashMap<>();

    public static CoreElement create() {
        return CoreElement.create(XaerosProtocol.class)
                .withInit(XaerosProtocol::init);
    }

    private static void init() {
        /*
        messageHandler.register(0, LevelMapProperties.class, null, new LevelMapPropertiesConsumer(), LevelMapProperties::read, LevelMapProperties::write);
        messageHandler.register(1, HandshakePacket.class, new HandshakePacket.ServerHandler(), new HandshakePacket.ClientHandler(), HandshakePacket::read, HandshakePacket::write);
        messageHandler.register(2, ClientboundTrackedPlayerPacket.class, null, new ClientboundTrackedPlayerPacket.Handler(), ClientboundTrackedPlayerPacket::read, ClientboundTrackedPlayerPacket::write);
        messageHandler.register(3, ClientboundPlayerTrackerResetPacket.class, null, new ClientboundPlayerTrackerResetPacket.Handler(), ClientboundPlayerTrackerResetPacket::read, ClientboundPlayerTrackerResetPacket::write);
        messageHandler.register(4, ClientboundRulesPacket.class, null, new ClientboundRulesPacket.ClientHandler(), ClientboundRulesPacket::read, ClientboundRulesPacket::write);
        */

        packetToId.put(PacketOutWorld.class, 0);
        packetToId.put(HandshakePacket.class, 1);
        packetToId.put(PacketOutUpdatePlayer.class, 2);
        packetToId.put(PacketOutRemovePlayer.class, 2);
        packetToId.put(PacketOutClearAllPlayer.class, 3);
        packetToId.put(PacketOutRules.class, 4);

        idToPacket.put(1, HandshakePacket::read);

        WorldMap.register();
        MiniMap.register();
    }

    public static Optional<IInPacket> toPacket(byte[] raw) {
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.wrappedBuffer(raw));
        int packetId = serializer.readByte();
        system.Func1<PacketDataSerializer, IInPacket> creator = idToPacket.get(packetId);
        return creator == null ? Optional.empty() : Optional.of(creator.invoke(serializer));
    }
    public static byte[] toRaw(IOutPacket packet) {
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer());
        serializer.writeByte(packetToId.get(packet.getClass()));
        packet.write(serializer);
        return serializer.array();
    }

    public static void broadcastToAll(Player player, IOutPacket packet) {
        PluginChannel.broadcast(List.of(WorldMap, MiniMap), player, packet);
    }
    public static void broadcastToAll(Player player, Iterable<IOutPacket> packet) {
        broadcastToAll(Collections.singletonList(player), packet);
    }
    public static void broadcastToAll(Iterable<Player> player, Iterable<IOutPacket> packet) {
        PluginChannel.broadcast(List.of(WorldMap, MiniMap), player, packet);
    }
}
