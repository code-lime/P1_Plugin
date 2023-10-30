package org.lime.gp.item.projectile;

import com.mojang.math.Transformation;
import org.bukkit.Location;
import org.bukkit.entity.Trident;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;

public record ProjectileFrame(Location location, Transformation transformation, ItemStack item, Collection<String> tags) {
    public static int FRAME_DURATION = 3;

    public static ProjectileFrame of(Trident trident) {
        Location location = trident.getLocation();
        Vector3f translation = new Vector3f((float) location.x(), (float) location.y(), (float) location.z());
        Vector rotate = new Vector(0, location.getYaw() + 90, 180 - location.getPitch());
        Quaternionf rotation = new Quaternionf().rotationXYZ((float) Math.toRadians(rotate.getX()), (float) Math.toRadians(rotate.getY()), (float) Math.toRadians(rotate.getZ()));

        Transformation transformation = new Transformation(translation, rotation, new Vector3f(1,1,1), new Quaternionf());
        return new ProjectileFrame(location, transformation, trident.getItem(), trident.getScoreboardTags());
    }

    public boolean isEquals(ProjectileFrame frame) {
        return location.equals(frame.location);
    }

    public Transformation withOffset(Vector offset) {
        Transformation old = transformation();
        return new Transformation(old.getTranslation().sub((float) offset.getX(), (float) offset.getY(), (float) offset.getZ()), old.getLeftRotation(), old.getScale(), old.getRightRotation());
    }
}
