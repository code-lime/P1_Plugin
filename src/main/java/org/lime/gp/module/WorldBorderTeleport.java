package org.lime.gp.module;

import net.minecraft.world.level.World;
import net.minecraft.world.level.border.WorldBorder;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;

public class WorldBorderTeleport {
    public static CoreElement create() {
        return CoreElement.create(WorldBorderTeleport.class)
                .withInit(WorldBorderTeleport::init);
    }

    private static void init() {
        lime.repeat(WorldBorderTeleport::update, 10);
    }
    private static void update() {
        Bukkit.getWorlds().forEach(world -> {
            if (world instanceof CraftWorld craftWorld) {
                World handle = craftWorld.getHandle();
                WorldBorder border = handle.getWorldBorder();
                handle.players().forEach(player -> {
                    if (border.getDistanceToBorder(player) < -200)
                        player.teleportTo(border.getCenterX(), player.getY(), border.getCenterZ());
                });
            }
        });
    }
}
