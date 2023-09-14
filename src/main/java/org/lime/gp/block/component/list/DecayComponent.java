package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.DecayInstance;
import org.lime.gp.docs.IDocsLink;
import org.lime.system.range.*;

@InfoComponent.Component(name = "decay") public class DecayComponent extends ComponentDynamic<JsonObject, DecayInstance> {
    public final IRange ticks;
    public final String replace;
    public final int displayCount;
    
    public DecayComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        ticks = IRange.parse(json.get("ticks").getAsString());
        replace = json.get("replace").getAsString();
        displayCount = json.has("display_count") ? json.get("display_count").getAsInt() : 0;
    }

    public double tickDecayModify() { return 1 / totalDecay(); }
    public double totalDecay() { return ticks.getValue(100.0); }
    public double valueDecayModify(double value) { return 1 / value; }

    @Override public DecayInstance createInstance(CustomTileMetadata metadata) { return new DecayInstance(this, metadata); }
    @Override public Class<DecayInstance> classInstance() { return DecayInstance.class; }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("ticks"), IJElement.link(docs.range()), IComment.text("Время, через которое блок изменится")),
                JProperty.require(IName.raw("replace"), IJElement.link(docs.setBlock()), IComment.text("Блок, на который произойдет замена")),
                JProperty.optional(IName.raw("display_count"), IJElement.raw(10), IComment.text("Количество целых частей в прогрессе"))
        ), "Изменение блока по прохождению определенного времени");
    }
}
