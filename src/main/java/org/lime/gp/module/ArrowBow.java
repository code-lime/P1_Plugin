package org.lime.gp.module;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.lime.display.Displays;
import org.lime.display.Passenger;
import org.lime.gp.lime;

import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.ItemStack;
import org.lime.gp.player.module.HandCuffs;
import org.lime.gp.player.module.drugs.Drugs;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.plugin.CoreElement;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ArrowBow implements Listener {
    public static CoreElement create() {
        return CoreElement.create(ArrowBow.class)
                .withInstance()
                .withInit(ArrowBow::init);
    }

    private static final NamespacedKey BOW_KEY = new NamespacedKey(lime._plugin, "arrow_bow");
    private static boolean isCanShoot(Entity entity) {
        return !entity.isInsideVehicle() && !Passenger.hasVehicle(entity.getEntityId());
    }
    private static void init() {
        lime.repeatTicks(ArrowBow::updateLock, 1);
    }

    private static final List<Material> blackListItems = List.of(Material.BOW, Material.CROSSBOW);
    private static void updateLock() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (isCanShoot(player)) return;
            Drugs.lockArmsTick(player, item -> blackListItems.contains(item.getType()));
        });
    }

    @EventHandler private static void on(ProjectileLaunchEvent e) {
        if (e.getEntity().getShooter() instanceof LivingEntity shooter) {
            setBowItem(e.getEntity(), shooter.getActiveItem());
        }
    }
    @EventHandler private static void on(EntityShootBowEvent e) {
        if (isCanShoot(e.getEntity())) return;
        e.setCancelled(true);
    }

    private static ItemStack getBowItem(PersistentDataContainer container) {
        if (!container.has(BOW_KEY)) return ItemStack.EMPTY;
        return CraftItemStack.asNMSCopy(org.bukkit.inventory.ItemStack.deserializeBytes(container.get(BOW_KEY, PersistentDataType.BYTE_ARRAY)));
    }
    public static ItemStack getBowItem(Projectile projectile) {
        return getBowItem(projectile.getPersistentDataContainer());
    }
    public static ItemStack getBowItem(IProjectile projectile) {
        return getBowItem(projectile.getBukkitEntity().getPersistentDataContainer());
    }

    public static void setBowItem(Projectile projectile, org.bukkit.inventory.ItemStack item) {
        if (item.getType().isAir()) return;
        PersistentDataContainer container = projectile.getPersistentDataContainer();
        container.set(BOW_KEY, PersistentDataType.BYTE_ARRAY, item.serializeAsBytes());
    }
    public static void setBowItem(Projectile projectile, ItemStack item) {
        setBowItem(projectile, CraftItemStack.asBukkitCopy(item));
    }
}
