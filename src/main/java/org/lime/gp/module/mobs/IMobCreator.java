package org.lime.gp.module.mobs;

import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3D;

import javax.annotation.Nullable;

public interface IMobCreator {
    @Nullable Entity spawn(WorldServer worldserver, Vec3D pos);
}
