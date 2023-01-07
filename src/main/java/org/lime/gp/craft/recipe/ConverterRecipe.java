package org.lime.gp.craft.recipe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.item.crafting.ShapedRecipes;
import net.minecraft.world.level.World;
import org.lime.gp.craft.slot.OutputSlot;
import org.lime.gp.craft.slot.RecipeSlot;
import org.lime.gp.item.Items;
import org.lime.gp.lime;
import org.lime.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ConverterRecipe extends AbstractRecipe {
    public final ImmutableList<RecipeSlot> input;
    public final ImmutableMap<OutputSlot, Optional<String>> output;
    public final String converter_type;
    public final boolean replace;

    public ConverterRecipe(MinecraftKey key, String group, List<RecipeSlot> input, Map<OutputSlot, Optional<String>> output, String converter_type, boolean replace) {
        super(key, group, Recipes.CONVERTER);
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
    @Override public ItemStack getResultItem() { return system.rand(output.entrySet()).getKey().nms(); }

    @Override protected Stream<ShapedRecipes> createDisplayRecipe(MinecraftKey displayKey, String displayGroup) {
        List<ShapedRecipes> recipes = new ArrayList<>();
        int index = 0;
        for (Map.Entry<OutputSlot, Optional<String>> kv : output.entrySet()) {
            OutputSlot result = kv.getKey();
            index++;
            NonNullList<RecipeItemStack> slots = NonNullList.withSize(3*3, RecipeItemStack.EMPTY);
            slots.set(4, RecipeItemStack.of(input.stream().flatMap(v -> v.getWhitelistIngredientsShow().map(IDisplayRecipe::amountToName))));
            recipes.add(new ShapedRecipes(new MinecraftKey(displayKey.getNamespace() + "." + index, displayKey.getPath()), kv.getValue().orElse(displayGroup), 3, 3, slots, result.nms()));
        }
        return recipes.stream();
    }
}



















