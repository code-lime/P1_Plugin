package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.OtherGenericInstance;

@InfoComponent.Component(name = "other.generic")
public final class OtherGenericComponent extends ComponentDynamic<JsonObject, OtherGenericInstance> {
    public final boolean interactOwner;

    public OtherGenericComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        interactOwner = !json.has("interact_owner") || json.get("interact_owner").getAsBoolean();
    }

    @Override
    public OtherGenericInstance createInstance(CustomTileMetadata metadata) {
        return new OtherGenericInstance(this, metadata);
    }
}
