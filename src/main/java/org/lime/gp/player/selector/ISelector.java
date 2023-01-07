package org.lime.gp.player.selector;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public abstract class ISelector {
    protected Player player;
    protected abstract void init();
    protected abstract boolean isRemoveUpdate();
    protected abstract void onRemove();
    public abstract SelectorType getType();
    public void select(Player player) { UserSelector.setSelector(player, this); }
    public final void remove() { UserSelector.removeSelector(player); }

    protected abstract boolean onBlockClick(PlayerInteractEvent event);
}
