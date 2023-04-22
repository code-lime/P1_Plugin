package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "durability") public class DurabilitySetting extends ItemSetting<JsonPrimitive> {
    public final int maxDurability;
    public DurabilitySetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        maxDurability = json.getAsInt();
    }
}