package org.lime.gp.item.settings.list;

import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonNull;

@Setting(name = "backpack_drop") public class BackPackDropSetting extends ItemSetting<JsonNull> {
    public BackPackDropSetting(Items.ItemCreator creator) {
        super(creator);
    }
}