package org.lime.gp.item.settings.use.target;

import org.bukkit.Location;
import org.bukkit.World;

public interface ILocationTarget extends ITarget {
    default World getWorld() {
        return getLocation().getWorld();
    }
    Location getLocation();
}
