package org.lime.gp.block.component.data.voice;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.game.PacketPlayOutEntity;
import net.minecraft.network.protocol.game.PacketPlayOutNamedEntitySpawn;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.CraftServer;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.lime.gp.display.DisplayManager;
import org.lime.gp.display.ObjectDisplay;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.module.TimeoutData;
import org.lime.gp.player.voice.DistanceData;
import org.lime.gp.player.voice.Radio;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class RadioDisplay extends ObjectDisplay<RadioDisplay.RadioData, EntityPlayer> implements Radio.RadioElement {
    @Override public boolean hasLevel(int level) { return data.level == level; }
    @Override public UUID unique() { return uuid; }
    @Override public UUID playUUID() { return uuid; }
    @Override public boolean isDistance(Location location, double total_distance) { return Radio.RadioElement.isDistance(location, location(), total_distance); }
    @Override public Collection<Player> listeners() { return getShowPlayers(); }
    @Override public short distance() { return data.distance; }

    public static final class RadioData extends TimeoutData.ITimeout {
        private final Location location;
        private final short distance;
        private final short max_distance;
        private final int level;

        public RadioData(Location location, org.lime.gp.player.voice.RadioData radioData, DistanceData distanceData) {
            super(5);
            this.location = location;
            this.distance = distanceData.distance;
            this.max_distance = distanceData.max_distance;
            this.level = radioData.level;
        }
    }
    private final UUID uuid;
    private RadioData data;
    @Override public double getDistance() { return data.max_distance; }
    @Override public Location location() { return data.location; }

    @Override public void update(RadioData data, double delta) {
        this.data = data;
        super.update(data, delta);
    }
    @Override protected void sendData(Player player, boolean child) {
        PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook relMoveLook = new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(entityID, (short)0, (short)0, (short)0, (byte)0, (byte)0, true);
        PacketPlayOutNamedEntitySpawn ppones = new PacketPlayOutNamedEntitySpawn(entity);
        PacketPlayOutPlayerInfo ppopi_add = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entity);
        PacketPlayOutPlayerInfo ppopi_del = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entity);

        PacketManager.sendPackets(player, ppopi_add, ppones, relMoveLook, ppopi_del);
        super.sendData(player, child);
    }

    protected RadioDisplay(UUID uuid, RadioData data) {
        this.uuid = uuid;
        this.data = data;
        postInit();
    }

    @Override protected EntityPlayer createEntity(Location location) {
        WorldServer world = ((CraftWorld)location.getWorld()).getHandle();
        EntityPlayer fakePlayer = new EntityPlayer(
                ((CraftServer) Bukkit.getServer()).getServer(),
                world,
                new GameProfile(uuid, ".")
        );
        fakePlayer.setInvisible(true);
        fakePlayer.setInvulnerable(true);
        fakePlayer.moveTo(location.getX(), location.getY(), location.getZ(), 0, 0);
        return fakePlayer;
    }

    public static class RadioManager extends DisplayManager<UUID, RadioData, RadioDisplay> implements Radio.RadioListener {
        @Override public boolean isFast() { return true; }
        @Override public boolean isAsync() { return true; }

        @Override public Map<UUID, RadioData> getData() { return TimeoutData.map(RadioData.class); }
        @Override public RadioDisplay create(UUID uuid, RadioData data) { return new RadioDisplay(uuid, data); }

        @Override public Stream<Radio.RadioElement> elements() { return this.getDisplays().values().stream().map(v -> v); }
    }
    public static RadioManager manager() { return new RadioManager(); }
}