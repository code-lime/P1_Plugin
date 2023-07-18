package org.lime.gp.block.component.display.partial.list;

import java.util.Optional;

import org.lime.display.models.Model;
import org.lime.gp.lime;
import org.lime.gp.block.component.display.partial.PartialEnum;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ModelPartial extends FramePartial {
    private final String model;
    private Model generic = null;

    public ModelPartial(int distanceChunk, JsonObject json) {
        super(distanceChunk, json);
        this.model = parseModel(json.get("model"));
    }

    private String parseModel(JsonElement json) {
        if (json.isJsonPrimitive()) return json.getAsString();
        generic = lime.models.parse(json.getAsJsonObject());
        return "#generic";
    }

    public Optional<Model> model() {
        return Optional.ofNullable(generic).or(() -> lime.models.get(model));
    }

    @Override public PartialEnum type() { return PartialEnum.Model; }
    @Override public String toString() { return super.toString()+ "^" + model; }
}