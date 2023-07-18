package org.lime.gp.module.damage;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

public class EntityDamageByPlayerEvent extends BaseDamageByPlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    public EntityDamageByPlayerEvent(Player owner, EntityDamageByEntityEvent base) { super(owner, base); }

    public Optional<Player> getEntityPlayer() { return super.getEntity() instanceof Player player ? Optional.of(player) : Optional.empty(); }
    public boolean isEntityPlayer() { return super.getEntity() instanceof Player; }
    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
