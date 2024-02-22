package org.lime.gp.item.elemental.step.group;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.system.utils.MathUtils;

@Step(name = "offset")
public record OffsetStep(IStep<?> step, Transformation offset, boolean onlyYaw) implements IStep<OffsetStep> {
    @Override public void execute(Player player, DataContext context, Transformation location) {
        if (onlyYaw) location = MathUtils.onlyYaw(location);
        location = MathUtils.transform(offset, location);
        step.execute(player, context, location);
    }

    public OffsetStep parse(JsonObject json) {
        return new OffsetStep(
                IStep.parse(json.get("step")),
                MathUtils.transformation(json.get("offset")),
                json.has("only_yaw") && json.get("only_yaw").getAsBoolean()
        );
    }
}
