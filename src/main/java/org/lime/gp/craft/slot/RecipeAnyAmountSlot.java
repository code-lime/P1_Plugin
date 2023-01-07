package org.lime.gp.craft.slot;

import net.minecraft.world.item.ItemStack;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.lime.gp.item.Items;
import org.lime.system;

import java.util.Optional;
import java.util.stream.Stream;

public class RecipeAnyAmountSlot extends RecipeSlot {
    private final RecipeSlot base;
    public final int amount;

    public RecipeAnyAmountSlot(RecipeSlot base, int amount) {
        this.base = base;
        this.amount = amount;
    }

    @Override public Stream<ItemStack> getWhitelistIngredientsShow() {
        return getWhitelistKeys()
                .map(Items::getItemCreator)
                .flatMap(Optional::stream)
                .flatMap(c -> system.funcEx(() -> c.createItem(amount)).optional().invoke().stream())
                .map(CraftItemStack::asNMSCopy);
    }

    @Override public boolean test(net.minecraft.world.item.ItemStack item) { return base.test(item) && item.getCount() >= amount; }
    @Override public net.minecraft.world.item.ItemStack result(int count) { return base.result(count); }
    @Override public Stream<String> getWhitelistKeys() { return base.getWhitelistKeys(); }
}
