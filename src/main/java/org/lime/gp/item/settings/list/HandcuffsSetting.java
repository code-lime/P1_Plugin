package org.lime.gp.item.settings.list;

import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonNull;

@Setting(name = "handcuffs") public class HandcuffsSetting extends ItemSetting<JsonNull> {
    public HandcuffsSetting(Items.ItemCreator creator) { super(creator); }
}