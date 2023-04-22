package org.lime.gp.item.settings;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.system;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.data.ItemCreator;

public interface IItemSetting {
    ItemCreator creator();
    String name();
    system.Toast2<ItemStack, Boolean> replace(ItemStack item);
    void apply(ItemStack item, ItemMeta meta, Apply apply);
    void appendArgs(ItemStack item, Apply apply);
}