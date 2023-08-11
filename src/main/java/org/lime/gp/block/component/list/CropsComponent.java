package org.lime.gp.block.component.list;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.bukkit.util.Vector;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.CropsInstance;
import org.lime.gp.item.data.Checker;
import org.lime.system;

@InfoComponent.Component(name = "crops") public class CropsComponent extends ComponentDynamic<JsonObject, CropsInstance> {
    public final Checker filter;
    public final Transformation offset;
    public CropsComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        JsonElement filter = json.get("filter");
        if (filter.isJsonArray()) this.filter = Checker.createCheck(filter.getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList());
        else if (filter.isJsonPrimitive()) this.filter = Checker.createCheck(filter.getAsString());
        else throw new IllegalArgumentException("Field 'filter' in `crops` with value '"+filter+"' not supported");
        this.offset = json.has("offset") ? system.transformation(json.get("offset")) : Transformation.identity();
    }

    @Override public CropsInstance createInstance(CustomTileMetadata metadata) {
        return new CropsInstance(this, metadata);
    }
}




















