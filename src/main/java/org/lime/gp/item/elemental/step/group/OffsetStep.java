package org.lime.gp.item.elemental.step.group;

import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.lime.gp.item.elemental.step.IStep;

public record OffsetStep(IStep step, Transformation offset) implements IStep {
    @Override public void execute(Player player, Transformation location) {
        step.execute(player, location.compose(offset));
    }
}
