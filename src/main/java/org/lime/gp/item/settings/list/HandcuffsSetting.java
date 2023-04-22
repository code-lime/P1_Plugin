package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonNull;

@Setting(name = "handcuffs") public class HandcuffsSetting extends ItemSetting<JsonNull> {
    public HandcuffsSetting(ItemCreator creator) { super(creator); }
}