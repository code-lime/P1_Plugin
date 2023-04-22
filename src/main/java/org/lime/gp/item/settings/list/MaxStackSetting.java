package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "max_stack") public class MaxStackSetting extends ItemSetting<JsonPrimitive> {
    public final int maxStack;
    public MaxStackSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        maxStack = json.getAsInt();
    }
}