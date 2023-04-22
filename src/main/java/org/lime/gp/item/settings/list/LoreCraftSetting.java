package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonNull;

@Setting(name = "lore_craft") public class LoreCraftSetting extends ItemSetting<JsonNull> {
    public LoreCraftSetting(ItemCreator creator) {
        super(creator);
    }
}