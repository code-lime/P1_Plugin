package org.lime.gp.item.settings.use.target;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.lime.gp.item.settings.use.UseSetting;

import java.util.Optional;

public class EntityTarget implements IEntityTarget, ILocationTarget {
    private final Location location;
    private final Entity target;

    public EntityTarget(Entity target) {
        this.target = target;
        this.location = target.getLocation();
    }

    @Override public boolean isSelf() { return false; }
    @Override public boolean isActive() { return UseSetting.isDistance(location, target.getLocation(), 1); }
    @Override public Entity getTargetEntity(Entity self) { return target; }

    public Entity getEntity() { return target; }
    @Override public Location getLocation() { return location; }

    @Override public Optional<BlockTarget> castToBlock() { return Optional.empty(); }
    @Override public Optional<PlayerTarget> castToPlayer() {
        return target instanceof Player player
                ? Optional.of(new PlayerTarget(player))
                : Optional.empty();
    }
    @Override public Optional<EntityTarget> castToEntity() { return Optional.of(this); }
}
