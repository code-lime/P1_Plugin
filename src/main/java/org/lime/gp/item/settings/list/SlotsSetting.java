package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "slots") public class SlotsSetting extends ItemSetting<JsonPrimitive> {
    public final int slots;

    public SlotsSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        this.slots = json.getAsInt();
    }
}