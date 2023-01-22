package org.lime.gp.item.settings.list;

import org.bukkit.Material;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonArray;

@Setting(name = "duplicate") public class DuplicateSetting extends ItemSetting<JsonArray> {
    public DuplicateSetting(Items.ItemCreator creator, JsonArray json) {
        super(creator, json);
        json.forEach(item -> {
            int id = item.getAsInt();
            Items.creators.put(id, creator);
            Items.creatorNamesIDs.put(id, creator.getKey());
            try { Items.creatorMaterials.put(id, Material.valueOf(creator.item)); } catch (Exception ignored) { }
        });
    }
}