package org.lime.gp.item.elemental.step.group;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.display.transform.LocalLocation;
import org.lime.gp.item.elemental.step.IStep;

public record OffsetStep(IStep step, Vector offset) implements IStep {
    @Override public void execute(Player player, LocalLocation location) {
        step.execute(player, location.add(offset));
    }
}
