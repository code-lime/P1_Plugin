package org.lime.gp.entity.component.data.boat;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.lime.display.models.shadow.IBuilder;
import org.lime.gp.module.TimeoutData;

public class BoatData extends TimeoutData.ITimeout {
    public final IBuilder model;
    public final Vector partPosition;
    public final Vector localOffset;
    public final World world;
    public final float yaw;
    public final float pitch;

    public BoatData(IBuilder model, Location location) {
        super(5);
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        int blockX = Location.locToBlock(x);
        int blockY = Location.locToBlock(y);
        int blockZ = Location.locToBlock(z);

        int partX = blockX >> 4;
        int partY = blockY >> 4;
        int partZ = blockZ >> 4;

        this.model = model;
        this.partPosition = new Vector(partX, partY, partZ);
        this.localOffset = new Vector(x - (partX << 4), y - (partZ << 4), z - (partZ << 4));
        this.world = location.getWorld();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }
}













