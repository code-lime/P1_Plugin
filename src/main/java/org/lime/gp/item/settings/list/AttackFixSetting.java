package org.lime.gp.item.settings.list;

import com.google.gson.JsonNull;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

@Setting(name = "attack_fix") public class AttackFixSetting extends ItemSetting<JsonNull> {
    public AttackFixSetting(ItemCreator creator, JsonNull json) {
        super(creator, json);
    }
}
