package org.lime.gp.craft.recipe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

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
import org.lime.gp.craft.slot.output.IOutputSlot;
import org.lime.gp.craft.slot.RecipeSlot;
import org.lime.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class ConverterRecipe extends AbstractRecipe {
    public final ImmutableList<RecipeSlot> input;
    public final ImmutableMap<IOutputSlot, Optional<String>> output;
    public final String converter_type;
    public final boolean replace;

    public ConverterRecipe(MinecraftKey key, String group, CraftingBookCategory category, List<RecipeSlot> input, Map<IOutputSlot, Optional<String>> output, String converter_type, boolean replace) {
        super(key, group, category, Recipes.CONVERTER);
        this.input = ImmutableList.copyOf(input);
        this.output = ImmutableMap.copyOf(output);
        this.converter_type = converter_type;
        this.replace = replace;
    }

    @Override public Stream<String> getWhitelistKeys() { return input.stream().flatMap(RecipeSlot::getWhitelistKeys).distinct(); }

    @Override public boolean matches(IInventory inventory, World world) {
        ItemStack item = inventory.getContents().get(0);
        for (RecipeSlot slot : input)
            if (slot.test(item))
                return true;
        return false;
    }

    @Override public boolean canCraftInDimensions(int i, int j) { return i == 1 && j == 1; }
    @Override public ItemStack getResultItem(IRegistryCustom custom) { return system.rand(output.entrySet()).getKey().nms(false); }

    @Override protected Stream<RecipeCrafting> createDisplayRecipe(MinecraftKey displayKey, String displayGroup, CraftingBookCategory category) {
        List<RecipeCrafting> recipes = new ArrayList<>();
        int index = 0;
        for (Map.Entry<IOutputSlot, Optional<String>> kv : output.entrySet()) {
            IOutputSlot result = kv.getKey();
            index++;
            NonNullList<RecipeItemStack> slots = NonNullList.withSize(3*3, RecipeItemStack.EMPTY);
            slots.set(4, RecipeItemStack.of(input.stream().flatMap(v -> v.getWhitelistIngredientsShow().map(IDisplayRecipe::amountToName))));
            recipes.add(new ShapedRecipes(new MinecraftKey(displayKey.getNamespace() + "." + index, displayKey.getPath()), kv.getValue().orElse(displayGroup), category, 3, 3, slots, result.nms(true)));
        }
        return recipes.stream();
    }
}



















