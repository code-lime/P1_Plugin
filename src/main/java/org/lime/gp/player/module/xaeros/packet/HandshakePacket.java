package org.lime.gp.player.module.xaeros.packet;

import net.minecraft.network.PacketDataSerializer;
import org.bukkit.entity.Player;
import org.lime.gp.lime;
import org.lime.gp.player.module.xaeros.PluginChannel;

public class HandshakePacket implements IOutPacket, IInPacket {
    public int networkVersion;
    public HandshakePacket(int networkVersion) { this.networkVersion = networkVersion; }

    @Override public void write(PacketDataSerializer serializer) { serializer.writeInt(this.networkVersion); }
    public static HandshakePacket read(PacketDataSerializer serializer) { return new HandshakePacket(serializer.readInt()); }

    @Override public void handle(PluginChannel channel, Player player) {
        //lime.logOP("Send handshake get with version " + networkVersion + " to " + player.getName() + "#" + channel);
    }
}
