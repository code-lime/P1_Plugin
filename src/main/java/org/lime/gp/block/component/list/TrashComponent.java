package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.TrashInstance;

@InfoComponent.Component(name = "trash")
public final class TrashComponent extends ComponentDynamic<JsonObject, TrashInstance> {
    public TrashComponent(BlockInfo info, JsonObject json) {
        super(info, json);
    }

    @Override
    public TrashInstance createInstance(CustomTileMetadata metadata) {
        return new TrashInstance(this, metadata);
    }
}
