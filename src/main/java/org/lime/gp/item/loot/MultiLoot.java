package org.lime.gp.item.loot;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.lime.gp.module.PopulateLootEvent;

import com.google.gson.JsonArray;

public class MultiLoot extends ILoot {
    public List<ILoot> loot = new ArrayList<>();

    public MultiLoot(JsonArray json) {
        json.getAsJsonArray().forEach(kv -> loot.add(ILoot.parse(kv)));
    }

    @Override public List<ItemStack> generate() {
        List<ItemStack> items = new ArrayList<>();
        this.loot.forEach(kv -> items.addAll(kv.generate()));
        return items;
    }
    @Override public List<ItemStack> generateFilter(PopulateLootEvent loot) {
        List<ItemStack> items = new ArrayList<>();
        this.loot.forEach(kv -> items.addAll(kv.generateFilter(loot)));
        return items;
    }
}