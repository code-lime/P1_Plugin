package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonNull;

@Setting(name = "brush") public class BrushSetting extends ItemSetting<JsonNull> {
    public BrushSetting(ItemCreator creator) { super(creator); }
}