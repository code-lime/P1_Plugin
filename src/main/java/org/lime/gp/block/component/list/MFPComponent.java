package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.bukkit.util.Vector;
import org.lime.ToDoException;
import org.lime.docs.IIndexGroup;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.MFPInstance;
import org.lime.gp.docs.IDocsLink;
import org.lime.system;

@InfoComponent.Component(name = "mfp")
public final class MFPComponent extends ComponentDynamic<JsonObject, MFPInstance> {
    public final Vector offset;

    public final double out_rotation;
    public final Vector out_offset;

    public MFPComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        this.offset = json.has("offset") ? system.getVector(json.get("offset").getAsString()) : new Vector(0, 0, 0);
        this.out_rotation = json.has("out_rotation") ? json.get("out_rotation").getAsDouble() : 0;
        this.out_offset = json.has("out_offset") ? system.getVector(json.get("out_offset").getAsString()) : new Vector(0, 0, 0);
    }

    @Override public MFPInstance createInstance(CustomTileMetadata metadata) { return new MFPInstance(this, metadata); }
    @Override public Class<MFPInstance> classInstance() { return MFPInstance.class; }
    @Override public IIndexGroup docs(String index, IDocsLink docs) { throw new ToDoException("BLOCK COMPONENT: " + index); }
}
