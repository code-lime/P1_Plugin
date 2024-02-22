package org.lime.gp.item.settings.use.target;

import org.bukkit.entity.Player;

public interface IPlayerTarget extends ITarget {
    Player getTargetPlayer(Player self);
}
