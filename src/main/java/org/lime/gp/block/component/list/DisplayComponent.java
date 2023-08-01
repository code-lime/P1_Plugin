package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.display.partial.Partial;
import org.lime.gp.block.component.display.partial.PartialLoader;
import org.lime.gp.module.JavaScript;
import org.lime.system;

import java.util.*;

@InfoComponent.Component(name = "display")
public final class DisplayComponent extends ComponentDynamic<JsonObject, DisplayInstance> {
    public final Map<Integer, Partial> partials;
    public final Map<UUID, Partial> partialMap = new HashMap<>();
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
        LinkedList<Partial> partials = new LinkedList<>();
        maxDistanceSquared = PartialLoader.load(info, json, partials, this.partialMap);
        this.partials = createPartials(info, partials);
    }

    public DisplayComponent(BlockInfo info, List<Partial> partials) {
        super(info);
        animation_tick = null;
        LinkedList<Partial> _partials = new LinkedList<>();
        maxDistanceSquared = PartialLoader.loadStatic(info, partials, _partials, this.partialMap);
        this.partials = createPartials(info, _partials);
    }

    private static Map<Integer, Partial> createPartials(BlockInfo info, LinkedList<Partial> partials) {
        int lenght = partials.size();
        if (lenght == 0) return Collections.emptyMap();
        HashMapWithDefault<Integer, Partial> outPartials = new HashMapWithDefault<>(partials.get(0));
        Partial last = null;
        for (int i = lenght - 1; i >= 0; i--) {
            Partial partial = partials.get(i);
            if (last != null) {
                int lastDistanceChunk = last.distanceChunk();
                int delta = partial.distanceChunk() - lastDistanceChunk;
                for (int _i = 1; _i < delta; _i++) {
                    outPartials.put(lastDistanceChunk + _i, last);
                }
            }
            if (outPartials.putIfAbsent(partial.distanceChunk(), partial) == null) last = partial;
        }
        return outPartials;
    }
    
    @Override
    public DisplayInstance createInstance(CustomTileMetadata metadata) {
        return new DisplayInstance(this, metadata);
    }

    private static class HashMapWithDefault<Key,Value> extends HashMap<Key, Value> {
        private final Value defaultValue;

        public HashMapWithDefault(Value defaultValue) {
            this.defaultValue = defaultValue;
        }
        @Override public Value get(Object key) {
            return super.getOrDefault(key, defaultValue);
        }
    }
}
