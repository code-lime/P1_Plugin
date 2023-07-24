package org.lime.gp.item.loot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.lime.gp.module.loot.IPopulateLoot;
import org.lime.gp.module.loot.PopulateLootEvent;

import com.google.gson.JsonArray;

public class MultiLoot extends ILoot {
    public List<ILoot> loot = new ArrayList<>();

    public MultiLoot(JsonArray json) {
        json.getAsJsonArray().forEach(kv -> loot.add(ILoot.parse(kv)));
    }
    public MultiLoot(Collection<ILoot> loot) {
        this.loot.addAll(loot);
    }

    @Override public List<ItemStack> generate() {
        List<ItemStack> items = new ArrayList<>();
        this.loot.forEach(kv -> items.addAll(kv.generate()));
        return items;
    }
    @Override public List<ItemStack> generateFilter(IPopulateLoot loot) {
        List<ItemStack> items = new ArrayList<>();
        this.loot.forEach(kv -> items.addAll(kv.generateFilter(loot)));
        return items;
    }
}