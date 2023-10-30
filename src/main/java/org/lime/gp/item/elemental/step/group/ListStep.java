package org.lime.gp.item.elemental.step.group;

import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.step.IStep;

import java.util.List;

public record ListStep(List<IStep> steps) implements IStep {
    @Override public void execute(Player player, DataContext context, Transformation location) {
        steps.forEach(step -> step.execute(player, context, location));
    }
}
