package org.lime.gp.item.settings.use.target;

import java.util.Optional;

public interface ITarget {
    boolean isSelf();
    boolean isActive();

    Optional<BlockTarget> castToBlock();
    Optional<PlayerTarget> castToPlayer();
}
