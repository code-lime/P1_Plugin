package org.lime.gp.module;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.lime.core;
import org.lime.gp.lime;

public class SnowMan implements Listener {
    public static core.element create() {
        return core.element.create(SnowMan.class)
                .withInit(SnowMan::init)
                .withInstance();
    }
    public static void init() {
        lime.repeat(SnowMan::update, 2);
    }
    public static void update() {
        Bukkit.getWorlds().forEach(world -> world
                .getEntitiesByClass(Snowman.class)
                .forEach(snowman -> snowman.setAI(false))
        );
    }

    @EventHandler public static void on(EntityDamageEvent e) {
        if (e.getEntityType() != EntityType.SNOWMAN) return;
        if (e.getCause() == EntityDamageEvent.DamageCause.DROWNING) e.setCancelled(true);
    }
    @EventHandler public static void on(EntityBlockFormEvent e) {
        if (e.getEntity().getType() != EntityType.SNOWMAN) return;
        e.setCancelled(true);
    }
}
