package org.lime.gp.player.module.xaeros.packet;

import net.minecraft.network.PacketDataSerializer;

public final class PacketOutClearAllPlayer implements IOutPacket {
    public static final PacketOutClearAllPlayer Instance = new PacketOutClearAllPlayer();
    private PacketOutClearAllPlayer(){}
    @Override public void write(PacketDataSerializer serializer) {}
}
