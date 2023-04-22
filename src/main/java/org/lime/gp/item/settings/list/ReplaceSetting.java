package org.lime.gp.item.settings.list;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.system;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonObject;

@Setting(name = "replace") public class ReplaceSetting extends ItemSetting<JsonObject> {
    public final Material material;
    public final Integer id;
    public ReplaceSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        material = json.has("material") ? Material.valueOf(json.get("material").getAsString()) : null;
        id = json.has("id") ? json.get("id").getAsInt() : null;
    }

    @Override public system.Toast2<ItemStack, Boolean> replace(ItemStack item) {
        item = item.clone();
        ItemMeta meta = item.getItemMeta();
        boolean save = false;
        if (id != null) {
            meta.setCustomModelData(id);
            save = true;
        }
        if (material != null) {
            item.setType(material);
            save = true;
        }
        if (save) {
            item.setItemMeta(meta);
            return system.toast(item, true);
        }
        return system.toast(item, false);
    }
}