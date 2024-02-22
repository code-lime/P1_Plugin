package org.lime.gp.item.elemental.step.group;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;

import java.util.List;

@Step(name = "list")
public record ListStep(List<? extends IStep<?>> steps) implements IStep<ListStep> {
    @Override public void execute(Player player, DataContext context, Transformation location) {
        steps.forEach(step -> step.execute(player, context, location));
    }

    public ListStep parse(JsonObject json) {
        return new ListStep(
                json.getAsJsonArray("steps").asList().stream().map(IStep::parse).toList()
        );
    }
}
