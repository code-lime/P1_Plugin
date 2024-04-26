package org.lime.gp.item.settings.use.target;

import java.util.Optional;

public interface ITarget {
    boolean isSelf();
    boolean isActive();

    Optional<BlockTarget> castToBlock();
    Optional<PlayerTarget> castToPlayer();
    Optional<EntityTarget> castToEntity();
    default Optional<ILocationTarget> castToLocation() {
        return this instanceof ILocationTarget locationTarget
                ? Optional.of(locationTarget)
                : Optional.empty();
    }
}
