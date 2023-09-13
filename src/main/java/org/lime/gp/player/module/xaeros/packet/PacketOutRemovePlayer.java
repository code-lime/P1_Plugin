package org.lime.gp.player.module.xaeros.packet;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketDataSerializer;

import java.util.UUID;

public final class PacketOutRemovePlayer implements IOutPacket {
    private final UUID uuid;
    public PacketOutRemovePlayer(UUID uuid) { this.uuid = uuid; }

    @Override public void write(PacketDataSerializer serializer) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.putBoolean("r", true);
        nbt.putUUID("i", uuid);
        serializer.writeNbt(nbt);
    }
}
