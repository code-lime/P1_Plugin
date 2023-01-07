package org.lime.gp.module;

import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.lime.core;

public class ArmorStandArms implements Listener {
    public static core.element create() {
        return core.element.create(ArmorStandArms.class)
                .withInstance();
    }
    @EventHandler public static void on(EntitySpawnEvent e) {
        if (e.getEntity() instanceof ArmorStand armorStand) {
            armorStand.setArms(true);
        }
    }
}
