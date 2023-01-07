package org.lime.gp.block.component.list;

import com.google.gson.JsonNull;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.PaintingInstance;

@InfoComponent.Component(name = "painting")
public final class PaintingComponent extends ComponentDynamic<JsonNull, PaintingInstance> {
    public PaintingComponent(BlockInfo info) {
        super(info);
    }

    @Override
    public PaintingInstance createInstance(CustomTileMetadata metadata) {
        return new PaintingInstance(this, metadata);
    }
}
