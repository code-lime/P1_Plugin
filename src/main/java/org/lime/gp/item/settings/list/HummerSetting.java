package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonNull;

@Setting(name = "hummer") public class HummerSetting extends ItemSetting<JsonNull> {
    public HummerSetting(ItemCreator creator) { super(creator); }
}