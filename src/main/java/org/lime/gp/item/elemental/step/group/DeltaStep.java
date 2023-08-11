package org.lime.gp.item.elemental.step.group;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.display.transform.LocalLocation;
import org.lime.gp.item.elemental.step.IStep;

public record DeltaStep(IStep step, Vector delta, int count) implements IStep {
    public LocalLocation toRandom(LocalLocation location) {
        return location.add(Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).multiply(delta));
    }
    @Override public void execute(Player player, LocalLocation location) {
        for (int i = 0; i < count; i++)
            step.execute(player, toRandom(location));
    }
}
