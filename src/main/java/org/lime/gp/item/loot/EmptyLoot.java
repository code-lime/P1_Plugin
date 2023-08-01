package org.lime.gp.item.loot;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.lime.gp.module.loot.IPopulateLoot;

public class EmptyLoot implements ILoot {
    public static final EmptyLoot Instance = new EmptyLoot();
    private EmptyLoot() {}
    @Override public List<ItemStack> generateLoot(IPopulateLoot loot) { return new ArrayList<>(); }
}