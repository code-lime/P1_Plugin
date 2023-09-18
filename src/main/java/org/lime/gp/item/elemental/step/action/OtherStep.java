package org.lime.gp.item.elemental.step.action;

import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.lime.gp.item.elemental.Elemental;
import org.lime.gp.item.elemental.step.IStep;

public record OtherStep(String step) implements IStep {
    @Override public void execute(Player player, Transformation location) { Elemental.execute(player, step, location); }
}
