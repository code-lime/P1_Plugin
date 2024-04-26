package org.lime.gp.player.module.xaeros;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.lime.gp.lime;
import org.lime.gp.player.module.xaeros.packet.IOutPacket;

import java.util.ArrayList;
import java.util.List;

public class PluginChannel {
    private final String key;
    private boolean isRegistered;
    public PluginChannel(String key) { this.key = key; }

    public boolean is(String key) { return this.key.equals(key); }

    public void register() {
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(lime._plugin, key);
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(lime._plugin, key, this::onPacket);
        isRegistered = true;
    }
    public void unregister() {
        Bukkit.getServer().getMessenger().unregisterOutgoingPluginChannel(lime._plugin, key);
        Bukkit.getServer().getMessenger().unregisterIncomingPluginChannel(lime._plugin, key, this::onPacket);
        isRegistered = false;
    }
    private void onPacket(@NotNull String channel, @NotNull Player player, @NotNull byte[] raw) {
        if (!isRegistered) return;
        if (!channel.equals(key)) return;
        XaerosProtocol.toPacket(raw).ifPresent(packet -> packet.handle(this, player));
    }

    public void send(Player player, IOutPacket packet) {
        if (!isRegistered) return;
        sendRaw(player, XaerosProtocol.toRaw(packet));
    }
    public void send(Iterable<Player> players, Iterable<IOutPacket> packets) {
        if (!isRegistered) return;
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
        if (!isRegistered) return;
        player.sendPluginMessage(lime._plugin, key, raw);
    }
    private void sendRawList(Iterable<Player> players, Iterable<byte[]> rawList) {
        if (!isRegistered) return;
        players.forEach(player -> rawList.forEach(raw -> sendRaw(player, raw)));
    }

    @Override public String toString() { return key; }
}
