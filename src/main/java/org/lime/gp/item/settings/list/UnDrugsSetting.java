package org.lime.gp.item.settings.list;

import org.lime.gp.item.Items;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

import com.google.gson.JsonPrimitive;

@Setting(name = "undrugs") public class UnDrugsSetting extends ItemSetting<JsonPrimitive> {
    public final double time;
    public UnDrugsSetting(Items.ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        this.time = json.getAsDouble();
    }
}