package org.lime.gp.player.module;

import com.google.gson.JsonPrimitive;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.lime.core;
import org.lime.gp.module.damage.PlayerDamageByPlayerEvent;

public class ForeDamage implements Listener {
    private static double fore_damage = 1;
    public static core.element create() {
        return core.element.create(ForeDamage.class)
                .withInstance()
                .<JsonPrimitive>addConfig("config", v -> v.withParent("fore_damage").withDefault(new JsonPrimitive(0.25)).withInvoke(_v -> fore_damage = _v.getAsDouble()));
    }
    @SuppressWarnings("deprecation")
    @EventHandler public static void on(PlayerDamageByPlayerEvent e) {
        Player owner = e.getDamageOwner();
        Entity entity = e.getDamageEntity();
        if (owner != entity && entity.getType() != EntityType.ARROW) return;
        e.setDamage(EntityDamageEvent.DamageModifier.BASE, e.getDamage(EntityDamageEvent.DamageModifier.BASE) * fore_damage);
    }
}
