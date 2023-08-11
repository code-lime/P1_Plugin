package org.lime.gp.craft.recipe;

import net.minecraft.core.IRegistryCustom;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.World;
import org.bukkit.inventory.Recipe;

@SuppressWarnings("unused")
public class IgnoreRecipe implements IRecipe<IInventory> {
    @Override public boolean matches(IInventory iInventory, World world) { return false; }
    @Override public ItemStack assemble(IInventory iInventory, IRegistryCustom custom) { return null; }
    @Override public boolean canCraftInDimensions(int i, int j) { return false; }
    @Override public ItemStack getResultItem(IRegistryCustom custom) { return null; }
    @Override public MinecraftKey getId() { return new MinecraftKey("none", "none"); }
    @Override public RecipeSerializer<?> getSerializer() { return null; }
    @Override public net.minecraft.world.item.crafting.Recipes<?> getType() { return null; }
    @Override public Recipe toBukkitRecipe() { return null; }
}
