package org.lime.gp.module;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.lime.core;
import org.lime.gp.lime;

public class SconeCutterDamage {
    public static core.element create() {
        return core.element.create(SconeCutterDamage.class)
                .withInit(SconeCutterDamage::init);
    }
    public static void init() {
        lime.repeat(SconeCutterDamage::update, 0.25);
    }
    public static void update() {
        Bukkit.getWorlds().forEach(world -> world.getEntitiesByClass(LivingEntity.class).forEach(entity -> {
            if (entity.getLocation().getBlock().getType() != Material.STONECUTTER) return;
            entity.damage(20, entity);
        }));
    }
}
