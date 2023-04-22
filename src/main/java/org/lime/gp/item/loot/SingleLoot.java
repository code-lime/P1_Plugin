package org.lime.gp.item.loot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.lime.system;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.data.IItemCreator;

public class SingleLoot extends ILoot {
    public Checker item;
    public system.IRange amount;

    public SingleLoot(Checker item, system.IRange amount) {
        this.item = item;
        this.amount = amount;
        if (item.getWhitelistCreators().anyMatch(v -> true)) throw new IllegalArgumentException("[LOOT] ITEM '"+item+"' NOT FOUNDED!");
    }

    @Override
    public List<ItemStack> generate() {
        int amount = (int)this.amount.getValue(64);
        if (amount < 1) return Collections.emptyList();
        List<IItemCreator> creators = item.getWhitelistCreators().toList();
        if (creators.isEmpty()) return Collections.emptyList();
        if (creators.size() == 1) return Collections.singletonList(creators.get(0).createItem(amount));
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < amount; i++) items.add(system.rand(creators).createItem());
        return items;
    }
}