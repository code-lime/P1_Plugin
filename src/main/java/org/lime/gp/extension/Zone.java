package org.lime.gp.extension;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class Zone {
    public static void showBox(Player player, Vector pos1, Vector pos2, Particle particle) {
        show(player, pos1, pos2, true, particle, false);
    }
    public static void showBox(Player player, Vector pos1, Vector pos2, boolean full, Particle particle) {
        show(player, pos1, pos2, full, particle, false);
    }

    private static void show(Player player, Vector pos1, Vector pos2, boolean full, Particle particle, boolean onlyDown) {
        int append = full ? 1 : 0;
        Vector max = new Vector(Math.max(pos1.getX(), pos2.getX()) + append, Math.max(pos1.getY(), pos2.getY()) + append, Math.max(pos1.getZ(), pos2.getZ()) + append);
        Vector min = new Vector(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()));

        if (!onlyDown) {
            for (double y = min.getY(); y <= max.getY(); y += 0.25) {
                player.spawnParticle(particle, max.getX(), y, max.getZ(),0);
                player.spawnParticle(particle, min.getX(), y, min.getZ(),0);
                player.spawnParticle(particle, max.getX(), y, min.getZ(),0);
                player.spawnParticle(particle, min.getX(), y, max.getZ(),0);
            }
        }

        for (double x = min.getX(); x <= max.getX(); x += 0.25) {
            if (!onlyDown) {
                player.spawnParticle(particle, x, max.getY(), max.getZ(),0);
                player.spawnParticle(particle, x, max.getY(), min.getZ(),0);
            }
            player.spawnParticle(particle, x, min.getY(), min.getZ(),0);
            player.spawnParticle(particle, x, min.getY(), max.getZ(),0);
        }

        for (double z = min.getZ(); z <= max.getZ(); z += 0.25) {
            if (!onlyDown) {
                player.spawnParticle(particle, max.getX(), max.getY(), z, 0);
                player.spawnParticle(particle, min.getX(), max.getY(), z,0);
            }
            player.spawnParticle(particle, min.getX(), min.getY(), z,0);
            player.spawnParticle(particle, max.getX(), min.getY(), z,0);
        }
    }
}
