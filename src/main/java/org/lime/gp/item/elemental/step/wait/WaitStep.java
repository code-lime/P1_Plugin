package org.lime.gp.item.elemental.step.wait;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.lime;

@Step(name = "wait")
public record WaitStep(IStep<?> step, double sec) implements IStep<WaitStep> {
    @Override public void execute(Player player, DataContext context, Transformation location) {
        lime.once(() -> step.execute(player, context, location), sec);
    }

    public WaitStep parse(JsonObject json) {
        return new WaitStep(
                IStep.parse(json.get("step")),
                json.get("sec").getAsDouble()
        );
    }
}
