package org.lime.gp.module;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.lime.plugin.CoreElement;

public class ExplosionDisable implements Listener {
    public static CoreElement create() {
        return CoreElement.create(ExplosionDisable.class)
                .withInstance();
    }

    @EventHandler public static void on(ExplosionPrimeEvent e) {
        e.setCancelled(true);
    }
}
