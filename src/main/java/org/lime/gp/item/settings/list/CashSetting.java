package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "cash") public class CashSetting extends ItemSetting<JsonPrimitive> {
    public final int cash;
    public CashSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        cash = json.getAsInt();
    }
}