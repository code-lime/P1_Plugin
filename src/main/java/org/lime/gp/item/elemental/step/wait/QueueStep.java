package org.lime.gp.item.elemental.step.wait;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.lime;

import java.util.List;

@Step(name = "queue")
public record QueueStep(List<? extends IStep<?>> steps, double sec) implements IStep<QueueStep> {
    @Override public void execute(Player player, DataContext context, Transformation location) {
        int sec = 0;
        for (IStep<?> step : steps) {
            lime.once(() -> step.execute(player, context, location), sec);
            sec += this.sec;
        }
    }

    public QueueStep parse(JsonObject json) {
        return new QueueStep(
                json.getAsJsonArray("steps").asList().stream().map(IStep::parse).toList(),
                json.get("sec").getAsDouble()
        );
    }
}
