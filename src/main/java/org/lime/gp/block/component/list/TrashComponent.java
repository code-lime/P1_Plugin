package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.ToDoException;
import org.lime.docs.IIndexGroup;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.StairInstance;
import org.lime.gp.block.component.data.TrashInstance;
import org.lime.gp.docs.IDocsLink;

@InfoComponent.Component(name = "trash")
public final class TrashComponent extends ComponentDynamic<JsonObject, TrashInstance> {
    public TrashComponent(BlockInfo info, JsonObject json) { super(info, json); }

    @Override public TrashInstance createInstance(CustomTileMetadata metadata) { return new TrashInstance(this, metadata); }
    @Override public Class<TrashInstance> classInstance() { return TrashInstance.class; }
    @Override public IIndexGroup docs(String index, IDocsLink docs) { throw new ToDoException("BLOCK COMPONENT: " + index); }
}
