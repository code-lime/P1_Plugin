package org.lime.gp.item.settings.list;

import java.util.ArrayList;
import java.util.List;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonArray;

@Setting(name = "remote_execute") public class RemoteExecuteSetting extends ItemSetting<JsonArray> {
    public final List<String> execute;
    public RemoteExecuteSetting(ItemCreator creator, JsonArray array) {
        super(creator);
        execute = new ArrayList<>();
        array.forEach(item -> execute.add(item.getAsString()));
    }
}