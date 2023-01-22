package org.lime.gp.item.settings.list;

import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "dye_color") public class DyeColorSetting extends ItemSetting<JsonPrimitive> {
    public final boolean dyeColor;
    public DyeColorSetting(Items.ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        dyeColor = json.getAsBoolean();
    }
}