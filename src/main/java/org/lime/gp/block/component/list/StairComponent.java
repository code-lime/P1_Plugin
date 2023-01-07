package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.StairInstance;

@InfoComponent.Component(name = "stair")
public final class StairComponent extends ComponentDynamic<JsonObject, StairInstance> {
    public StairComponent(BlockInfo info, JsonObject json) {
        super(info, json);
    }

    @Override
    public StairInstance createInstance(CustomTileMetadata metadata) {
        return new StairInstance(this, metadata);
    }
}
