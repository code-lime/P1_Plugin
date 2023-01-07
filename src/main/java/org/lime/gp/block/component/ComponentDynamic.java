package org.lime.gp.block.component;

import com.google.gson.JsonElement;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.CustomTileMetadata;

public abstract class ComponentDynamic<T extends JsonElement, I extends BlockInstance> extends ComponentStatic<T> {
    public ComponentDynamic(BlockInfo info) { super(info); }
    public ComponentDynamic(BlockInfo info, T json) { super(info, json); }
    public abstract I createInstance(CustomTileMetadata metadata);
}
