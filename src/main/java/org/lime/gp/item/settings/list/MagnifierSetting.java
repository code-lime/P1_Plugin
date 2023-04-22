package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonNull;

@Setting(name = "magnifier") public class MagnifierSetting extends ItemSetting<JsonNull> {
    public MagnifierSetting(ItemCreator creator) { super(creator); }
}