package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;

import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.UsestationInstance;

@InfoComponent.Component(name = "medstation")
public final class UsestationComponent extends ComponentDynamic<JsonObject, UsestationInstance> {
    public final int distance;
    public UsestationComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        this.distance = json.get("distance").getAsInt();
    }

    @Override public UsestationInstance createInstance(CustomTileMetadata metadata) {
        return new UsestationInstance(this, metadata);
    }
}
