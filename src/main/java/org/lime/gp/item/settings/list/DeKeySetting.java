package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonNull;

@Setting(name = "de_key") public class DeKeySetting extends ItemSetting<JsonNull> {
    public DeKeySetting(ItemCreator creator) {
        super(creator);
    }
}