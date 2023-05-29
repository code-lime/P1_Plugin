package org.lime.gp.block.component.display.instance;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.lime.gp.block.component.display.display.BlockModelDisplay;
import org.lime.gp.block.component.display.instance.list.ItemDisplayObject;
import org.lime.gp.block.component.display.instance.list.ItemFrameDisplayObject;
import org.lime.gp.block.component.display.instance.list.ModelDisplayObject;
import org.lime.gp.module.TimeoutData;

public final class DisplayMap extends TimeoutData.ITimeout {
    public final Map<UUID, ItemFrameDisplayObject> frameMap = new HashMap<>();
    public final Map<BlockModelDisplay.BlockModelKey, ModelDisplayObject> modelMap = new HashMap<>();
    public final Map<UUID, ItemDisplayObject> viewMap = new HashMap<>();
    
    public DisplayMap(Map<UUID, ItemFrameDisplayObject> itemFrame, Map<BlockModelDisplay.BlockModelKey, ModelDisplayObject> models, Map<UUID, ItemDisplayObject> viewMap) {
        super(DisplayInstance.TIMEOUT_TICKS);
        this.frameMap.putAll(itemFrame);
        this.modelMap.putAll(models);
        this.viewMap.putAll(viewMap);
    }
}