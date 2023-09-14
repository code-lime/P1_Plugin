package org.lime.gp.block.component.display.instance.list;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.lime.display.models.shadow.IBuilder;
import org.lime.system.execute.*;

public record ModelDisplayObject(Location location, Set<UUID> viewers, IBuilder model, Map<String, Object> data, double distance) {
    public boolean hasViewer(UUID uuid) { return viewers.contains(uuid); }
    public static ModelDisplayObject of(Location location, IBuilder model, Map<String, Object> data, double distance) {
        return new ModelDisplayObject(location, ConcurrentHashMap.newKeySet(), model, data, distance);
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
