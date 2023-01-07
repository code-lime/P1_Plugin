package org.lime.gp.craft.recipe;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.item.crafting.ShapedRecipes;
import net.minecraft.world.level.World;
import org.lime.gp.craft.Crafts;
import org.lime.gp.craft.slot.OutputSlot;
import org.lime.gp.craft.slot.RecipeSlot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CauldronRecipe extends AbstractRecipe {
    public final ImmutableList<RecipeSlot> input;
    public final OutputSlot output;
    public CauldronRecipe(MinecraftKey key, String group, List<RecipeSlot> input, OutputSlot output) {
        super(key, group, Recipes.CAULDRON);
        this.input = ImmutableList.copyOf(input);
        this.output = output;
    }

    @Override public boolean matches(IInventory inventory, World world) {
        List<ItemStack> items = inventory.getContents().stream().filter(v -> !v.isEmpty()).toList();
        int length = items.size();
        if (input.size() != length) return false;
        for (int i = 0; i < items.size(); i++)
            if (!input.get(i).test(items.get(i)))
                return false;
        return true;
    }
    @Override public NonNullList<ItemStack> getRemainingItems(IInventory inventory) {
        return Crafts.getRemainingItems(input.stream().collect(HashMap::new, (map, v) -> map.put(map.size(), v), Map::putAll), inventory);
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem() { return output.nms(); }
    @Override public Stream<String> getWhitelistKeys() { return input.stream().flatMap(RecipeSlot::getWhitelistKeys).distinct(); }

    @Override protected Stream<ShapedRecipes> createDisplayRecipe(MinecraftKey displayKey, String displayGroup) {
        NonNullList<RecipeItemStack> slots = NonNullList.withSize(3*3, RecipeItemStack.EMPTY);
        List<RecipeItemStack> items = input.stream().map(v -> v.getWhitelistIngredientsShow().map(IDisplayRecipe::amountToName)).map(RecipeItemStack::of).toList();
        int count = Math.min(items.size(), slots.size());
        for (int i = 0; i < count; i++) slots.set(i, items.get(i));
        return Stream.of(new ShapedRecipes(displayKey, displayGroup, 3, 3, slots, output.nms()));
    }
}



























