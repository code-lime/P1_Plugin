package org.lime.gp.player.module.cinv;

import io.netty.buffer.Unpooled;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.NonNullList;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.recipebook.AutoRecipeAbstract;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.lime.gp.craft.recipe.IDisplayRecipe;
import org.lime.gp.extension.ItemNMS;
import org.lime.gp.item.data.Checker;
import org.lime.gp.player.inventory.MainPlayerInventory;
import org.lime.system.utils.ItemUtils;

import java.util.*;
import java.util.stream.Stream;

public interface IQueryRecipe extends IDisplayRecipe {
    IRecipe<?> base();
    Map<Integer, Collection<ItemStack>> slots();
    boolean check(Checker item);

    static Stream<IQueryRecipe> ofRecipe(IRegistryCustom custom, IRecipe<?> recipe) {
        if (recipe instanceof IDisplayRecipe displayRecipe) return displayRecipe.getDisplayRecipe(custom).map(v -> of(custom, recipe, v));
        else if (recipe instanceof RecipeCrafting crafting && !crafting.getIngredients().isEmpty()) return Stream.of(of(custom, recipe, crafting));
        else if (recipe instanceof RecipeCooking cooking) return Stream.of(of(custom, recipe, cooking));
        else return Stream.empty();
    }

    private static <TRecipe extends IRecipe<C>, C extends net.minecraft.world.IInventory>TRecipe cloneRecipe(TRecipe recipe) {
        RecipeSerializer<TRecipe> serializer = (RecipeSerializer<TRecipe>)recipe.getSerializer();

        PacketDataSerializer buffer = new PacketDataSerializer(Unpooled.buffer());
        serializer.toNetwork(buffer, recipe);
        return serializer.fromNetwork(recipe.getId(), buffer);
    }

    private static IQueryRecipe of(IRegistryCustom custom, IRecipe<?> base, RecipeCrafting displayRecipe) {
        Map<Integer, Collection<ItemStack>> slots = new HashMap<>();
        NonNullList<RecipeItemStack> items = displayRecipe.getIngredients();

        ((AutoRecipeAbstract<RecipeItemStack>) (inputs, slot, amount, gridX, gridY) -> slots.put(slot, Arrays.asList(inputs.next().getItems())))
                .placeRecipe(3,3, 0, displayRecipe, items.iterator(), 1);

        slots.put(0, Collections.singletonList(displayRecipe.getResultItem(custom)));

        RecipeCrafting resultDisplayRecipe = cloneRecipe(displayRecipe);

        ItemStack item = resultDisplayRecipe.getResultItem(custom);

        //ItemNMS.addLore()

        return new IQueryRecipe() {
            @Override public Stream<RecipeCrafting> getDisplayRecipe(IRegistryCustom custom) { return Stream.of(resultDisplayRecipe); }
            @Override public MinecraftKey getRecipeKey() { return base.getId(); }
            @Override public Map<Integer, Collection<ItemStack>> slots() { return slots; }
            @Override public IRecipe<?> base() { return base; }
            @Override public boolean check(Checker checker) {
                for (Collection<ItemStack> items : slots.values())
                    for (ItemStack item : items)
                        if (checker.check(item))
                            return true;
                return false;
            }
        };
    }
    private static IQueryRecipe of(IRegistryCustom custom, IRecipe<?> base, RecipeCooking cooking) {
        RecipeItemStack input = cooking.getIngredients().get(0);
        ItemStack output = cooking.getResultItem(custom);

        HashMap<Integer, Collection<ItemStack>> slots = new HashMap<>();
        Collection<ItemStack> inputItems = Arrays.asList(input.getItems());
        slots.put(2, inputItems);
        slots.put(0, Collections.singletonList(output));

        NonNullList<RecipeItemStack> displaySlots = NonNullList.withSize(3*3, RecipeItemStack.EMPTY);
        RecipeItemStack _empty = RecipeItemStack.of(Stream.of(CraftItemStack.asNMSCopy(MainPlayerInventory.createBarrier(false))));
        for (int i = 0; i < 9; i++) displaySlots.set(i, _empty);
        displaySlots.set(1, input);
        displaySlots.set(7, RecipeItemStack.of(Stream.of(CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.LAVA_BUCKET)))));

        RecipeCrafting displayRecipe = new ShapedRecipes(cooking.getId(), cooking.getGroup(), CraftingBookCategory.MISC, 3, 3, displaySlots, output);

        RecipeCrafting resultDisplayRecipe = cloneRecipe(displayRecipe);

        return new IQueryRecipe() {
            @Override public Stream<RecipeCrafting> getDisplayRecipe(IRegistryCustom custom) { return Stream.of(resultDisplayRecipe); }
            @Override public MinecraftKey getRecipeKey() { return base.getId(); }
            @Override public Map<Integer, Collection<ItemStack>> slots() { return slots; }
            @Override public IRecipe<?> base() { return base; }
            @Override public boolean check(Checker checker) {
                for (Collection<ItemStack> items : slots.values())
                    for (ItemStack item : items)
                        if (checker.check(item))
                            return true;
                return false;
            }
        };
    }
}
