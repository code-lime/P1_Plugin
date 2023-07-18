package org.lime.gp.item.settings.list;

import com.google.gson.JsonElement;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

import java.util.ArrayList;
import java.util.List;

@Setting(name = "bait") public class BaitSetting extends ItemSetting<JsonElement> {
    public final List<String> tags = new ArrayList<>();

    public BaitSetting(ItemCreator creator, JsonElement json) {
        super(creator, json);
        if (json.isJsonArray()) json.getAsJsonArray().forEach(item -> tags.add(item.getAsString()));
        else tags.add(json.getAsString());
    }
}