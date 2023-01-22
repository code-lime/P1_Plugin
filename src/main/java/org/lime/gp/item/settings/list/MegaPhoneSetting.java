package org.lime.gp.item.settings.list;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.system;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;
import org.lime.gp.player.voice.MegaPhoneData;

import com.google.gson.JsonObject;

@Setting(name = "megaphone") public class MegaPhoneSetting extends ItemSetting<JsonObject> {
    public final short def_distance;// 16
    public final short min_distance;// 0
    public final short max_distance;// 32
    public MegaPhoneSetting(Items.ItemCreator creator, JsonObject json) {
        super(creator);
        def_distance = json.get("def_distance").getAsShort();
        min_distance = json.get("min_distance").getAsShort();
        max_distance = json.get("max_distance").getAsShort();
    }

    @Override public void apply(ItemStack item, ItemMeta meta, Apply apply) {
        List<system.Action1<MegaPhoneData>> modify = new ArrayList<>();
        apply.get("distance").map(Short::parseShort).ifPresent(distance -> modify.add(data -> data.distance = distance));
        apply.get("volume").map(Integer::parseInt).ifPresent(volume -> modify.add(data -> data.volume = volume));
        if (!modify.isEmpty()) MegaPhoneData.modifyData(this, meta, data -> modify.forEach(action -> action.invoke(data)));
    }
}