package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.ToDoException;
import org.lime.docs.IIndexGroup;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.WaitingInstance;
import org.lime.gp.docs.IDocsLink;

@InfoComponent.Component(name = "waiting") public class WaitingComponent extends ComponentDynamic<JsonObject, WaitingInstance> {
    public final int progress;
    public final int max_count;
    public final String type;

    public final boolean debug;

    public WaitingComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        progress = json.get("progress").getAsInt();
        max_count = json.get("max_count").getAsInt();
        if (max_count > 127) throw new IllegalArgumentException("ITEM COUNT LIMIT IN BLOCK '"+info.getKey()+"': " + max_count + " > 127");
        type = json.get("type").getAsString();
        debug = json.has("debug") && json.get("debug").getAsBoolean();
    }

    @Override public WaitingInstance createInstance(CustomTileMetadata metadata) { return new WaitingInstance(this, metadata); }
    @Override public Class<WaitingInstance> classInstance() { return WaitingInstance.class; }
    @Override public IIndexGroup docs(String index, IDocsLink docs) { throw new ToDoException("BLOCK COMPONENT: " + index); }
}
