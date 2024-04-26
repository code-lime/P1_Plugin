package org.lime.gp.player.module.xaeros;

import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketDataSerializer;
import org.bukkit.entity.Player;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.lime;
import org.lime.gp.player.module.xaeros.packet.*;
import org.lime.plugin.CoreElement;
import org.lime.system.json;
import org.lime.system.execute.*;
import org.lime.system.utils.RandomUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class XaerosProtocol {
    public static final PluginChannel WorldMap = new PluginChannel("xaeroworldmap:main");
    public static final PluginChannel MiniMap = new PluginChannel("xaerominimap:main");

    private static int SERVER_ID = RandomUtils.rand(10000, 10000000);
    private static boolean ENABLE = false;

    public static int serverId() {
        return SERVER_ID;
    }

    public static Optional<PluginChannel> findChannel(String key) {
        return WorldMap.is(key) ? Optional.of(WorldMap) : MiniMap.is(key) ? Optional.of(MiniMap) : Optional.empty();
    }

    private static final HashMap<Class<? extends IOutPacket>, Integer> packetToId = new HashMap<>();
    private static final HashMap<Integer, Func1<PacketDataSerializer, IInPacket>> idToPacket = new HashMap<>();

    public static CoreElement create() {
        return CoreElement.create(XaerosProtocol.class)
                .withInit(XaerosProtocol::init)
                .<JsonObject>addConfig("data/xaeroworldmap", v -> v
                        .withDefault(json.object()
                                .add("server_id", SERVER_ID)
                                .add("enable", true)
                                .build()
                        )
                        .withInvoke(json -> {
                            SERVER_ID = json.get("server_id").getAsInt();
                            ENABLE = json.get("enable").getAsBoolean();

                            if (ENABLE) {
                                WorldMap.register();
                                MiniMap.register();
                            } else {
                                WorldMap.unregister();
                                MiniMap.unregister();
                            }
                        })
                );
    }
    private static void changeEnable(boolean enable) {
        ENABLE = !ENABLE;
        lime.writeAllConfig("data/xaeroworldmap", json.format(json.object().add("server_id", SERVER_ID).add("enable", ENABLE).build()));
        if (ENABLE) {
            WorldMap.register();
            MiniMap.register();
        } else {
            WorldMap.unregister();
            MiniMap.unregister();
        }
    }

    private static void init() {
        packetToId.put(PacketOutWorld.class, 0);
        packetToId.put(HandshakePacket.class, 1);
        packetToId.put(PacketOutUpdatePlayer.class, 2);
        packetToId.put(PacketOutRemovePlayer.class, 2);
        packetToId.put(PacketOutClearAllPlayer.class, 3);
        packetToId.put(PacketOutRules.class, 4);

        idToPacket.put(1, HandshakePacket::read);

        AnyEvent.addEvent("xaeroworldmap", AnyEvent.type.owner_console, v -> v.createParam("enable", "disable"), (p, status) -> {
            switch (status) {
                case "enable" -> changeEnable(true);
                case "disable" -> changeEnable(false);
            }
        });
    }

    public static Optional<IInPacket> toPacket(byte[] raw) {
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.wrappedBuffer(raw));
        int packetId = serializer.readByte();
        Func1<PacketDataSerializer, IInPacket> creator = idToPacket.get(packetId);
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
