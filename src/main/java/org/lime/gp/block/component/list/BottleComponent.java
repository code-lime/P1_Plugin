package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.BottleInstance;

@InfoComponent.Component(name = "bottle")
public final class BottleComponent extends ComponentDynamic<JsonObject, BottleInstance> {
    public final int totalLevel;

    public BottleComponent(BlockInfo info, int totalLevel) {
        super(info);
        this.totalLevel = totalLevel;
    }

    public BottleComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        totalLevel = json.has("total_level") ? json.get("total_level").getAsInt() : 0;
    }

    @Override
    public BottleInstance createInstance(CustomTileMetadata metadata) {
        return new BottleInstance(this, metadata);
    }
}
