package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.DecayInstance;
import org.lime.system;

@InfoComponent.Component(name = "decay") public class DecayComponent extends ComponentDynamic<JsonObject, DecayInstance> {
    public final system.IRange ticks;
    public final Material replace;
    public final int displayCount;
    
    public DecayComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        ticks = system.IRange.parse(json.get("ticks").getAsString());
        replace = Material.valueOf(json.get("replace").getAsString());
        displayCount = json.has("display_count") ? json.get("display_count").getAsInt() : 0;
    }

    public double tickDecayModify() {
        return 1 / totalDecay();
    }
    public double totalDecay() {
        return ticks.getValue(100.0);
    }
    public double valueDecayModify(double value) {
        return 1 / value;
    }

    @Override public DecayInstance createInstance(CustomTileMetadata metadata) {
        return new DecayInstance(this, metadata);
    }
}
