package org.lime.gp.craft.book;

import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.item.crafting.RecipeCrafting;

import javax.annotation.Nullable;

public interface IHandleAutoRecipe {
    void handle(IRecipe<?> baseRecipe, @Nullable RecipeCrafting displayRecipe);
}
