package org.lime.gp.craft.recipe;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeCrafting;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.item.crafting.ShapedRecipes;
import net.minecraft.world.level.World;
import org.lime.gp.craft.slot.OutputSlot;
import org.lime.gp.craft.slot.RecipeSlot;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class LaboratoryRecipe extends AbstractRecipe {
    public final ImmutableList<RecipeSlot> input_thirst;
    public final ImmutableList<RecipeSlot> input_dust;
    public final OutputSlot output;
    public LaboratoryRecipe(MinecraftKey key, String group, List<RecipeSlot> input_thirst, List<RecipeSlot> input_dust, OutputSlot output) {
        super(key, group, Recipes.LABORATORY);
        this.input_thirst = ImmutableList.copyOf(input_thirst);
        this.input_dust = ImmutableList.copyOf(input_dust);
        this.output = output;
    }

    @Override public Stream<String> getWhitelistKeys() { return Stream.concat(input_thirst.stream(), input_dust.stream()).flatMap(RecipeSlot::getWhitelistKeys).distinct(); }

    private static boolean testSlots(List<RecipeSlot> _input_slots, List<ItemStack> _input_items) {
        ArrayList<RecipeSlot> input_slots = new ArrayList<>(_input_slots);
        ArrayList<ItemStack> input_items = new ArrayList<>(_input_items);

        input_slots.removeIf(v -> v.test(ItemStack.EMPTY));
        input_items.removeIf(ItemStack::isEmpty);

        int length = input_slots.size();
        if (length != input_items.size()) return false;

        for (int i = length - 1; i >= 0; i--) {
            ItemStack item = input_items.get(i);
            boolean remove = false;
            for (RecipeSlot slot : input_slots) {
                if (slot.test(item)) {
                    remove = true;
                    input_slots.remove(slot);
                    input_items.remove(i);
                    break;
                }
            }
            if (!remove) return false;
        }
        return input_items.isEmpty() && input_slots.isEmpty();
    }

    @Override public boolean matches(IInventory inventory, World world) {
        List<ItemStack> contents = inventory.getContents();
        return testSlots(this.input_thirst, contents.subList(0, 3)) && testSlots(this.input_dust, contents.subList(3, 6));
    }

    @Override public boolean canCraftInDimensions(int i, int j) { return true; }
    @Override public ItemStack getResultItem() { return output.nms(); }

    @Override protected Stream<RecipeCrafting> createDisplayRecipe(MinecraftKey displayKey, String displayGroup) {
        NonNullList<RecipeItemStack> slots = NonNullList.withSize(3*3, RecipeItemStack.EMPTY);

        {
            List<RecipeItemStack> input_thirst = this.input_thirst.stream().map(v -> v.getWhitelistIngredientsShow().map(IDisplayRecipe::genericItem)).map(RecipeItemStack::of).toList();
            int count_thirst = Math.min(input_thirst.size(), slots.size() - 1);
            for (int i = 0; i < Math.min(count_thirst, 3); i++) slots.set(i, input_thirst.get(i));
        }
        {
            List<RecipeItemStack> input_dust = this.input_dust.stream().map(v -> v.getWhitelistIngredientsShow().map(IDisplayRecipe::genericItem)).map(RecipeItemStack::of).toList();
            int count_dust = Math.min(input_dust.size(), slots.size() - 1);
            for (int i = 0; i < Math.min(count_dust, 3); i++) slots.set(i + 3, input_dust.get(i));
        }

        return Stream.of(new ShapedRecipes(displayKey, displayGroup, 3, 3, slots, output.nms()));
    }
}



















