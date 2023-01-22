package org.lime.gp.item.settings.list;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.system;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;
import org.lime.gp.player.voice.RadioData;

import com.google.gson.JsonObject;

@Setting(name = "radio") public class RadioSetting extends ItemSetting<JsonObject> {
    public final int min_level;// 1490,
    public final int def_level;
    public final int max_level;// 1740,
    public final int on;// 19,
    public final int off;// 20
    public final short total_distance;
    public final boolean is_on;
    public final RadioData.RadioState state;
    public RadioSetting(Items.ItemCreator creator, JsonObject json) {
        super(creator, json);
        min_level = json.get("min_level").getAsInt();
        def_level = json.get("def_level").getAsInt();
        max_level = json.get("max_level").getAsInt();
        total_distance = json.get("total_distance").getAsShort();
        on = json.get("on").getAsInt();
        off = json.get("off").getAsInt();
        is_on = !json.has("is_on") || json.get("is_on").getAsBoolean();
        state = json.has("state") ? RadioData.RadioState.valueOf(json.get("state").getAsString()) : RadioData.RadioState.all;
    }

    @Override public void apply(ItemStack item, ItemMeta meta, Apply apply) {
        List<system.Action1<RadioData>> modifyRadio = new ArrayList<>();
        apply.get("level").map(Integer::parseInt).ifPresent(level -> modifyRadio.add(data -> data.level = level));
        apply.get("state").map(Boolean::parseBoolean).ifPresent(state -> modifyRadio.add(data -> data.enable = state));
        if (!modifyRadio.isEmpty()) RadioData.modifyData(this, meta, data -> modifyRadio.forEach(action -> action.invoke(data)));
    }
}