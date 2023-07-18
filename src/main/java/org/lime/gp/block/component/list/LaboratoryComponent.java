package org.lime.gp.block.component.list;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.gson.JsonObject;
import org.lime.display.models.Model;
import org.lime.display.transform.LocalLocation;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.LaboratoryInstance;
import org.lime.system;

@InfoComponent.Component(name = "laboratory")
public final class LaboratoryComponent extends ComponentDynamic<JsonObject, LaboratoryInstance> {
    public final ImmutableList<LocalLocation> input_thirst;
    public final LocalLocation input_dust;
    public final LocalLocation output;

    public final Model model_interact;

    public LaboratoryComponent(BlockInfo creator, JsonObject json) {
        super(creator, json);
        JsonObject input = json.getAsJsonObject("input");
        this.input_thirst = Streams.stream(input.getAsJsonArray("thirst").iterator()).map(item -> new LocalLocation(system.getLocation(null, item.getAsString()))).collect(ImmutableList.toImmutableList());
        this.input_dust = new LocalLocation(system.getLocation(null, input.get("dust").getAsString()));
        this.output = new LocalLocation(system.getLocation(null, json.get("output").getAsString()));
        this.model_interact = LaboratoryInstance.createInteract(this);
    }

    @Override
    public LaboratoryInstance createInstance(CustomTileMetadata metadata) {
        return new LaboratoryInstance(this, metadata);
    }
}
