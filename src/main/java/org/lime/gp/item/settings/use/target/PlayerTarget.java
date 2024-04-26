package org.lime.gp.item.settings.use.target;

import org.bukkit.entity.Player;

import java.util.Optional;

public class PlayerTarget extends EntityTarget implements IPlayerTarget {
    private final Player target;

    public PlayerTarget(Player target) {
        super(target);
        this.target = target;
    }

    public Player getPlayer() { return target; }

    @Override public Optional<PlayerTarget> castToPlayer() { return Optional.of(this); }
    @Override public Player getTargetPlayer(Player self) { return target; }
}
