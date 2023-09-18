package org.lime.gp.module.npc.display;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.lime.display.Displays;
import org.lime.display.EditedDataWatcher;
import org.lime.display.ObjectDisplay;
import org.lime.display.models.display.BaseChildDisplay;
import org.lime.display.models.display.ChildEntityDisplay;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.lime;
import org.lime.gp.module.DrawText;
import org.lime.gp.module.npc.EPlayerModule;
import org.lime.gp.module.npc.eplayer.IEPlayer;
import org.lime.gp.module.npc.eplayer.Pose;
import org.lime.packetwrapper.WrapperPlayServerEntityTeleport;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class EPlayerDisplay extends ObjectDisplay<IEPlayer, EntityPlayer> {
    private static final DataWatcherObject<EntityPose> DATA_POSE = EditedDataWatcher.getDataObject(Entity.class, "DATA_POSE");
    @Override public double getDistance() { return 40; }
    public double getTargetDistance() { return 7; }
    public IEPlayer npc() { return npc; }

    private final IEPlayer npc;
    public final List<Pair<EnumItemSlot, ItemStack>> equipment;

    private Location target = null;

    @Override public boolean isFilter(Player player) { return npc.isShow(player.getUniqueId()) && ExtMethods.isPlayerLoaded(player); }
    @Override public Location location() { return getByTarget(target, null); }

    private Location getByTarget(Location target, Double minDistance) {
        Location location = super.location();
        if (target == null || location.getWorld() != target.getWorld()) return location;
        if (minDistance != null && location.distance(target) > minDistance) return location;
        return location.clone().setDirection(target.toVector().subtract(location.toVector()));
    }

    public Stream<DrawText.IShow> nickList() { return Stream.of(new ShowNickName(this), new ShowNone(this)); }

    public EPlayerDisplay(IEPlayer npc) {
        super(npc.location());
        this.npc = npc;
        this.equipment = ChildEntityDisplay.toPacketData(npc.createEquipment());
        BaseChildDisplay<?, IEPlayer, ?> sitParent;
        if (npc.pose() == Pose.SIT) sitParent = preInitDisplay(lime.models.builder().block().display(this));
        else sitParent = null;
        postInit();
        if (sitParent != null) Displays.addPassengerID(sitParent.entityID, this.entityID);
    }

    @Override protected void sendData(Player player, boolean child) {
        PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook relMoveLook = new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(entityID, (short) 0, (short) 0, (short) 0, (byte) 0, (byte) 0, true);
        PacketPlayOutNamedEntitySpawn ppones = new PacketPlayOutNamedEntitySpawn(entity);
        ClientboundPlayerInfoUpdatePacket ppopi_add = new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.a.ADD_PLAYER, entity);
        ClientboundPlayerInfoRemovePacket ppopi_del = new ClientboundPlayerInfoRemovePacket(Collections.singletonList(entity.getUUID()));
        PacketPlayOutEntityEquipment ppoee = new PacketPlayOutEntityEquipment(entityID, equipment);

        Boolean single = npc.single();
        if (single != null && single) {
            PacketPlayOutEntityTeleport movePacket = new PacketPlayOutEntityTeleport(entity);
            PacketPlayOutEntityHeadRotation headPacket = new PacketPlayOutEntityHeadRotation(entity, (byte) MathHelper.floor((entity.getYRot() % 360.0F) * 256.0F / 360.0F));
            lime.nextTick(() -> PacketManager.sendPackets(player, movePacket, headPacket));
        }

        PacketManager.sendPackets(player, ppopi_add, ppones, relMoveLook);
        PacketPlayOutEntityMetadata packet = getDataWatcherPacket(player).orElse(null);
        lime.once(() -> PacketManager.sendPackets(player, ppopi_add, packet, ppoee), 0.5);
        lime.once(() -> PacketManager.sendPackets(player, ppopi_del, packet), 5);
        super.sendData(player, child);
    }

    public static final byte PLAYER_FULL_PARTS = Arrays.stream(PlayerModelPart.values()).map(PlayerModelPart::getMask).reduce(0, (a, b) -> a | b).byteValue();

    @Override protected EntityPlayer createEntity(Location location) {
        UUID fakePlayerUUID = EPlayerModule.createUUID();

        WorldServer world = ((CraftWorld) location.getWorld()).getHandle();
        EntityPlayer fakePlayer = new EntityPlayer(
                ((CraftServer) Bukkit.getServer()).getServer(),
                world,
                npc.setSkin(new GameProfile(fakePlayerUUID, ""))
        );
        DataWatcher entityData = fakePlayer.getEntityData();
        entityData.set(EntityHuman.DATA_PLAYER_MODE_CUSTOMISATION, PLAYER_FULL_PARTS);
        if (npc.pose() == Pose.CRAWL)
            entityData.set(DATA_POSE, EntityPose.SWIMMING);

        fakePlayer.moveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        return fakePlayer;
    }
    @Override protected void editDataWatcher(Player player, EditedDataWatcher dataWatcher) {
        dataWatcher.setCustom(EntityHuman.DATA_PLAYER_MODE_CUSTOMISATION, Byte.MAX_VALUE);
        super.editDataWatcher(player, dataWatcher);
    }
    @Override public void update(IEPlayer npc, double delta) {
        if (npc != this.npc) {

        }
        Boolean single = npc.single();
        if (single != null && single) {
            Player near = this.getNearShow(getTargetDistance(), p -> p.getGameMode() != GameMode.SPECTATOR);
            target = near == null ? null : near.getLocation();
        } else {
            target = null;
        }

        Location location = this.location();
        if (single == null || single) {
            if (location.equals(last_location)) {
                super.update(npc, delta);
                this.invokeAll(this::sendDataChild);
                return;
            }
            entity.moveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            super.update(npc, delta);
            PacketPlayOutEntityTeleport movePacket = new PacketPlayOutEntityTeleport(entity);
            PacketPlayOutEntityHeadRotation headPacket = new PacketPlayOutEntityHeadRotation(entity, (byte) MathHelper.floor((location.getYaw() % 360.0F) * 256.0F / 360.0F));
            this.invokeAll(player -> PacketManager.sendPackets(player, movePacket, headPacket));
        } else {
            super.update(npc, delta);
            this.invokeAll(player -> {
                Location _location = getByTarget(player.getLocation(), getTargetDistance());
                WrapperPlayServerEntityTeleport wpset = new WrapperPlayServerEntityTeleport();
                wpset.setEntityID(entityID);
                wpset.setX(_location.getX());
                wpset.setY(_location.getY());
                wpset.setZ(_location.getZ());
                wpset.setYaw(_location.getYaw());
                wpset.setPitch(_location.getPitch());
                wpset.setOnGround(entity.onGround());
                wpset.sendPacket(player);

                PacketPlayOutEntityHeadRotation headPacket = new PacketPlayOutEntityHeadRotation(entity, (byte) MathHelper.floor((_location.getYaw() % 360.0F) * 256.0F / 360.0F));
                PacketManager.sendPacket(player, headPacket);
            });
        }
        this.invokeAll(this::sendDataChild);
    }

    public void click(Player player, boolean isShift) {
        npc.click(player, isShift);
    }

    @Override
    public void hide(Player player) {
        super.hide(player);
        ClientboundPlayerInfoRemovePacket ppopi = new ClientboundPlayerInfoRemovePacket(Collections.singletonList(entity.getUUID()));
        PacketManager.sendPackets(player, ppopi);
    }
}
