package org.lime.gp.item.elemental.step.wait;

import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.lime;

public record WaitStep(IStep step, double sec) implements IStep {
    @Override public void execute(Player player, DataContext context, Transformation location) {
        lime.once(() -> step.execute(player, context, location), sec);
    }
}
