package org.lime.gp.item.elemental.step.group;

import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.lime.gp.item.elemental.step.IStep;

public record DeltaStep(IStep step, Vector delta, int count) implements IStep {
    public Transformation toRandom(Transformation location) {
        Vector offset = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).multiply(2).multiply(delta);
        return location.compose(new Transformation(new Vector3f((float) offset.getX(), (float) offset.getY(), (float) offset.getZ()), null, null, null));
    }
    @Override public void execute(Player player, Transformation location) {
        for (int i = 0; i < count; i++)
            step.execute(player, toRandom(location));
    }
}
