package org.lime.gp.player.module;

import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.lime.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.Tables;
import org.lime.gp.database.rows.HouseRow;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.*;
import org.lime.gp.lime;
import org.lime.gp.player.perm.Perms;
import org.lime.gp.player.menu.MenuCreator;

import java.util.HashMap;
import java.util.UUID;

public class HandCuffs implements Listener {
    private static final PotionEffect SLOW_DIGGING = PotionEffectType.SLOW_DIGGING.createEffect(20, 100).withIcon(false).withParticles(false).withAmbient(false);
    public static core.element create() {
        return core.element.create(HandCuffs.class)
                .withInit(HandCuffs::init)
                .withInstance();
    }
    public static void init() {
        lime.repeat(HandCuffs::update, 0.1);
        lime.repeatTicks(HandCuffs::updateLock, 1);
    }

    private static final HashMap<UUID, MoveInfo> moveList = new HashMap<>();
    public static class MoveInfo {
        public final UUID owner;

        public MoveInfo(Player owner, Player player) {
            this.owner = owner.getUniqueId();
        }
    }

    public static void updateLock() {
        moveList.keySet().forEach(uuid -> Drugs.lockArmsTick(Bukkit.getPlayer(uuid)));
    }
    public static void update() {
        moveList.entrySet().removeIf(kv -> {
            Player other = Bukkit.getPlayer(kv.getKey());
            Player owner = Bukkit.getPlayer(kv.getValue().owner);
            if (other == null) {
                if (owner != null) showMenu(owner, owner.getUniqueId(), kv.getKey(), false);
                return true;
            }
            if (owner == null || Death.isDamageLay(other.getUniqueId()) || Death.isDamageLay(owner.getUniqueId())) {
                showMenu(other, kv.getValue().owner, kv.getKey(), false);
                return true;
            }
            if (TargetMove.isTarget(other.getUniqueId())) TargetMove.unTarget(other.getUniqueId());
            Knock.unKnock(other);

            Location owner_loc = owner.getLocation();
            Location other_loc = other.getLocation();

            Vector delta = owner_loc.toVector().subtract(other_loc.toVector());
            other.addPotionEffect(SLOW_DIGGING);
            if (delta.length() <= 3) return false;
            delta = owner_loc.toVector().add(delta.normalize().multiply(-2));
            other.teleport(new Location(owner.getWorld(), delta.getX(), delta.getY(), delta.getZ(), other_loc.getYaw(), other_loc.getPitch()));
            return false;
        });
    }
    private static void showMenu(Player player, UUID owner, UUID target, boolean isOn) {
        MenuCreator.show(player,
                "lang.police.handcuffs",
                Apply.of().add("target_uuid", target.toString()).add("owner_uuid", owner.toString()).add("key", isOn ? "on" : "off")
        );
    }
    public static boolean isMove(UUID uuid) {
        return moveList.containsKey(uuid);
    }
    public static void lock(Player owner, Player other) {
        UUID target_uuid = other.getUniqueId();
        if (moveList.containsKey(target_uuid)) return;
        moveList.put(target_uuid, new MoveInfo(owner, other));
        showMenu(owner, owner.getUniqueId(), target_uuid, true);
    }
    public static void unLock(Player other) {
        UUID target_uuid = other.getUniqueId();
        MoveInfo info = moveList.remove(target_uuid);
        if (info == null) return;
        showMenu(other, info.owner, target_uuid, false);
    }
    public static void unLockAny(Player owner) {
        UUID uuid = owner.getUniqueId();
        moveList.entrySet().removeIf(kv -> {
            if (uuid.equals(kv.getValue().owner)) {
                showMenu(owner, uuid, kv.getKey(), false);
                return true;
            } else if (uuid.equals(kv.getKey())) {
                showMenu(owner, kv.getValue().owner, kv.getKey(), false);
                return true;
            }
            return false;
        });
    }
    private static boolean inPolice(Location location) {
        if (location.getWorld() != lime.MainWorld) return false;
        for (HouseRow row : Tables.HOUSE_TABLE.getRows()) {
            if (row.inZone(location) && row.type == HouseRow.HouseType.PRISON) {
                return true;
            }
        }
        return false;
    }
    @EventHandler public static void on(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player damager)) return;
        if (!moveList.containsKey(damager.getUniqueId())) return;
        e.setCancelled(true);
    }
    @EventHandler public static void on(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!moveList.containsKey(player.getUniqueId())) return;
        e.setCancelled(true);
    }
    @EventHandler public static void on(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();
        if (moveList.containsKey(player.getUniqueId())) {
            e.setCancelled(true);
            return;
        }
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!(e.getRightClicked() instanceof Player other)) return;
        MoveInfo moveInfo = moveList.getOrDefault(other.getUniqueId(), null);
        Items.getItemCreator(e.getPlayer().getInventory().getItemInMainHand())
                .map(v -> v instanceof Items.ItemCreator _v ? _v : null)
                .ifPresent(creator -> {
                    if (moveInfo != null) {
                        if (!player.getUniqueId().equals(moveInfo.owner)) return;
                        boolean canUse = Perms.getCanData(player.getUniqueId()).isCanUse(creator.getKey());
                        if (!canUse || !creator.has(HandcuffsSetting.class)) return;
                        unLock(other);
                        return;
                    }
                    if (!Knock.isKnockOrLay(other)) return;
                    if (Death.isDamageLay(other.getUniqueId())) return;
                    boolean canUse = Perms.getCanData(player.getUniqueId()).isCanUse(creator.getKey());
                    if (canUse) {
                        if (creator.has(HandcuffsSetting.class)) HandCuffs.lock(player, other);
                        else if (creator.has(BatonSetting.class)) Search.search(player, other, !inPolice(player.getLocation()), item -> true);
                    }
                });
    }
    @EventHandler(priority = EventPriority.LOWEST) public static void on(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!moveList.containsKey(player.getUniqueId())) return;
        e.setCancelled(true);
    }
}



























