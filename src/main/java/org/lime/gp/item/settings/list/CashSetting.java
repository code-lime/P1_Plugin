package org.lime.gp.item.settings.list;

import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "cash") public class CashSetting extends ItemSetting<JsonPrimitive> {
    public final int cash;
    public CashSetting(Items.ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        cash = json.getAsInt();
    }
}