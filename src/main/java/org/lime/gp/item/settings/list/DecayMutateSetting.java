package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.lime.gp.block.component.data.BaseAgeableInstance;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.loot.ILoot;
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
}


















