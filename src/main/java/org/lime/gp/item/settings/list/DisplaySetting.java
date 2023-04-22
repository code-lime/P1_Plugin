package org.lime.gp.item.settings.list;

import java.util.Optional;

import org.bukkit.inventory.ItemStack;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonObject;

@Setting(name = "display") public class DisplaySetting extends ItemSetting<JsonObject> {
    public String item;
    public DisplaySetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        item = json.get("item").getAsString();
    }

    public Optional<ItemStack> item(int original_id) {
        return Items.getItemCreator(item).map(v -> v.createItem(Apply.of().add("original_id", original_id + "")));
    }
}