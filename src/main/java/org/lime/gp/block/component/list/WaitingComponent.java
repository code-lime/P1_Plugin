package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.WaitingInstance;

@InfoComponent.Component(name = "waiting") public class WaitingComponent extends ComponentDynamic<JsonObject, WaitingInstance> {
    /*
    {
        "progress": 10,
        "total_sec": 3600,
        "input": "Minecraft.STONE*3",
        "fuel": [
            "Minecraft.STONE*2"
        ],
        "catalyse": [
            "Minecraft.STONE*2"
        ],
        "output": "Minecraft.STONE*3"
    }*/
    public final int progress;
    public final int max_count;
    public final String type;

    public WaitingComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        progress = json.get("progress").getAsInt();
        max_count = json.get("max_count").getAsInt();
        type = json.get("type").getAsString();
    }

    @Override public WaitingInstance createInstance(CustomTileMetadata metadata) {
        return new WaitingInstance(this, metadata);
    }
}
