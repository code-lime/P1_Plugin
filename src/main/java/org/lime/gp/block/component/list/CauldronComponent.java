package org.lime.gp.block.component.list;

import com.google.gson.JsonNull;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.BottleInstance;
import org.lime.gp.block.component.data.CauldronInstance;
import org.lime.gp.docs.IDocsLink;

@InfoComponent.Component(name = "cauldron")
public final class CauldronComponent extends ComponentDynamic<JsonNull, CauldronInstance> {
    public CauldronComponent(BlockInfo info) { super(info); }

    @Override public CauldronInstance createInstance(CustomTileMetadata metadata) { return new CauldronInstance(this, metadata); }
    @Override public Class<CauldronInstance> classInstance() { return CauldronInstance.class; }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.nullable(), "Блок является котлом").withChilds();
    }
}
