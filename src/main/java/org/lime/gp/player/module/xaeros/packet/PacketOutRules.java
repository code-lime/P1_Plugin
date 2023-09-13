package org.lime.gp.player.module.xaeros.packet;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketDataSerializer;

public final class PacketOutRules implements IOutPacket {
    private final boolean allowCaveModeOnServer;
    private final boolean allowNetherCaveModeOnServer;
    private final boolean allowRadarOnServer;

    public PacketOutRules(boolean allowCaveModeOnServer, boolean allowNetherCaveModeOnServer, boolean allowRadarOnServer) {
        this.allowCaveModeOnServer = allowCaveModeOnServer;
        this.allowNetherCaveModeOnServer = allowNetherCaveModeOnServer;
        this.allowRadarOnServer = allowRadarOnServer;
    }

    @Override public void write(PacketDataSerializer serializer) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.putBoolean("cm", this.allowCaveModeOnServer);
        nbt.putBoolean("ncm", this.allowNetherCaveModeOnServer);
        nbt.putBoolean("r", this.allowRadarOnServer);
        serializer.writeNbt(nbt);
    }
}
