package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "dye_color") public class DyeColorSetting extends ItemSetting<JsonPrimitive> {
    public final boolean dyeColor;
    public DyeColorSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        dyeColor = json.getAsBoolean();
    }
}