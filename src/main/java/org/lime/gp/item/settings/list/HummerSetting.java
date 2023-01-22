package org.lime.gp.item.settings.list;

import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonNull;

@Setting(name = "hummer") public class HummerSetting extends ItemSetting<JsonNull> {
    public HummerSetting(Items.ItemCreator creator) { super(creator); }
}