package org.lime.gp.entity.component.display.instance;

import org.lime.gp.entity.component.display.display.EntityModelDisplay;
import org.lime.gp.module.TimeoutData;

import java.util.HashMap;
import java.util.Map;

public final class DisplayMap extends TimeoutData.ITimeout {
    public final Map<EntityModelDisplay.EntityModelKey, DisplayObject> map = new HashMap<>();

    public DisplayMap(Map<EntityModelDisplay.EntityModelKey, DisplayObject> map) {
        super(3);
        this.map.putAll(map);
    }
}
