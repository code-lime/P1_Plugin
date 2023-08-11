package org.lime.gp.item.elemental.step.group;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.display.transform.LocalLocation;
import org.lime.gp.item.elemental.step.IStep;

import java.util.List;

public record ListStep(List<IStep> steps) implements IStep {
    @Override public void execute(Player player, LocalLocation location) { steps.forEach(step -> step.execute(player, location)); }
}
