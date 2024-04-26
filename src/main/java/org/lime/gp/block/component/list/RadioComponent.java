package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.ToDoException;
import org.lime.docs.IIndexGroup;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.voice.RadioInstance;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.player.voice.RadioData;

import javax.annotation.Nullable;

@InfoComponent.Component(name = "radio")
public final class RadioComponent extends ComponentDynamic<JsonObject, RadioInstance> {
    public final int min_level;
    public final int max_level;
    public final int def_level;

    public final int total_distance;
    public final short min_distance;
    public final short max_distance;
    public final short def_distance;
    public final RadioData.RadioState state;
    public final @Nullable String category;

    public final boolean noise;

    public int rangeLevel(int level) {
        return Math.max(Math.min(level, max_level), min_level);
    }

    public int anyLevel() {
        return rangeLevel((max_level + min_level) / 2);
    }

    public short rangeDistance(int distance) {
        return (short) Math.max(Math.min(distance, max_distance), min_distance);
    }

    public short anyDistance() {
        return rangeDistance((max_distance + min_distance) / 2);
    }

    public RadioComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        min_level = json.get("min_level").getAsInt();
        max_level = json.get("max_level").getAsInt();
        def_level = json.get("def_level").getAsInt();

        total_distance = json.get("total_distance").getAsInt();
        min_distance = json.get("min_distance").getAsShort();
        max_distance = json.get("max_distance").getAsShort();
        def_distance = json.get("def_distance").getAsShort();
        
        noise = json.has("noise") ? json.get("noise").getAsBoolean() : false;
        
        state = json.has("state") ? RadioData.RadioState.valueOf(json.get("state").getAsString()) : RadioData.RadioState.all;
        category = json.has("category") ? json.get("category").getAsString() : null;
    }

    @Override public RadioInstance createInstance(CustomTileMetadata metadata) { return new RadioInstance(this, metadata); }
    @Override public Class<RadioInstance> classInstance() { return RadioInstance.class; }
    @Override public IIndexGroup docs(String index, IDocsLink docs) { throw new ToDoException("BLOCK COMPONENT: " + index); }
}
