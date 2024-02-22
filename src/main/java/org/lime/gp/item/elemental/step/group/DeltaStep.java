package org.lime.gp.item.elemental.step.group;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.system.utils.MathUtils;

@Step(name = "delta")
public record DeltaStep(IStep<?> step, Vector delta, int count) implements IStep<DeltaStep> {
    public Transformation toRandom(Transformation location) {
        Vector offset = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).multiply(2).multiply(delta);
        return MathUtils.transform(location, new Transformation(new Vector3f((float) offset.getX(), (float) offset.getY(), (float) offset.getZ()), null, null, null));
    }
    @Override public void execute(Player player, DataContext context, Transformation location) {
        for (int i = 0; i < count; i++)
            step.execute(player, context, toRandom(location));
    }

    public DeltaStep parse(JsonObject json) {
        return new DeltaStep(
                IStep.parse(json.get("step")),
                MathUtils.getVector(json.get("delta").getAsString()),
                json.get("count").getAsInt()
        );
    }
}
