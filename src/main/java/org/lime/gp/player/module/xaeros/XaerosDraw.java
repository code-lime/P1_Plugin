package org.lime.gp.player.module.xaeros;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.lime.gp.lime;
import org.lime.gp.player.module.xaeros.packet.*;
import org.lime.plugin.CoreElement;
import org.lime.system.json;
import org.lime.system.utils.RandomUtils;

import java.util.*;

public class XaerosDraw implements Listener {
    public static CoreElement create() {
        return CoreElement.create(XaerosDraw.class)
                .withInit(XaerosDraw::init)
                .withInstance()
                .<JsonObject>addConfig("xaeroworldmap", v -> v
                        .withDefault(json.object()
                                .add("server_id", SERVER_ID)
                                .build()
                        )
                        .withInvoke(json -> {
                            SERVER_ID = json.get("server_id").getAsInt();
                        })
                );
    }

    public static boolean allowFilter(Player player) { return player.isOp(); }

    private static int SERVER_ID = RandomUtils.rand(10000, 10000000);
    private static void init() { lime.repeat(XaerosDraw::update, 1); }
    private static final HashSet<Player> syncToPlayers = new HashSet<>();
    private static final HashMap<UUID, Boolean> lastSendPlayers = new HashMap<>();

    private static void update() {
        List<IOutPacket> packets = new ArrayList<>();
        HashSet<Player> syncToPlayers = new HashSet<>();
        lastSendPlayers.entrySet().forEach(kv -> kv.setValue(false));
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (allowFilter(player)) syncToPlayers.add(player);
            UUID uuid = player.getUniqueId();
            lastSendPlayers.compute(uuid, (k, v) -> true);
            packets.add(new PacketOutUpdatePlayer(player));
        });
        lastSendPlayers.entrySet().removeIf(kv -> {
            if (kv.getValue()) return false;
            packets.add(new PacketOutRemovePlayer(kv.getKey()));
            return true;
        });
        XaerosDraw.syncToPlayers.removeIf(player -> {
            if (syncToPlayers.contains(player)) return false;
            XaerosProtocol.broadcastToAll(player, PacketOutClearAllPlayer.Instance);
            return true;
        });
        XaerosDraw.syncToPlayers.addAll(syncToPlayers);
        XaerosDraw.syncToPlayers.forEach(player -> packets.forEach(packet -> {
            if (packet instanceof PacketOutUpdatePlayer poup && poup.is(player)) return;
            XaerosProtocol.broadcastToAll(player, packet);
        }));
    }
    @EventHandler private static void on(PlayerRegisterChannelEvent e) {
        Player player = e.getPlayer();
        if (allowFilter(player))
            XaerosProtocol.findChannel(e.getChannel())
                    .ifPresent(channel -> channel.send(List.of(player), List.of(
                            new HandshakePacket(2),
                            new PacketOutRules(true, true, true),
                            new PacketOutWorld(SERVER_ID)
                    )));
    }
    @EventHandler private static void on(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        if (allowFilter(player))
            XaerosProtocol.broadcastToAll(player, List.of(
                    new HandshakePacket(2),
                    new PacketOutRules(true, true, true),
                    new PacketOutWorld(SERVER_ID)
            ));
    }
}














