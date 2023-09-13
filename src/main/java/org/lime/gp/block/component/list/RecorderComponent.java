package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.ToDoException;
import org.lime.docs.IIndexGroup;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.voice.RecorderInstance;
import org.lime.gp.docs.IDocsLink;

@InfoComponent.Component(name = "recorder")
public final class RecorderComponent extends ComponentDynamic<JsonObject, RecorderInstance> {
    public RecorderComponent(BlockInfo info, JsonObject json) {
        super(info, json);
    }

    @Override public RecorderInstance createInstance(CustomTileMetadata metadata) { return new RecorderInstance(this, metadata); }
    @Override public Class<RecorderInstance> classInstance() { return RecorderInstance.class; }
    @Override public IIndexGroup docs(String index, IDocsLink docs) { throw new ToDoException("BLOCK COMPONENT: " + index); }
}
