package org.lime.gp.player.module.xaeros.packet;

import net.minecraft.network.PacketDataSerializer;

public interface IOutPacket {
    void write(PacketDataSerializer serializer);
}
