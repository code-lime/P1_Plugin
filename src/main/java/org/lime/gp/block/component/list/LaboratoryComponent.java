package org.lime.gp.block.component.list;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.gson.JsonObject;
import org.lime.display.models.shadow.IBuilder;
import org.lime.display.transform.LocalLocation;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.LaboratoryInstance;
import org.lime.gp.docs.IDocsLink;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.MathUtils;

@InfoComponent.Component(name = "laboratory")
public final class LaboratoryComponent extends ComponentDynamic<JsonObject, LaboratoryInstance> {
    public final ImmutableList<LocalLocation> input_thirst;
    public final LocalLocation input_dust;

    public final IBuilder model_interact;

    public LaboratoryComponent(BlockInfo creator, JsonObject json) {
        super(creator, json);
        JsonObject input = json.getAsJsonObject("input");
        this.input_thirst = Streams.stream(input.getAsJsonArray("thirst").iterator())
                .map(item -> new LocalLocation(MathUtils.getLocation(null, item.getAsString())))
                .collect(ImmutableList.toImmutableList());
        this.input_dust = new LocalLocation(MathUtils.getLocation(null, input.get("dust").getAsString()));
        this.model_interact = LaboratoryInstance.createInteract(this);
    }

    @Override public LaboratoryInstance createInstance(CustomTileMetadata metadata) { return new LaboratoryInstance(this, metadata); }
    @Override public Class<LaboratoryInstance> classInstance() { return LaboratoryInstance.class; }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("input"), JObject.of(
                        JProperty.require(IName.raw("thirst"), IJElement.anyList(IJElement.link(docs.location())), IComment.text("Список позиций слотов с жидкостью")),
                        JProperty.require(IName.raw("dust"), IJElement.link(docs.location()), IComment.text("Позиция слота с предметами"))
                ), IComment.text("Данные о входных слотах"))
        ), IComment.text("Блок явзяется лабораторией"));
    }
}
