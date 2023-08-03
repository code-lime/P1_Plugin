package org.lime.gp.item.elemental.step.action;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.gp.item.elemental.step.IStep;

public class NoneStep implements IStep {
    public static final NoneStep Instance = new NoneStep();
    private NoneStep() {}
    @Override public void execute(Player player, Vector position) { }
}
