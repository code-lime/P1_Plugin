package org.lime.gp.entity.component.display.instance;

import org.bukkit.Location;
import org.lime.display.models.display.IAnimationData;
import org.lime.display.models.shadow.IBuilder;
import org.lime.gp.block.component.display.instance.list.ModelDisplayObject;
import org.lime.system.execute.Func1;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record DisplayObject(Location location, Set<UUID> viewers, IBuilder model, Map<String, Object> data) implements IAnimationData {
    public boolean hasViewer(UUID uuid) {
        return viewers.contains(uuid);
    }

    public static DisplayObject of(Location location, IBuilder model, Map<String, Object> data) {
        return new DisplayObject(location, ConcurrentHashMap.newKeySet(), model, data);
    }

    public void removeViewersIf(Func1<UUID, Boolean> filter) {
        viewers.removeIf(filter::invoke);
    }
    public boolean isViewersEmpty() {
        return viewers.isEmpty();
    }

    public void addViewer(UUID viewer) {
        viewers.add(viewer);
    }
}
