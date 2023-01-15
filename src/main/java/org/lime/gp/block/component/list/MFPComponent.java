package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.bukkit.util.Vector;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.MFPInstance;
import org.lime.system;

@InfoComponent.Component(name = "mfp")
public final class MFPComponent extends ComponentDynamic<JsonObject, MFPInstance> {
    public final Vector offset;

    public MFPComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        this.offset = json.has("offset") ? system.getVector(json.get("offset").getAsString()) : new Vector(0, 0, 0);
    }

    @Override
    public MFPInstance createInstance(CustomTileMetadata metadata) {
        return new MFPInstance(this, metadata);
    }
}
