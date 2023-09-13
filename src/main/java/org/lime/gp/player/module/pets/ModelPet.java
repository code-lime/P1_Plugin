package org.lime.gp.player.module.pets;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.lime.display.models.display.BaseChildDisplay;
import org.lime.display.models.shadow.Builder;
import org.lime.display.models.shadow.IBuilder;
import org.lime.gp.lime;
import org.lime.gp.module.JavaScript;
import org.lime.system;

import java.util.HashMap;
import java.util.Map;

public class ModelPet extends AbstractPet {
    public final IBuilder model;
    public final String animation_tick;
    public final HashMap<String, Object> animation_args = new HashMap<>();

    @Override public IBuilder model() { return model; }

    @Override
    public void tick(BaseChildDisplay<?, ?, ?> model, Map<String, Object> data) {
        if (animation_tick == null) return;
        JavaScript.invoke(animation_tick,
                system.map.<String, Object>of()
                        .add(animation_args, k -> k, v -> v)
                        .add("data", data)
                        .build()
        );
        this.model.animation().apply(model.js, data);
    }

    private static Object toObj(JsonPrimitive json) {
        return json.isNumber() ? json.getAsNumber() : json.isBoolean() ? json.getAsBoolean() : json.getAsString();
    }

    protected ModelPet(String key, JsonObject json) {
        super(key, json);
        String modelKey = json.get("model").getAsString();
        if (json.has("animation")) {
            JsonObject animation = json.getAsJsonObject("animation");
            animation_tick = animation.has("tick") ? animation.get("tick").getAsString() : null;
            if (animation.has("args"))
                animation.getAsJsonObject("args").entrySet().forEach(kv -> animation_args.put(kv.getKey(), toObj(kv.getValue().getAsJsonPrimitive())));
        } else {
            animation_tick = null;
        }
        this.model = lime.models.get(modelKey).orElseGet(() -> {
            lime.logOP("Model '" + modelKey + "' in pet '" + key + "' not founded!");
            return lime.models.builder().none();
        });
    }
}
