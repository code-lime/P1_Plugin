package org.lime.gp.player.module.xaeros.packet;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketDataSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class PacketOutUpdatePlayer implements IOutPacket {
    private final UUID uuid;
    private final double x;
    private final double y;
    private final double z;
    private final String dimension;

    public boolean is(UUID uuid) { return this.uuid.equals(uuid); }
    public boolean is(Player player) { return is(player.getUniqueId()); }

    public PacketOutUpdatePlayer(UUID uuid, double x, double y, double z, String dimension) {
        this.uuid = uuid;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
    }
    public PacketOutUpdatePlayer(UUID uuid, Location location) {
        this(uuid, location.getX(), location.getY(), location.getZ(), location.getWorld().getKey().toString());
    }
    public PacketOutUpdatePlayer(Player player) {
        this(player.getUniqueId(), player.getLocation());
    }

    @Override public void write(PacketDataSerializer serializer) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.putBoolean("r", false);
        nbt.putUUID("i", uuid);
        nbt.putDouble("x", this.x);
        nbt.putDouble("y", this.y);
        nbt.putDouble("z", this.z);
        nbt.putString("d", this.dimension.toString());
        serializer.writeNbt(nbt);
    }
}
