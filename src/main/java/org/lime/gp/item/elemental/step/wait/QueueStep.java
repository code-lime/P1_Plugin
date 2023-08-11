package org.lime.gp.item.elemental.step.wait;

import org.bukkit.entity.Player;
import org.lime.display.transform.LocalLocation;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.lime;

import java.util.List;

public record QueueStep(List<IStep> steps, double sec) implements IStep {
    @Override public void execute(Player player, LocalLocation location) {
        int sec = 0;
        for (IStep step : steps) {
            lime.once(() -> step.execute(player, location), sec);
            sec += this.sec;
        }
    }
}
