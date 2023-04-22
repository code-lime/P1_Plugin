package org.lime.gp.item.settings.list;

import java.util.HashMap;
import java.util.Map;

import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;
import org.lime.gp.player.menu.page.Menu;

import com.google.gson.JsonObject;

import net.kyori.adventure.text.Component;

@Setting(name = "vest") public class VestSetting extends ItemSetting<JsonObject> {
    public final int rows;
    public final Component title;
    public final Map<Integer, Checker> slots = new HashMap<>();

    public VestSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);

        this.rows = json.has("rows") ? json.get("rows").getAsInt() : 1;
        this.title = ChatHelper.formatComponent(json.get("title").getAsString());
        json.getAsJsonObject("slots").entrySet().forEach(kv -> {
            Checker checker = Checker.createCheck(kv.getValue().getAsString());
            Menu.rangeOf(kv.getKey()).forEach(slot -> this.slots.put(slot, checker));
        });
    }
}