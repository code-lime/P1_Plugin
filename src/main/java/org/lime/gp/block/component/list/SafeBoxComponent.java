package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.SafeBoxInstance;

@InfoComponent.Component(name = "safe_box")
public final class SafeBoxComponent extends ComponentDynamic<JsonObject, SafeBoxInstance> {
    public final boolean small;
    public final String insert;
    public final int close_time;
    public final String sound_crash;
    public final String sound_progress;
    public final String sound_open;

    public SafeBoxComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        this.small = !json.has("small") || json.get("small").getAsBoolean();
        this.insert = json.has("insert") ? json.get("insert").getAsString() : null;
        this.close_time = json.get("close_time").getAsInt();
        this.sound_crash = json.has("sound_crash") ? json.get("sound_crash").getAsString() : null;
        this.sound_progress = json.has("sound_progress") ? json.get("sound_progress").getAsString() : null;
        this.sound_open = json.has("sound_open") ? json.get("sound_open").getAsString() : null;
    }

    @Override
    public SafeBoxInstance createInstance(CustomTileMetadata metadata) {
        return new SafeBoxInstance(this, metadata);
    }
}
