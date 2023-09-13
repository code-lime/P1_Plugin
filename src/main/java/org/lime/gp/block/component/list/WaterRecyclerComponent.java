package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.ToDoException;
import org.lime.docs.IIndexGroup;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.WaterRecyclerInstance;
import org.lime.gp.docs.IDocsLink;

@InfoComponent.Component(name = "water_recycler")
public final class WaterRecyclerComponent extends ComponentDynamic<JsonObject, WaterRecyclerInstance> {
    public final int totalWaterLevel;
    public final int totalClearLevel;
    public final double inTickLevel;

    public WaterRecyclerComponent(BlockInfo info, int totalWaterLevel, int totalClearLevel, double inTickLevel) {
        super(info);
        this.totalWaterLevel = totalWaterLevel;
        this.totalClearLevel = totalClearLevel;
        this.inTickLevel = inTickLevel;
    }

    public WaterRecyclerComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        totalWaterLevel = json.has("total_water_level") ? json.get("total_water_level").getAsInt() : 0;
        totalClearLevel = json.has("total_clear_level") ? json.get("total_clear_level").getAsInt() : 0;
        inTickLevel = json.has("in_tick_level") ? json.get("in_tick_level").getAsDouble() : 0;
    }

    @Override public WaterRecyclerInstance createInstance(CustomTileMetadata metadata) { return new WaterRecyclerInstance(this, metadata); }
    @Override public Class<WaterRecyclerInstance> classInstance() { return WaterRecyclerInstance.class; }

    @Override public IIndexGroup docs(String index, IDocsLink docs) { throw new ToDoException("BLOCK COMPONENT: " + index); }
}
