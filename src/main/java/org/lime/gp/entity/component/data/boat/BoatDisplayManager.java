package org.lime.gp.entity.component.data.boat;

import org.lime.display.DisplayManager;
import org.lime.display.Displays;
import org.lime.gp.module.TimeoutData;
import org.lime.plugin.CoreElement;

import java.util.Map;
import java.util.UUID;

public class BoatDisplayManager extends DisplayManager<UUID, BoatData, BoatDisplay> {
    public static CoreElement create() {
        return CoreElement.create(BoatDisplayManager.class)
                .withInit(BoatDisplayManager::init);
    }
    private static void init() { Displays.initDisplay(new BoatDisplayManager()); }
    
    @Override public boolean isFast() { return true; }
    @Override public boolean isAsync() { return true; }

    @Override public Map<UUID, BoatData> getData() { return TimeoutData.map(BoatData.class); }
    @Override public BoatDisplay create(UUID uuid, BoatData data) { return new BoatDisplay(uuid, data); }
}