package org.lime.gp.item.data;

import java.util.stream.Stream;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.Items;

public class MaterialCreator extends IItemCreator {
    public final Material material;
    @Override public boolean updateReplace() { return false; }
    @Override public String getKey() { return Items.getMaterialKey(material); }
    @Override public int getID() { return 0; }
    @Override public Stream<Material> getWhitelist() { return Stream.of(material); }

    public MaterialCreator(Material material) { this.material = material; }

    @Override public ItemStack createItem(int count, Apply apply) { return new ItemStack(this.material, count); }
}