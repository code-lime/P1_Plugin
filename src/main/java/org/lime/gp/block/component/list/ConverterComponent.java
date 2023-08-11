package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.ConverterInstance;
import org.lime.system;

@InfoComponent.Component(name = "converter")
public final class ConverterComponent extends ComponentDynamic<JsonObject, ConverterInstance> {
    public final String converter_type;
    public final Transformation offset;

    public ConverterComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        this.offset = json.has("offset") ? system.transformation(json.get("offset")) : Transformation.identity();
        this.converter_type = json.get("converter_type").getAsString();
    }

    @Override
    public ConverterInstance createInstance(CustomTileMetadata metadata) {
        return new ConverterInstance(this, metadata);
    }
}
