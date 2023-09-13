package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.component.list.DecayComponent;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.system;

@Setting(name = "decay_mutate") public class DecayMutateSetting extends ItemSetting<JsonObject> {
    public final system.IRange mutate;

    public DecayMutateSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        mutate = system.IRange.parse(json.get("mutate").getAsString());
    }

    public double decayDelta(double total) {
        return mutate.getValue(total);
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("mutate"), IJElement.link(docs.range()), IComment.text("Количество восстанавливаемых тиков гниения"))
        ), "Удобрение-модификатор для блока " + docs.componentsLink(DecayComponent.class).link());
    }
}


















