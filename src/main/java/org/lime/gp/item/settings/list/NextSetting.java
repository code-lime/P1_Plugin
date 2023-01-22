package org.lime.gp.item.settings.list;

import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "next") public class NextSetting extends ItemSetting<JsonPrimitive> {
    public final String next;
    public NextSetting(Items.ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        this.next = json.getAsString();
    }
}