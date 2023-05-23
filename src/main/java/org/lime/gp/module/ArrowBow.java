package org.lime.gp.module;

import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.lime.gp.lime;

import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.ItemStack;

public class ArrowBow implements Listener {
    public static org.lime.core.element create() {
        return org.lime.core.element.create(ArrowBow.class)
                .withInstance();
    }

    private static final NamespacedKey BOW_KEY = new NamespacedKey(lime._plugin, "arrow_bow");

    @EventHandler private static void on(ProjectileLaunchEvent e) {
        if (e.getEntity().getShooter() instanceof LivingEntity shooter) {
            setBowItem(e.getEntity(), shooter.getActiveItem());
        }
    }

    public static ItemStack getBowItem(Projectile projectile) {
        PersistentDataContainer container = projectile.getPersistentDataContainer();
        if (!container.has(BOW_KEY)) return ItemStack.EMPTY;
        return CraftItemStack.asNMSCopy(org.bukkit.inventory.ItemStack.deserializeBytes(container.get(BOW_KEY, PersistentDataType.BYTE_ARRAY)));
    }
    public static ItemStack getBowItem(IProjectile projectile) {
        PersistentDataContainer container = projectile.getBukkitEntity().getPersistentDataContainer();
        if (!container.has(BOW_KEY)) return ItemStack.EMPTY;
        return CraftItemStack.asNMSCopy(org.bukkit.inventory.ItemStack.deserializeBytes(container.get(BOW_KEY, PersistentDataType.BYTE_ARRAY)));
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
