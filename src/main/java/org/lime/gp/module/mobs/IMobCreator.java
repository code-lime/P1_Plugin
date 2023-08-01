package org.lime.gp.module.mobs;

import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3D;

public interface IMobCreator {
    Entity spawn(WorldServer worldserver, Vec3D pos);
}
