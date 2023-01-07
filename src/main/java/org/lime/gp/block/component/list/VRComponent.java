package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.VRInstance;

@InfoComponent.Component(name = "vr")
public final class VRComponent extends ComponentDynamic<JsonObject, VRInstance> {
    public VRComponent(BlockInfo info, JsonObject json) {
        super(info, json);
    }

    @Override
    public VRInstance createInstance(CustomTileMetadata metadata) {
        return new VRInstance(this, metadata);
    }
}
