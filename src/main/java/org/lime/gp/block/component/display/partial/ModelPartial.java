package org.lime.gp.block.component.display.partial;

import java.util.Optional;

import org.lime.display.Models;
import org.lime.gp.lime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ModelPartial extends FramePartial {
    private final String model;
    private Models.Model generic = null;

    public ModelPartial(int distanceChunk, JsonObject json) {
        super(distanceChunk, json);
        this.model = parseModel(json.get("model"));
    }

    private String parseModel(JsonElement json) {
        if (json.isJsonPrimitive()) return json.getAsString();
        generic = lime.models.parse(json.getAsJsonObject());
        return "#generic";
    }

    public Optional<Models.Model> model() {
        return Optional.ofNullable(generic).or(() -> lime.models.get(model));
    }

    @Override public PartialEnum type() { return PartialEnum.Model; }
    @Override public String toString() { return super.toString()+ "^" + model; }
}