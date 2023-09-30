package org.lime.gp.craft.recipe;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.network.chat.ChatModifier;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeCrafting;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.item.crafting.ShapedRecipes;
import net.minecraft.world.item.crafting.ShapelessRecipes;

import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.settings.list.LoreCraftSetting;

import java.util.Collections;
import java.util.stream.Stream;

public interface IDisplayRecipe {
    Stream<RecipeCrafting> getDisplayRecipe(IRegistryCustom custom);
    MinecraftKey getRecipeKey();

    static ItemStack amountToName(ItemStack item) {
        return nameWithPostfix(item, Component.text(" x"+item.getCount()));
    }
    static ItemStack amountToName(ItemStack item, Component postfix) {
        return nameWithPostfix(item, Component.text(" x"+item.getCount()).append(postfix));
    }
    static ItemStack nameWithPostfix(ItemStack item, Component postfix) {
        IChatBaseComponent name = item.getHoverName();
        if (!item.hasCustomHoverName()) name = IChatBaseComponent.empty().append(item.getHoverName()).setStyle(ChatModifier.EMPTY.withItalic(false));
        return genericItem(item).setHoverName(IChatBaseComponent.empty()
                .append(name)
                .append(ChatHelper.toNMS(postfix.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET ? postfix.decoration(TextDecoration.ITALIC, false) : postfix))
        );
    }
    static ItemStack genericItem(ItemStack item) {
        item = item.copy();
        item.addTagElement("generic", NBTTagByte.valueOf(true));
        return item;
    }

    static RecipeCrafting removeLore(RecipeCrafting recipe, IRegistryCustom custom) {
        if (recipe instanceof ShapedRecipes shaped) return removeLore(shaped, custom);
        if (recipe instanceof ShapelessRecipes shapelless) return removeLore(shapelless, custom);
        return recipe;
    }

    static ItemStack removeLore(ItemStack item) {
        item = item.copy();
        ItemMeta meta = CraftItemStack.getItemMeta(item);
        if (meta == null || !meta.hasLore() || Items.has(LoreCraftSetting.class, item)) return item;
        meta.lore(Collections.emptyList());
        CraftItemStack.setItemMeta(item, meta);
        return item;
    }
    static RecipeItemStack removeLore(RecipeItemStack item) {
        ItemStack[] oldItems = item.getItems();
        int lenght = oldItems.length;
        RecipeItemStack out = new RecipeItemStack(Stream.empty());
        ItemStack[] items = new ItemStack[lenght];
        out.itemStacks = items;
        for (int i = 0; i < lenght; i++) items[i] = removeLore(oldItems[i]);
        return out;
    }
    static NonNullList<RecipeItemStack> removeLore(NonNullList<RecipeItemStack> items) {
        int lenght = items.size();
        NonNullList<RecipeItemStack> newItems = NonNullList.create();
        for (int i = 0; i < lenght; i++) newItems.add(removeLore(items.get(i)));
        return newItems;
    }

    static ShapedRecipes removeLore(ShapedRecipes recipe, IRegistryCustom custom) {
        return new ShapedRecipes(recipe.getId(), recipe.getGroup(), recipe.category(), recipe.getWidth(), recipe.getHeight(), removeLore(recipe.getIngredients()), recipe.getResultItem(custom));
    }
    static ShapelessRecipes removeLore(ShapelessRecipes recipe, IRegistryCustom custom) {
        return new ShapelessRecipes(recipe.getId(), recipe.getGroup(), recipe.category(), recipe.getResultItem(custom), removeLore(recipe.getIngredients()));
    }

    static boolean hasItem(RecipeCrafting recipe, IRegistryCustom custom, Checker ingredient) {
        return hasItem(recipe.getIngredients(), ingredient) || ingredient.check(recipe.getResultItem(custom));
    }
    static boolean hasItem(RecipeItemStack slot, Checker ingredient) {
        for (ItemStack item : slot.getItems())
            if (ingredient.check(item))
                return true;
        return false;
    }
    static boolean hasItem(NonNullList<RecipeItemStack> items, Checker ingredient) {
        for (RecipeItemStack item : items)
            if (hasItem(item, ingredient))
                return true;
        return false;
    }
}
