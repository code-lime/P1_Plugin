package org.lime.gp.entity.component.list;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.lime.display.models.shadow.IBuilder;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityInfo;
import org.lime.gp.entity.component.ComponentDynamic;
import org.lime.gp.entity.component.InfoComponent;
import org.lime.gp.entity.component.display.DisplayInstance;
import org.lime.gp.entity.component.display.DisplayPartial;
import org.lime.gp.module.JavaScript;
import org.lime.system.map;

import java.util.*;

@InfoComponent.Component(name = "display")
public final class DisplayComponent extends ComponentDynamic<JsonObject, DisplayInstance> {
    public final List<DisplayPartial.Partial> partials = new LinkedList<>();
    public final Map<UUID, DisplayPartial.Partial> partialMap = new HashMap<>();
    public final double maxDistanceSquared;
    public final String animation_tick;
    public final HashMap<String, Object> animation_args = new HashMap<>();

    public void animationTick(Map<String, Object> variable, Map<String, Object> data) {
        if (animation_tick == null) return;
        JavaScript.invoke(animation_tick,
                map.<String, Object>of()
                        .add(animation_args, k -> k, v -> v)
                        .add("variable", variable)
                        .add("data", data)
                        .build()
        );
    }

    private static Object toObj(JsonPrimitive json) {
        return json.isNumber() ? json.getAsNumber() : json.isBoolean() ? json.getAsBoolean() : json.getAsString();
    }

    public DisplayComponent(EntityInfo info, JsonObject json) {
        super(info, json);
        if (json.has("animation")) {
            JsonObject animation = json.getAsJsonObject("animation");
            animation_tick = animation.has("tick") ? animation.get("tick").getAsString() : null;
            if (animation.has("args"))
                animation.getAsJsonObject("args").entrySet().forEach(kv -> animation_args.put(kv.getKey(), toObj(kv.getValue().getAsJsonPrimitive())));
        } else {
            animation_tick = null;
        }
        maxDistanceSquared = DisplayPartial.load(info, json.getAsJsonObject("partial"), partials, partialMap);
    }

    public DisplayComponent(EntityInfo info, IBuilder model) {
        super(info);
        animation_tick = null;
        DisplayPartial.ModelPartial partial = new DisplayPartial.ModelPartial(-1, model);
        partials.add(partial);
        partialMap.put(partial.uuid, partial);
        maxDistanceSquared = -1;
    }

    @Override
    public DisplayInstance createInstance(CustomEntityMetadata metadata) {
        return new DisplayInstance(this, metadata);
    }
}
