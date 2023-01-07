package org.lime.gp.block.component.list;

import com.google.gson.JsonNull;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.CauldronInstance;

@InfoComponent.Component(name = "cauldron")
public final class CauldronComponent extends ComponentDynamic<JsonNull, CauldronInstance> {
    public CauldronComponent(BlockInfo info) {
        super(info);
    }

    @Override
    public CauldronInstance createInstance(CustomTileMetadata metadata) {
        return new CauldronInstance(this, metadata);
    }
}
