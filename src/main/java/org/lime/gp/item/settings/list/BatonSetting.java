package org.lime.gp.item.settings.list;

import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "baton") public class BatonSetting extends ItemSetting<JsonPrimitive> {
    public final double chance;
    public BatonSetting(Items.ItemCreator creator, JsonPrimitive json) {
        super(creator);
        this.chance = json.getAsDouble();
    }
}