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
import org.lime.gp.craft.book.Recipes;
import org.lime.gp.craft.slot.output.IOutputSlot;
import org.lime.gp.craft.slot.RecipeSlot;
import org.lime.gp.craft.slot.output.IOutputVariable;
import org.lime.gp.craft.slot.output.RangeOutputSlot;
import org.lime.gp.item.data.Checker;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class ConverterRecipe extends AbstractRecipe {
    public final ImmutableList<RecipeSlot> input;
    public final IConverterOutput output;
    //public final ImmutableMap<IOutputSlot, Optional<String>> output;

    public interface IConverterOutput {
        Stream<IOutputSlot> slots();
        default Stream<Toast2<IOutputSlot, Optional<String>>> slotsWithGroups() {
            return slots().map(v -> Toast.of(v, Optional.empty()));
        }

        static IConverterOutput ofString(String str) {
            Checker checker = Checker.createCheck(str);
            return () -> checker.getWhitelistKeys().map(v -> new RangeOutputSlot(v, 1));
        }
        static IConverterOutput ofMap(Map<IOutputSlot, Optional<String>> output) {
            return new IConverterOutput() {
                @Override public Stream<IOutputSlot> slots() { return output.keySet().stream(); }
                @Override public Stream<Toast2<IOutputSlot, Optional<String>>> slotsWithGroups() {
                    return output.entrySet().stream().map(v -> Toast.of(v.getKey(), v.getValue()));
                }
            };
        }
    }

    public final String converter_type;
    public final boolean replace;

    public ConverterRecipe(MinecraftKey key, String group, CraftingBookCategory category, List<RecipeSlot> input, IConverterOutput output, String converter_type, boolean replace) {
        super(key, group, category, Recipes.CONVERTER);
        this.input = ImmutableList.copyOf(input);
        this.output = output;
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
    @Override public ItemStack assemble(IInventory inventory, IRegistryCustom custom, IOutputVariable variable) { return RandomUtils.rand(output.slots().toList()).create(false, variable); }
    @Override public ItemStack getResultItem(IRegistryCustom custom) { return RandomUtils.rand(output.slots().toList()).create(false, IOutputVariable.empty()); }

    @Override protected Stream<RecipeCrafting> createDisplayRecipe(MinecraftKey displayKey, String displayGroup, CraftingBookCategory category) {
        List<RecipeCrafting> recipes = new ArrayList<>();
        Toast1<Integer> index = Toast.of(0);
        output.slotsWithGroups().forEach(kv -> {
            IOutputSlot result = kv.val0;
            index.val0++;
            NonNullList<RecipeItemStack> slots = NonNullList.withSize(3*3, RecipeItemStack.EMPTY);
            slots.set(4, RecipeItemStack.of(input.stream().flatMap(v -> v.getWhitelistIngredientsShow().map(IDisplayRecipe::amountToName))));
            recipes.add(new ShapedRecipes(new MinecraftKey(displayKey.getNamespace() + "." + index.val0, displayKey.getPath()), kv.val1.orElse(displayGroup), category, 3, 3, slots, result.create(true, IOutputVariable.empty())));
        });
        return recipes.stream();
    }
}



















