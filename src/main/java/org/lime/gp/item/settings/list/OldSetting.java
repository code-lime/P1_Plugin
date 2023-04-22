package org.lime.gp.item.settings.list;

import java.util.ArrayList;
import java.util.List;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonArray;

//TODO
@Setting(name = "old") public class OldSetting extends ItemSetting<JsonArray> {
    public final List<Integer> old = new ArrayList<>();
    public OldSetting(ItemCreator creator, JsonArray json) {
        super(creator, json);
        json.forEach(item -> old.add(item.getAsInt()));
    }
}