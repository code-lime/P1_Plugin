package org.lime.gp.extension;

import com.google.common.collect.Lists;
import net.minecraft.world.phys.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Collections;

public final class Zone {
    public static void showBox(Collection<? extends Player> players, World world, AxisAlignedBB rotated, Particle particle) {
        show(players, world, new Vector(rotated.minX, rotated.minY, rotated.minZ), new Vector(rotated.maxX, rotated.maxY, rotated.maxZ), true, particle, false);
    }
    public static void showBox(Collection<? extends Player> players, World world, Vector pos1, Vector pos2, Particle particle) {
        show(players, world, pos1, pos2, true, particle, false);
    }
    public static void showBox(Collection<? extends Player> players, World world, Vector pos1, Vector pos2, boolean full, Particle particle) {
        show(players, world, pos1, pos2, full, particle, false);
    }
    public static void showBox(Player player, AxisAlignedBB rotated, Particle particle) {
        showBox(Collections.singleton(player), player.getWorld(), rotated, particle);
    }
    public static void showBox(Player player, Vector pos1, Vector pos2, Particle particle) {
        showBox(Collections.singleton(player), player.getWorld(), pos1, pos2, particle);
    }
    public static void showBox(Player player, Vector pos1, Vector pos2, boolean full, Particle particle) {
        showBox(Collections.singleton(player), player.getWorld(), pos1, pos2, full, particle);
    }

    private static void show(Collection<? extends Player> players, World world, Vector pos1, Vector pos2, boolean full, Particle particle, boolean onlyDown) {
        int append = full ? 1 : 0;
        Vector max = new Vector(Math.max(pos1.getX(), pos2.getX()) + append, Math.max(pos1.getY(), pos2.getY()) + append, Math.max(pos1.getZ(), pos2.getZ()) + append);
        Vector min = new Vector(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()));

        var builder = particle.builder().receivers(Lists.newArrayList(players)).count(0);

        if (!onlyDown) {
            for (double y = min.getY(); y <= max.getY(); y += 0.25) {
                builder
                        .location(new Location(world, max.getX(), y, max.getZ())).spawn()
                        .location(new Location(world, min.getX(), y, min.getZ())).spawn()
                        .location(new Location(world, max.getX(), y, min.getZ())).spawn()
                        .location(new Location(world, min.getX(), y, max.getZ())).spawn();
            }
        }

        for (double x = min.getX(); x <= max.getX(); x += 0.25) {
            if (!onlyDown) {
                builder
                        .location(new Location(world, x, max.getY(), max.getZ())).spawn()
                        .location(new Location(world, x, max.getY(), min.getZ())).spawn();
            }
            builder
                    .location(new Location(world, x, min.getY(), min.getZ())).spawn()
                    .location(new Location(world, x, min.getY(), max.getZ())).spawn();
        }

        for (double z = min.getZ(); z <= max.getZ(); z += 0.25) {
            if (!onlyDown) {
                builder
                        .location(new Location(world, max.getX(), max.getY(), z)).spawn()
                        .location(new Location(world, min.getX(), max.getY(), z)).spawn();
            }
            builder
                    .location(new Location(world, min.getX(), min.getY(), z)).spawn()
                    .location(new Location(world, max.getX(), min.getY(), z)).spawn();
        }
    }
}
