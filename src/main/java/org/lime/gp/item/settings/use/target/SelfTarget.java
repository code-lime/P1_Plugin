package org.lime.gp.item.settings.use.target;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Optional;

public class SelfTarget implements IPlayerTarget, IEntityTarget {
    public static final SelfTarget Instance = new SelfTarget();

    private SelfTarget() {}

    @Override public boolean isSelf() { return true; }
    @Override public boolean isActive() { return true; }
    @Override public Player getTargetPlayer(Player self) { return self; }
    @Override public Entity getTargetEntity(Entity self) { return self; }

    @Override public Optional<BlockTarget> castToBlock() { return Optional.empty(); }
    @Override public Optional<PlayerTarget> castToPlayer() { return Optional.empty(); }
    @Override public Optional<EntityTarget> castToEntity() { return Optional.empty(); }
}
