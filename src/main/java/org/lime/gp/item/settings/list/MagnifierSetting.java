package org.lime.gp.item.settings.list;

import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonNull;

@Setting(name = "magnifier") public class MagnifierSetting extends ItemSetting<JsonNull> {
    public MagnifierSetting(Items.ItemCreator creator) { super(creator); }
}