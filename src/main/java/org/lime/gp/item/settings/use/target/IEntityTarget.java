package org.lime.gp.item.settings.use.target;

import org.bukkit.entity.Entity;

public interface IEntityTarget extends ITarget {
    Entity getTargetEntity(Entity self);
}
