package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.DisplayInstance;
import org.lime.gp.block.component.display.DisplayPartial;
import org.lime.gp.module.JavaScript;
import org.lime.system;

import java.util.*;

@InfoComponent.Component(name = "display")
public final class DisplayComponent extends ComponentDynamic<JsonObject, DisplayInstance> {
    public final List<DisplayPartial.Partial> partials = new LinkedList<>();
    public final Map<UUID, DisplayPartial.Partial> partialMap = new HashMap<>();
    public final double maxDistanceSquared;

    public final String animation_tick;
    public final HashMap<String, Object> animation_args = new HashMap<>();

    public void animationTick(Map<String, String> variable, Map<String, String> display_variable, Map<String, Object> data) {
        if (animation_tick == null) return;
        JavaScript.invoke(animation_tick,
                system.map.<String, Object>of()
                        .add(animation_args, k -> k, v -> v)
                        .add("variable", variable)
                        .add("display_variable", display_variable)
                        .add("data", data)
                        .build()
        );
    }

    private static Object toObj(JsonPrimitive json) {
        return json.isNumber() ? json.getAsNumber() : json.isBoolean() ? json.getAsBoolean() : json.getAsString();
    }

    public DisplayComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        if (json.has("animation")) {
            JsonObject animation = json.getAsJsonObject("animation");
            animation_tick = animation.has("tick") ? animation.get("tick").getAsString() : null;
            if (animation.has("args"))
                animation.getAsJsonObject("args").entrySet().forEach(kv -> animation_args.put(kv.getKey(), toObj(kv.getValue().getAsJsonPrimitive())));
        } else {
            animation_tick = null;
        }
        maxDistanceSquared = DisplayPartial.load(info, json, this.partials, this.partialMap);
    }

    public DisplayComponent(BlockInfo info, List<DisplayPartial.Partial> partials) {
        super(info);
        animation_tick = null;
        maxDistanceSquared = DisplayPartial.loadStatic(info, partials, this.partials, this.partialMap);
    }

    @Override
    public DisplayInstance createInstance(CustomTileMetadata metadata) {
        return new DisplayInstance(this, metadata);
    }
}
