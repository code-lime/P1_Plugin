package net.minecraft.world.entity;

import net.minecraft.world.phys.AxisAlignedBB;

public interface ITargetBoundingBox {
    AxisAlignedBB makeBoundingBox(EntityLimeMarker marker);
}
