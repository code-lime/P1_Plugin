package org.lime.gp.item.settings.list;

import com.google.gson.JsonPrimitive;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

@Setting(name = "shield_ignore") public class ShieldIgnoreSetting extends ItemSetting<JsonPrimitive> {
    public final double chance;

    public ShieldIgnoreSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        this.chance = json.getAsDouble();
    }
}








