package org.lime.gp.player.module;

import dev.geco.gsit.api.GSitAPI;
import dev.geco.gsit.api.event.PrePlayerGetUpPoseEvent;
import dev.geco.gsit.objects.GetUpReason;

import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.list.*;
import org.lime.gp.lime;
import org.lime.gp.module.damage.EntityDamageByPlayerEvent;
import org.lime.gp.player.module.drugs.Drugs;
import org.lime.gp.player.perm.Perms;
import org.lime.system;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.UUID;

public class Knock implements Listener {
    private static final long KNOCK_TIME_MS = 10 * 1000;
    public static CoreElement create() {
        return CoreElement.create(Knock.class)
                .withInit(Knock::init)
                .withInstance();
    }

    private static final HashMap<UUID, system.Toast2<Long, Location>> knockCooldown = new HashMap<>();
    public static void init() {
        lime.repeat(Knock::update, 0.1);
        lime.repeatTicks(Knock::updateLock, 1);
    }
    public static void knock(Player player) {
        if (lime.isSit(player) || lime.isLay(player)) return;
        system.Toast2<Long, Location> knock = knockCooldown.getOrDefault(player.getUniqueId(), null);
        if (knock == null) knockCooldown.put(player.getUniqueId(), system.toast(System.currentTimeMillis() + KNOCK_TIME_MS, player.getLocation().clone()));
        else knock.val0 = System.currentTimeMillis() + KNOCK_TIME_MS;
    }
    public static void unKnock(Player player) {
        knockCooldown.remove(player.getUniqueId());
        lime.unSit(player);
    }
    public static boolean isKnockOrLay(Player player) {
        return knockCooldown.containsKey(player.getUniqueId()) || lime.isSit(player);
    }
    public static boolean isKnock(UUID uuid) {
        return knockCooldown.containsKey(uuid);
    }
    public static void updateLock() {
        knockCooldown.keySet().forEach(uuid -> Drugs.lockArmsTick(Bukkit.getPlayer(uuid)));
    }
    public static void update() {
        long now = System.currentTimeMillis();
        knockCooldown.entrySet().removeIf(kv -> {
            Player player = Bukkit.getPlayer(kv.getKey());
            if (player == null) return true;
            system.Toast2<Long, Location> knock = kv.getValue();
            if (now > knock.val0) return true;
            if (lime.isSit(player)) return false;
            if (lime.isLay(player)) return false;
            Location location = knock.val1.clone();
            GSitAPI.createSeat(location.getBlock(), player, true, 0, 0, 0, location.getYaw(), true);
            return false;
        });
    }
    @EventHandler public static void on(PrePlayerGetUpPoseEvent e) {
        if (e.getPoseSeat().getPose() == Pose.SITTING && e.getReason() == GetUpReason.GET_UP && isKnock(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler public static void on(EntityDamageByPlayerEvent e) {
        e.getEntityPlayer()
                .ifPresent(target -> Items.getItemCreator(e.getItem())
                    .filter(v -> e.getBase().getFinalDamage() != 0)
                    .map(v -> v instanceof ItemCreator _v ? _v : null)
                    .filter(v -> v.getOptional(BatonSetting.class).map(_v -> system.rand_is(_v.chance)).orElse(false))
                    .filter(v -> Perms.getCanData(e.getDamageOwner().getUniqueId()).isCanUse(v.getKey()))
                    .ifPresent(v -> Knock.knock(target))
                );
    }
    @EventHandler(priority = EventPriority.LOWEST) public static void on(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        if (!knockCooldown.containsKey(e.getPlayer().getUniqueId())) return;
        e.setCancelled(true);
    }


    @EventHandler public static void on(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player damager)) return;
        if (!knockCooldown.containsKey(damager.getUniqueId())) return;
        e.setCancelled(true);
    }
    @EventHandler public static void on(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!knockCooldown.containsKey(player.getUniqueId())) return;
        e.setCancelled(true);
    }
    @EventHandler public static void on(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();
        if (!knockCooldown.containsKey(player.getUniqueId())) return;
        e.setCancelled(true);
    }
}






















