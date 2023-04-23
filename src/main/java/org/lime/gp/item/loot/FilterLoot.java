package org.lime.gp.item.loot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.lime.system;
import org.lime.gp.item.loot.filter.ILootFilter;
import org.lime.gp.module.PopulateLootEvent;

import com.google.gson.JsonObject;

public class FilterLoot extends ILoot {
    public List<system.Toast2<ILootFilter, ILoot>> loot = new ArrayList<>();

    public FilterLoot(JsonObject json) {
        json.entrySet().forEach(kv -> loot.add(system.toast(ILootFilter.parse(kv.getKey()), ILoot.parse(kv.getValue()))));
    }

    @Override public List<ItemStack> generate() {
        return Collections.emptyList();
    }
    @Override public List<ItemStack> generateFilter(PopulateLootEvent loot) {
        List<ItemStack> items = new ArrayList<>();
        for (system.Toast2<ILootFilter, ILoot> item : this.loot) {
            if (item.val0.isFilter(loot)) {
                return item.val1.generateFilter(loot);
            }
        }
        return items;
    }
}
