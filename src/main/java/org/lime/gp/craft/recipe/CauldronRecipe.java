package org.lime.gp.craft.recipe;

import com.google.common.collect.ImmutableList;

import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeCrafting;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.item.crafting.ShapedRecipes;
import net.minecraft.world.level.World;
import org.lime.gp.craft.Crafts;
import org.lime.gp.craft.slot.output.IOutputSlot;
import org.lime.gp.craft.slot.RecipeSlot;
import org.lime.gp.craft.slot.output.IOutputVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CauldronRecipe extends AbstractRecipe {
    public final ImmutableList<RecipeSlot> input;
    public final IOutputSlot output;
    public CauldronRecipe(MinecraftKey key, String group, CraftingBookCategory category, List<RecipeSlot> input, IOutputSlot output) {
        super(key, group, category, Recipes.CAULDRON);
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
    @Override public ItemStack getResultItem(IRegistryCustom custom) { return output.create(false, IOutputVariable.empty()); }
    @Override public ItemStack assemble(IInventory inventory, IRegistryCustom custom, IOutputVariable variable) { return output.create(false, variable); }

    @Override public Stream<String> getWhitelistKeys() { return input.stream().flatMap(RecipeSlot::getWhitelistKeys).distinct(); }

    @Override protected Stream<RecipeCrafting> createDisplayRecipe(MinecraftKey displayKey, String displayGroup, CraftingBookCategory category) {
        NonNullList<RecipeItemStack> slots = NonNullList.withSize(3*3, RecipeItemStack.EMPTY);
        List<RecipeItemStack> items = input.stream().map(v -> v.getWhitelistIngredientsShow().map(IDisplayRecipe::amountToName)).map(RecipeItemStack::of).toList();
        int count = Math.min(items.size(), slots.size());
        for (int i = 0; i < count; i++) slots.set(i, items.get(i));
        return Stream.of(new ShapedRecipes(displayKey, displayGroup, category, 3, 3, slots, output.create(true, IOutputVariable.empty())));
    }
}



























