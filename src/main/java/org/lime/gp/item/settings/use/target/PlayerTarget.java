package org.lime.gp.item.settings.use.target;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.lime.gp.item.settings.use.UseSetting;

import java.util.Optional;

public class PlayerTarget implements IPlayerTarget {
    private final Location location;
    private final Player target;

    public PlayerTarget(Player target) {
        this.target = target;
        this.location = target.getLocation();
    }

    @Override public boolean isSelf() { return false; }
    @Override public boolean isActive() { return UseSetting.isDistance(location, target.getLocation(), 1); }
    @Override public Player getTargetPlayer(Player self) { return target; }

    @Override public Optional<BlockTarget> castToBlock() { return Optional.empty(); }
    @Override public Optional<PlayerTarget> castToPlayer() { return Optional.of(this); }
 }
