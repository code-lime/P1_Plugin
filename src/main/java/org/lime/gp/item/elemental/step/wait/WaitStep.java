package org.lime.gp.item.elemental.step.wait;

import org.bukkit.entity.Player;
import org.lime.display.transform.LocalLocation;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.lime;

public record WaitStep(IStep step, double sec) implements IStep {
    @Override public void execute(Player player, LocalLocation location) {
        lime.once(() -> step.execute(player, location), sec);
    }
}
