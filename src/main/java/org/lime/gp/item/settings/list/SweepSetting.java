package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "sweep") public class SweepSetting extends ItemSetting<JsonPrimitive> {
    public final boolean sweep;
    public SweepSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        sweep = json.getAsBoolean();
    }
}