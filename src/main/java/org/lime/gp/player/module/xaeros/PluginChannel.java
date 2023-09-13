package org.lime.gp.player.module.xaeros;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.lime.gp.lime;
import org.lime.gp.player.module.xaeros.packet.IOutPacket;

import java.util.ArrayList;
import java.util.List;

public class PluginChannel {
    private final String key;
    public PluginChannel(String key) { this.key = key; }

    public boolean is(String key) { return this.key.equals(key); }

    public void register() {
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(lime._plugin, key);
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(lime._plugin, key, (channel, player, raw) -> {
            if (!channel.equals(key)) return;
            XaerosProtocol.toPacket(raw).ifPresent(packet -> packet.handle(this, player));
        });
    }

    public void send(Player player, IOutPacket packet) {
        sendRaw(player, XaerosProtocol.toRaw(packet));
    }
    public void send(Iterable<Player> players, Iterable<IOutPacket> packets) {
        List<byte[]> rawList = new ArrayList<>();
        packets.forEach(packet -> rawList.add(XaerosProtocol.toRaw(packet)));
        sendRawList(players, rawList);
    }
    public static void broadcast(Iterable<PluginChannel> channels, Player player, IOutPacket packet) {
        byte[] raw = XaerosProtocol.toRaw(packet);
        channels.forEach(channel -> channel.sendRaw(player, raw));
    }
    public static void broadcast(Iterable<PluginChannel> channels, Iterable<Player> players, Iterable<IOutPacket> packets) {
        List<byte[]> rawList = new ArrayList<>();
        packets.forEach(packet -> rawList.add(XaerosProtocol.toRaw(packet)));
        channels.forEach(channel -> channel.sendRawList(players, rawList));
    }
    private void sendRaw(Player player, byte[] raw) {
        player.sendPluginMessage(lime._plugin, key, raw);
    }
    private void sendRawList(Iterable<Player> players, Iterable<byte[]> rawList) {
        players.forEach(player -> rawList.forEach(raw -> sendRaw(player, raw)));
    }

    @Override public String toString() { return key; }
}
