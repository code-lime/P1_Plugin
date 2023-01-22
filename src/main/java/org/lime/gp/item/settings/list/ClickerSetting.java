package org.lime.gp.item.settings.list;

import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "clicker") public class ClickerSetting extends ItemSetting<JsonPrimitive> {
    public final String type;
    public ClickerSetting(Items.ItemCreator creator, JsonPrimitive json) { super(creator); this.type = json.getAsString(); }
}