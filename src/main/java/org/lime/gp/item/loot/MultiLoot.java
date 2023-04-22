package org.lime.gp.item.loot;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;

public class MultiLoot extends ILoot {
    public List<ILoot> loot = new ArrayList<>();
    @Override
    public List<ItemStack> generate() {
        List<ItemStack> items = new ArrayList<>();
        loot.forEach(kv -> items.addAll(kv.generate()));
        return items;
    }
}