package org.lime.gp.item.projectile;

import com.comphenix.protocol.events.PacketEvent;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.EntityThrownTrident;
import net.minecraft.world.entity.projectile.EntityTridentBaseDamageEvent;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftTrident;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.entity.Trident;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.lime.core;
import org.lime.display.Displays;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.ProjectileSetting;
import org.lime.gp.lime;
import org.lime.gp.player.module.Death;
import org.lime.gp.player.module.HandCuffs;
import org.lime.gp.player.module.Knock;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.sound.IReplaceInfo;
import org.lime.gp.sound.Sounds;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProjectileItem implements Listener {
    public static core.element create() {
        return core.element.create(ProjectileItem.class)
                .withInstance()
                .withInit(ProjectileItem::init);
    }

    private static final ConcurrentHashMap<Integer, ProjectileFrame> projectileFrames = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, ProjectileTimerInfo> useTimerTicks = new ConcurrentHashMap<>();
    private static final ProjectileDisplayManager MANAGER = new ProjectileDisplayManager(projectileFrames);
    private static void init() {
        AnyEvent.addEvent("shoot.projectile",
                AnyEvent.type.owner,
                v -> v.createParam(Float::parseFloat, "[speed]").createParam(Float::parseFloat, "[divergence]"),
                (player, speed, divergence) ->
                        shoot(player.getLocation(),
                                ((CraftPlayer)player).getHandle(),
                                CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.GOLDEN_AXE)),
                                speed,
                                divergence,
                                false,
                                (byte)0,
                                Collections.emptyList()
                        ));
        lime.repeatTicks(ProjectileItem::update, ProjectileFrame.FRAME_DURATION);
        PacketManager.adapter()
                .add(PacketPlayOutSpawnEntity.class, ProjectileItem::onPacket)
                .listen();
        Displays.initDisplay(MANAGER);
        Sounds.registryReplaceEntity(id -> Optional.ofNullable(projectileFrames.get(id))
                .map(v -> IReplaceInfo.ofPos(v.location().toVector()).setTags(v.tags()))
        );
        CustomUI.addListener(new ProjectileTimer());
    }
    private static void update() {
        Set<Integer> removeList = new HashSet<>(projectileFrames.keySet());
        Bukkit.getWorlds().forEach(world -> world.getEntitiesByClass(Trident.class).forEach(trident -> {
            int id = trident.getEntityId();
            removeList.remove(id);
            projectileFrames.put(id, ProjectileFrame.of(trident));
        }));
        Bukkit.getOnlinePlayers().forEach(player -> useTimerTicks.compute(player.getUniqueId(), (uuid, info) ->
                Items.getOptional(ProjectileSetting.class, player.getInventory().getItemInMainHand())
                        .map(v -> {
                            String key = v.creator().getKey();
                            return info == null || !info.item().equals(key)
                                    ? new ProjectileTimerInfo(v.cooldown, key)
                                    : info.tick(ProjectileFrame.FRAME_DURATION);
                        })
                        .orElse(null)
        ));
        projectileFrames.keySet().removeAll(removeList);
    }

    public static int getTimerTicks(UUID uuid) {
        ProjectileTimerInfo info = useTimerTicks.get(uuid);
        return info == null ? 0 : info.ticks();
    }

    private static void onPacket(PacketPlayOutSpawnEntity packet, PacketEvent event) {
        if (!packet.getType().equals(EntityTypes.TRIDENT)) return;
        event.setCancelled(true);
    }
    private static void shoot(Location location, Entity owner, ItemStack item, float speed, float divergence, boolean pickupOwner, byte loyalty, Collection<String> tags) {
        WorldServer world = ((CraftWorld)location.getWorld()).getHandle();
        EntityThrownTrident projectile = new EntityThrownTrident(EntityTypes.TRIDENT, world);
        projectile.setPos(location.x(), location.y(), location.z());
        projectile.tridentItem = item;

        if (pickupOwner) projectile.setOwner(owner);
        else if (owner instanceof EntityHuman human) projectile.pickup = human.getAbilities().instabuild ? EntityArrow.PickupStatus.CREATIVE_ONLY : EntityArrow.PickupStatus.ALLOWED;

        if (loyalty > 0) projectile.setLoyalty(loyalty); //По конфигу возможность возврата

        float pitch = location.getPitch();
        float yaw = location.getYaw();
        float roll = 0;

        float x = -MathHelper.sin(yaw * ((float)Math.PI / 180)) * MathHelper.cos(pitch * ((float)Math.PI / 180));
        float y = -MathHelper.sin((pitch + roll) * ((float)Math.PI / 180));
        float z = MathHelper.cos(yaw * ((float)Math.PI / 180)) * MathHelper.cos(pitch * ((float)Math.PI / 180));

        projectile.getTags().addAll(tags);
        projectile.shoot(x, y, z, speed, divergence);
        world.addFreshEntity(projectile);

        projectileFrames.put(projectile.getId(), ProjectileFrame.of((CraftTrident)projectile.getBukkitEntity()));

        world.playSound(null, projectile, SoundEffects.TRIDENT_THROW, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }
    private static Location getLocation(Location eye, Location feet, float point) {
        return feet.clone().add(eye.toVector().subtract(feet.toVector()).multiply(point));
    }
    @EventHandler(priority = EventPriority.LOW) private static void on(PlayerInteractEvent e) {
        if (!(e.getPlayer() instanceof CraftPlayer player) || !e.getAction().isRightClick() || !player.isSneaking()) return;
        UUID uuid = player.getUniqueId();
        if (e.useItemInHand() == Event.Result.DENY
                || lime.isLay(player)
                || Death.isDamageLay(uuid)
                || HandCuffs.isMove(uuid)
                || Knock.isKnock(uuid)
        ) return;

        EntityPlayer handle = player.getHandle();
        ItemStack item = handle.getItemInHand(EnumHand.MAIN_HAND);
        Items.getOptional(ProjectileSetting.class, item)
                .ifPresent(projectile -> {
                    ProjectileTimerInfo info = useTimerTicks.get(uuid);
                    if (info == null || !info.item().equals(projectile.creator().getKey()) || info.ticks() > 0) return;
                    useTimerTicks.remove(uuid);
                    ItemStack _raw = item.copyWithCount(1);
                    Items.hurt(item, handle, 1, EnumItemSlot.MAINHAND);
                    shoot(getLocation(player.getEyeLocation(), player.getLocation(), projectile.height), item.isEmpty() ? null : handle, item.isEmpty() ? _raw : item.copy(), projectile.speed, projectile.divergence, projectile.pickupOwner, projectile.loyalty, projectile.tags);
                    if (!handle.getAbilities().instabuild) item.setCount(0);
                });
    }
    @EventHandler private static void on(EntityTridentBaseDamageEvent e) {
        e.setDamage(Items.getOptional(ProjectileSetting.class, e.getTrident().tridentItem).map(v -> v.damage).orElse(0f));
    }
}

















