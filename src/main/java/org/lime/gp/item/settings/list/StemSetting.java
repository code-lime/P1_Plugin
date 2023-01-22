package org.lime.gp.item.settings.list;

import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonObject;

@Setting(name = "stem") public class StemSetting extends ItemSetting<JsonObject> {
    public final String key;
    public final String result;
    public StemSetting(Items.ItemCreator creator, JsonObject json) {
        super(creator, json);
        key = json.get("key").getAsString();
        result = json.get("result").getAsString();
    }
}