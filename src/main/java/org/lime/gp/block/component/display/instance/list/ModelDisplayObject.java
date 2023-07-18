package org.lime.gp.block.component.display.instance.list;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.lime.display.models.Model;
import org.lime.system;

public record ModelDisplayObject(Location location, Set<UUID> viewers, Model model, Map<String, Object> data) {
    public boolean hasViewer(UUID uuid) { return viewers.contains(uuid); }
    public static ModelDisplayObject of(Location location, Model model, Map<String, Object> data) {
        return new ModelDisplayObject(location, ConcurrentHashMap.newKeySet(), model, data);
    }
    public void removeViewersIf(system.Func1<UUID, Boolean> filter) {
        viewers.removeIf(filter::invoke);
    }
    public boolean isViewersEmpty() {
        return viewers.isEmpty();
    }

    public void addViewer(UUID viewer) {
        viewers.add(viewer);
    }
}
