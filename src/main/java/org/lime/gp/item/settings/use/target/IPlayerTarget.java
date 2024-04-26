package org.lime.gp.item.settings.use.target;

import org.bukkit.entity.Player;

public interface IPlayerTarget extends IEntityTarget {
    Player getTargetPlayer(Player self);
}
