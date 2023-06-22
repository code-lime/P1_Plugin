package org.lime.gp.item.loot;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;

public class EmptyLoot extends ILoot {
    public static final EmptyLoot Instance = new EmptyLoot();
    private EmptyLoot() {}
    @Override public List<ItemStack> generate() { return new ArrayList<>(); }
}