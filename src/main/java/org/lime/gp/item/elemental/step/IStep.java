package org.lime.gp.item.elemental.step;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public interface IStep {
    void execute(Player player, Vector position);
}
