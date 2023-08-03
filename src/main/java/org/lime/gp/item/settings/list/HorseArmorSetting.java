package org.lime.gp.item.settings.list;

import com.google.gson.JsonPrimitive;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

@Setting(name = "horse_armor") public class HorseArmorSetting extends ItemSetting<JsonPrimitive> {
    public final boolean isArmor;
    public HorseArmorSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        isArmor = json.getAsBoolean();
    }
}
