package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.ToDoException;
import org.lime.docs.IIndexGroup;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.ShrubInstance;
import org.lime.gp.block.component.data.StairInstance;
import org.lime.gp.docs.IDocsLink;

@InfoComponent.Component(name = "stair")
public final class StairComponent extends ComponentDynamic<JsonObject, StairInstance> {
    public StairComponent(BlockInfo info, JsonObject json) {
        super(info, json);
    }

    @Override public StairInstance createInstance(CustomTileMetadata metadata) { return new StairInstance(this, metadata); }
    @Override public Class<StairInstance> classInstance() { return StairInstance.class; }
    @Override public IIndexGroup docs(String index, IDocsLink docs) { throw new ToDoException("BLOCK COMPONENT: " + index); }
}
