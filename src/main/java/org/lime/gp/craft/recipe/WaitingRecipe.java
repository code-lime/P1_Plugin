package org.lime.gp.craft.recipe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.lime.gp.craft.slot.output.IOutputVariable;
import org.lime.system;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WaitingRecipe extends AbstractRecipe {
    public final RecipeSlot input;
    public final ImmutableList<RecipeSlot> fuel;
    public final ImmutableList<RecipeSlot> catalyse;
    public final IOutputSlot output;
    public final int total_sec;
    public final String waiting_type;

    public WaitingRecipe(MinecraftKey key, String group, CraftingBookCategory category, RecipeSlot input, List<RecipeSlot> fuel, List<RecipeSlot> catalyse, IOutputSlot output, int total_sec, String waiting_type) {
        super(key, group, category, Recipes.WAITING);
        this.input = input;
        this.fuel = ImmutableList.copyOf(fuel);
        this.catalyse = ImmutableList.copyOf(catalyse);
        this.output = output;
        this.total_sec = total_sec;
        this.waiting_type = waiting_type;
    }

    @Override public boolean matches(IInventory inventory, World world) {
        List<ItemStack> contents = inventory.getContents();
        return input.split(contents.get(0)).map(count -> {
            List<ItemStack> items = contents.stream().skip(1).sorted(Comparator.comparingInt(ItemStack::getCount)).collect(Collectors.toList());
            for (RecipeSlot slot : catalyse) {
                boolean isFound = false;
                for (int i = 0; i < items.size(); i++) {
                    if (slot.test(items.get(i))) {
                        isFound = true;
                        items.remove(i);
                        break;
                    }
                }
                if (!isFound) return false;
            }
            for (RecipeSlot slot : fuel) {
                boolean isFound = false;
                for (int i = 0; i < items.size(); i++) {
                    if (slot.split(items.get(i)).map(count::equals).orElse(false)) {
                        isFound = true;
                        items.remove(i);
                        break;
                    }
                }
                if (!isFound) return false;
            }
            return true;
        }).orElse(false);
    }
    @Override public ItemStack assemble(IInventory inventory, IRegistryCustom custom, IOutputVariable variable) {
        ItemStack output = this.output.create(false, variable);
        input.split(inventory.getItem(0)).ifPresent(count -> output.setCount(output.getCount() * count));
        return output;
    }
    @Override public Stream<String> getWhitelistKeys() {
        return Streams.concat(
                input.getWhitelistKeys(),
                fuel.stream().flatMap(RecipeSlot::getWhitelistKeys),
                catalyse.stream().flatMap(RecipeSlot::getWhitelistKeys)
        ).distinct();
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(IRegistryCustom custom) { return ItemStack.EMPTY; }

    @Override protected Stream<RecipeCrafting> createDisplayRecipe(MinecraftKey displayKey, String displayGroup, CraftingBookCategory category) {
        NonNullList<RecipeItemStack> slots = NonNullList.withSize(3*3, RecipeItemStack.EMPTY);
        slots.set(1, input.getRecipeSlotNMS(IDisplayRecipe::amountToName));

        int fuel_count = Math.min(fuel.size(), 3);
        int catalyser_count = Math.min(catalyse.size(), 3);

        for (int i = 0; i < catalyser_count; i++) slots.set(i + 3, catalyse.get(i).getRecipeSlotNMS(IDisplayRecipe::amountToName));
        for (int i = 0; i < fuel_count; i++) slots.set(i + 6, fuel.get(i).getRecipeSlotNMS(IDisplayRecipe::amountToName));

        return Stream.of(new ShapedRecipes(displayKey, displayGroup, category, 3, 3, slots, IDisplayRecipe.nameWithPostfix(output.create(true, IOutputVariable.empty()), Component.text(" ⌚ " + system.formatTotalTime(total_sec, system.FormatTime.DAY_TIME)).color(NamedTextColor.LIGHT_PURPLE))));
    }

    @Override public String toString() {
        return "[waiting:"+waiting_type+"] "+getId()+" - "+input.getWhitelistKeys().collect(Collectors.joining(","));
    }
}















