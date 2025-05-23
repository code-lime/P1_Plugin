package org.lime.gp.module.damage;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.inventory.ItemStack;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.lime;

import java.util.Optional;

@SuppressWarnings("deprecation")
public abstract class BaseDamageByPlayerEvent extends Event {
    public static CoreElement create() {
        return CoreElement.create(BaseDamageByPlayerEvent.class)
                .withInstance(new Listener() {
                    @EventHandler public void on(EntityDamageByEntityEvent base) {
                        ExtMethods.damagerPlayer(base)
                                .or(() -> Optional.ofNullable(base.getDamager() instanceof Player p ? p : null))
                                .ifPresent(player -> Bukkit.getServer()
                                        .getPluginManager()
                                        .callEvent(new EntityDamageByPlayerEvent(player, base))
                                );
                    }
                });
    }

    private final EntityDamageByEntityEvent base;
    private final Player owner;

    protected BaseDamageByPlayerEvent(Player owner, EntityDamageByEntityEvent base) {
        this.base = base;
        this.owner = owner;
    }

    public EntityDamageByEntityEvent getBase() { return base; }
    public Player getDamageOwner() { return owner; }
    public Entity getDamageEntity() { return base.getDamager(); }
    public boolean isCancelled() { return base.isCancelled(); }
    public void setCancelled(boolean cancel) { base.setCancelled(cancel); }
    public double getOriginalDamage(DamageModifier type) throws IllegalArgumentException { return base.getOriginalDamage(type); }
    public void setDamage(DamageModifier type, double damage) throws IllegalArgumentException, UnsupportedOperationException { base.setDamage(type, damage); }
    public double getDamage(DamageModifier type) throws IllegalArgumentException { return base.getDamage(type); }
    public boolean isApplicable(DamageModifier type) throws IllegalArgumentException { return base.isApplicable(type); }
    public double getDamage() { return base.getDamage(); }
    public void setDamage(double damage) { base.setDamage(damage); }
    public DamageCause getCause() { return base.getCause(); }
    public Entity getEntity() { return base.getEntity(); }
    public EntityType getEntityType() { return base.getEntityType(); }
    public ItemStack getItem() {
        Entity entity = getDamageEntity();
        return entity instanceof ThrowableProjectile throwable
                ? throwable.getItem()
                : entity instanceof Player player
                    ? player.getInventory().getItemInMainHand()
                    : null;
    }
}
