package org.lime.gp.module.damage;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PlayerDamageByPlayerEvent extends EntityDamageByPlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    protected PlayerDamageByPlayerEvent(Player owner, EntityDamageByEntityEvent base) { super(owner, base); }

    public Player getEntity() { return (Player)super.getEntity(); }

    @Override public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
