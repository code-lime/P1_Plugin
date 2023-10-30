package org.lime.gp.entity.component.list;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.lime.display.models.ExecutorJavaScript;
import org.lime.display.models.shadow.IBuilder;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityInfo;
import org.lime.gp.entity.component.ComponentDynamic;
import org.lime.gp.entity.component.InfoComponent;
import org.lime.gp.entity.component.display.instance.DisplayInstance;
import org.lime.gp.entity.component.display.partial.PartialLoader;
import org.lime.gp.entity.component.display.partial.Partial;
import org.lime.gp.entity.component.display.partial.list.ModelPartial;
import org.lime.gp.module.JavaScript;
import org.lime.system.map;

import java.util.*;

@InfoComponent.Component(name = "display")
public final class DisplayComponent extends ComponentDynamic<JsonObject, DisplayInstance> {
    public final Map<Integer, Partial> partials;
    public final Map<UUID, Partial> partialMap = new HashMap<>();
    public final double maxDistanceSquared;
    public final ExecutorJavaScript animation;

    public void animationTick(Map<String, String> variable, Map<String, String> display_variable, Map<String, Object> data) {
        animation.execute(Map.of("variable", variable, "display_variable", display_variable, "data", data));
    }

    public DisplayComponent(EntityInfo info, JsonObject json) {
        super(info, json);
        if (json.has("animation")) {
            JsonObject animation = json.getAsJsonObject("animation");
            this.animation = new ExecutorJavaScript("tick", animation, JavaScript.js);
        } else {
            this.animation = ExecutorJavaScript.empty();
        }
        LinkedList<Partial> partials = new LinkedList<>();
        maxDistanceSquared = PartialLoader.load(info, json.getAsJsonObject("partial"), partials, partialMap);
        this.partials = createPartials(info, partials);
    }
/*
    public DisplayComponent(EntityInfo info, IBuilder model) {
        super(info);
        animation_tick = null;
        ModelPartial partial = new ModelPartial(-1, model);
        partials.add(partial);
        partialMap.put(partial.uuid, partial);
        maxDistanceSquared = -1;
    }
*/

    private static class HashMapWithDefault<Key,Value> extends HashMap<Key, Value> {
        private final Value defaultValue;

        public HashMapWithDefault(Value defaultValue) {
            this.defaultValue = defaultValue;
        }
        @Override public Value get(Object key) {
            return super.getOrDefault(key, defaultValue);
        }
    }
    private static Map<Integer, Partial> createPartials(EntityInfo info, LinkedList<Partial> partials) {
        int length = partials.size();
        if (length == 0) return Collections.emptyMap();
        HashMapWithDefault<Integer, Partial> outPartials = new HashMapWithDefault<>(partials.get(0));
        Partial last = null;
        for (int i = length - 1; i >= 0; i--) {
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
    public DisplayInstance createInstance(CustomEntityMetadata metadata) {
        return new DisplayInstance(this, metadata);
    }
}
