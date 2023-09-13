package org.lime.gp.module;

import com.google.gson.JsonPrimitive;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.lime;

public class StonecutterModule {
    private static boolean stonecutterEnable = true;
    public static boolean isEnable() {
        return stonecutterEnable;
    }
    public static CoreElement create() {
        return CoreElement.create(StonecutterModule.class)
                .withInit(StonecutterModule::init)
                .<JsonPrimitive>addConfig("config", v -> v
                        .withParent("stonecutter")
                        .withDefault(new JsonPrimitive(stonecutterEnable))
                        .withInvoke(json -> stonecutterEnable = json.getAsBoolean())
                );
    }
    public static void init() {
        lime.repeat(StonecutterModule::update, 0.25);
    }
    public static void update() {
        Bukkit.getWorlds().forEach(world -> world.getEntitiesByClass(LivingEntity.class).forEach(entity -> {
            if (entity.getLocation().getBlock().getType() != Material.STONECUTTER) return;
            entity.damage(5, entity);
        }));
    }
}
