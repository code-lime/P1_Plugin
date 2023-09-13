package org.lime.gp.player.module;

import com.google.gson.JsonPrimitive;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.lime;
import org.lime.gp.module.damage.EntityDamageByPlayerEvent;

public class ForeDamage implements Listener {
    private static double fore_damage = 1;
    public static CoreElement create() {
        return CoreElement.create(ForeDamage.class)
                .withInstance()
                .<JsonPrimitive>addConfig("config", v -> v.withParent("fore_damage").withDefault(new JsonPrimitive(0.25)).withInvoke(_v -> fore_damage = _v.getAsDouble()));
    }
    @SuppressWarnings("deprecation")
    @EventHandler public static void on(EntityDamageByPlayerEvent e) {
        Player owner = e.getDamageOwner();
        Entity damage = e.getDamageEntity();
        if (e.isEntityPlayer() && (damage == owner || damage.getType() == EntityType.ARROW)) {
            e.setDamage(EntityDamageEvent.DamageModifier.BASE, e.getDamage(EntityDamageEvent.DamageModifier.BASE) * fore_damage);
        }
    }
}
