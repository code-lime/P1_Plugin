package org.lime.gp.block.component.data.recipe;

import com.google.gson.JsonObject;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class IRecipe {
    public abstract boolean check(List<ItemStack> items);
    public abstract ItemStack craft(List<ItemStack> items);
    public abstract ItemStack checkCraft(List<ItemStack> items);
    public abstract void addToWhitelist();
    public abstract int getClicks();
    public abstract int getItemCount();

    public static IRecipe parse(String key, JsonObject json) {
        if (json.has("output")) return new Recipe(json);
        if (json.has("repair")) return new RecipeRepair(json);
        throw new IllegalArgumentException("Type anvil craft '" + key + "' error");
    }
}
