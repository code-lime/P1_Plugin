package org.lime.gp.item.loot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gson.JsonObject;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.item.data.Checker;
import org.lime.gp.module.loot.IPopulateLoot;

import com.google.gson.JsonArray;

public class MultiLoot implements ILoot {
    public List<ILoot> loot = new ArrayList<>();

    public MultiLoot(JsonArray json) {
        json.getAsJsonArray().forEach(kv -> loot.add(ILoot.parse(kv)));
    }
    public MultiLoot(Collection<ILoot> loot) {
        this.loot.addAll(loot);
    }

    @Override public List<ItemStack> generateLoot(IPopulateLoot loot) {
        List<ItemStack> items = new ArrayList<>();
        this.loot.forEach(kv -> items.addAll(kv.generateLoot(loot)));
        return items;
    }
}