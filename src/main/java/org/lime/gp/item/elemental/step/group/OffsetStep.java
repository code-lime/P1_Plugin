package org.lime.gp.item.elemental.step.group;

import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.system.utils.MathUtils;

public record OffsetStep(IStep step, Transformation offset, boolean onlyYaw) implements IStep {
    @Override public void execute(Player player, DataContext context, Transformation location) {
        if (onlyYaw) location = MathUtils.onlyYaw(location);
        location = MathUtils.transform(offset, location);
        step.execute(player, context, location);
    }
}
