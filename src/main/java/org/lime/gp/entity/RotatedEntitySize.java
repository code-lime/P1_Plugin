package org.lime.gp.entity;

import net.minecraft.world.entity.EntityLimeMarker;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.ITargetBoundingBox;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import org.joml.Math;
import org.joml.Vector3f;

import javax.annotation.Nonnull;

public class RotatedEntitySize extends EntitySize implements ITargetBoundingBox {
    public final float length;
    public RotatedEntitySize(float width, float height, float length, boolean fixed) {
        super(width, height, fixed);
        this.length = length;
    }

    @Override public @Nonnull AxisAlignedBB makeBoundingBox(EntityLimeMarker marker) {
        Vec3D pos = marker.position();

        float dx = width / 2.0f;
        float dy = height / 2.0f;
        float dz = length / 2.0f;

        Vector3f a = new Vector3f(dx, 0, dz);
        Vector3f b = new Vector3f(dx, 0, -dz);
        Vector3f c = new Vector3f(-dx, 0, dz);
        Vector3f d = new Vector3f(-dx, 0, -dz);

        Vector3f position = new Vector3f((float) pos.x, (float) pos.y, (float) pos.z);
        float yaw = Math.toRadians(marker.getBukkitYaw());
        float angleCos = Math.cos(yaw);
        float angleSin = Math.sin(yaw);

        float minX = position.x;
        float maxX = position.x;

        float minY = position.y - dy;
        float maxY = position.y + dy;

        float minZ = position.z;
        float maxZ = position.z;

        for (Vector3f point : new Vector3f[] { a, b, c, d }) {
            float x = position.x + angleCos * point.x + angleSin * point.z;
            float z = position.z - angleSin * point.x + angleCos * point.z;

            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);

            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }

        return new AxisAlignedBB(minX, minY, minZ, maxX - 1, maxY, maxZ - 1, true);
    }
    @Override public @Nonnull AxisAlignedBB makeBoundingBox(double x, double y, double z) {
        float f = Math.max(this.width, this.length) / 2.0F;
        return new AxisAlignedBB(x - f, y, z - f, x + f, y + this.height, z + f);
        //return new AxisAlignedBB(x, y, z, x, y + this.height, z);
    }
    @Override public @Nonnull EntitySize scale(float widthRatio, float heightRatio) {
        return (this.fixed || (widthRatio == 1.0F && heightRatio == 1.0F))
                ? this
                : new RotatedEntitySize(this.width * widthRatio, this.length, this.height * heightRatio, false);
    }
}














