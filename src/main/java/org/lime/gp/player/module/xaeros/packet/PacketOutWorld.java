package org.lime.gp.player.module.xaeros.packet;

import net.minecraft.network.PacketDataSerializer;

public class PacketOutWorld implements IOutPacket {
    private final int serverId;
    public PacketOutWorld(int serverId) { this.serverId = serverId; }
    @Override public void write(PacketDataSerializer serializer) { serializer.writeInt(serverId); }
}
