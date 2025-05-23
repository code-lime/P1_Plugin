package org.lime.gp.craft.slot;

import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.lime.gp.item.Items;
import org.lime.system.execute.Execute;

import javax.annotation.Nullable;
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
                .flatMap(c -> Execute.funcEx(() -> c.createItem(amount)).optional().invoke().stream())
                .map(CraftItemStack::asNMSCopy);
    }

    @Override public boolean test(net.minecraft.world.item.ItemStack item) { return base.test(item) && item.getCount() >= amount; }
    @Override public Optional<Integer> split(ItemStack item) { return base.split(item).map(count -> count / amount); }
    @Override public @Nullable ItemStack result(ItemStack slot) { return base.result(slot); }
    @Override public Stream<String> getWhitelistKeys() { return base.getWhitelistKeys(); }
    @Override public boolean checkCrafting() { return base.checkCrafting(); }
}
