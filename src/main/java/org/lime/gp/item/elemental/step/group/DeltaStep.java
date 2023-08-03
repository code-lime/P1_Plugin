package org.lime.gp.item.elemental.step.group;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.gp.item.elemental.step.IStep;

public record DeltaStep(IStep step, double deltaX, double deltaY, double deltaZ, int count) implements IStep {
    public Vector toRandom(Vector center) {
        return Vector.getRandom()
                .subtract(new Vector(0.5, 0.5, 0.5))
                .multiply(new Vector(deltaX * 2, deltaY * 2, deltaZ * 2))
                .add(center);
    }
    @Override public void execute(Player player, Vector position) {
        for (int i = 0; i < count; i++)
            step.execute(player, toRandom(position));
    }
}
