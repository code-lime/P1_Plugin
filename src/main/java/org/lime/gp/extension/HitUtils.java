package org.lime.gp.extension;

import net.minecraft.world.level.RayTrace;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftVector;
import org.bukkit.util.Vector;

public class HitUtils {
    public static Vector getHitBlockPoint(World world, Vector start, Vector end) {
        if (!(world instanceof CraftWorld handle)) return start;
        RayTrace ray = new RayTrace(CraftVector.toNMS(start), CraftVector.toNMS(end), RayTrace.BlockCollisionOption.COLLIDER, RayTrace.FluidCollisionOption.NONE, null);
        MovingObjectPositionBlock hit = handle.getHandle().clip(ray);
        return CraftVector.toBukkit(hit.getLocation());
    }
    public static boolean isHitPoint(World world, Vector start, Vector end, double epsilon) {
        Vector hit = getHitBlockPoint(world, start, end);
        return end.distanceSquared(hit) < epsilon * epsilon;
    }
}
