package org.lime.gp.item.loot;

import com.google.gson.JsonObject;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.filter.IFilter;
import org.lime.gp.filter.data.IFilterInfo;
import org.lime.gp.module.loot.IPopulateLoot;
import org.lime.gp.module.loot.Parameters;
import org.lime.system;

import java.util.ArrayList;
import java.util.List;

public class FilterLoot implements ILoot {
    public List<system.Toast2<IFilter<IPopulateLoot>, ILoot>> loot = new ArrayList<>();

    public FilterLoot(JsonObject json) {
        IFilterInfo<IPopulateLoot> filterInfo = Parameters.filterInfo();
        json.entrySet().forEach(kv -> loot.add(system.toast(IFilter.parse(filterInfo, kv.getKey()), ILoot.parse(kv.getValue()))));
    }

    @Override public List<ItemStack> generateLoot(IPopulateLoot loot) {
        List<ItemStack> items = new ArrayList<>();
        for (system.Toast2<IFilter<IPopulateLoot>, ILoot> item : this.loot) {
            if (item.val0.isFilter(loot)) {
                return item.val1.generateLoot(loot);
            }
        }
        return items;
    }
}
