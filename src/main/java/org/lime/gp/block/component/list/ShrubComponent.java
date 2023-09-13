package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.ToDoException;
import org.lime.docs.IIndexGroup;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.BaseAgeableInstance;
import org.lime.gp.block.component.data.ShrubInstance;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.loot.ILoot;
import org.lime.system;

@InfoComponent.Component(name = "shrub") public class ShrubComponent extends ComponentDynamic<JsonObject, ShrubInstance> implements BaseAgeableInstance.AgeableData {
    public final int ageCount;
    public final int ageRemove;

    public final system.IRange ageStepTicks;
    public final ILoot loot;

    public ShrubComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        JsonObject age = json.get("age").getAsJsonObject();
        ageCount = age.get("count").getAsInt();
        ageRemove = Math.max(age.has("remove") ? age.get("remove").getAsInt() : 1, 1);
        ageStepTicks = system.IRange.parse(age.get("step_ticks").getAsString());
        loot = ILoot.parse(json.get("loot"));
    }

    @Override public double tickAgeModify() { return 1 / ageStepTicks.getValue(100.0); }
    @Override public int limitAge() { return ageCount; }

    @Override public ShrubInstance createInstance(CustomTileMetadata metadata) { return new ShrubInstance(this, metadata); }
    @Override public Class<ShrubInstance> classInstance() { return ShrubInstance.class; }
    @Override public IIndexGroup docs(String index, IDocsLink docs) { throw new ToDoException("BLOCK COMPONENT: " + index); }
}















