package org.lime.gp.craft.recipe;

import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.Recipes;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftCampfireRecipe;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftRecipe;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftNamespacedKey;
import org.bukkit.inventory.Recipe;
import java.util.List;
import java.util.stream.Stream;

public abstract class AbstractRecipe implements IRecipe<IInventory>, IDisplayRecipe {
    private final MinecraftKey key;
    private final Recipes<?> type;
    private final String group;
    public AbstractRecipe(MinecraftKey key, String group, Recipes<?> type) {
        this.group = group;
        this.key = key;
        this.type = type;
    }
    @Override public MinecraftKey getId() { return key; }
    @Override public Recipes<?> getType() { return type; }
    @Override public ItemStack getToastSymbol() { return new ItemStack(Blocks.BEDROCK); }
    @Override public Recipe toBukkitRecipe() { return new CraftCampfireRecipe(CraftNamespacedKey.fromMinecraft(key), new org.bukkit.inventory.ItemStack(Material.STONE, 1), CraftRecipe.toBukkit(RecipeItemStack.EMPTY), 0, 0); }
    @Override public RecipeSerializer<?> getSerializer() { return null; }
    @Override public ItemStack assemble(IInventory inventory) { return getResultItem(); }
    public abstract Stream<String> getWhitelistKeys();
    private List<ShapedRecipes> displayRecipe = null;
    @Override public Stream<ShapedRecipes> getDisplayRecipe() {
        if (displayRecipe == null) displayRecipe = createDisplayRecipe(new MinecraftKey(key.getNamespace() + ".generic", key.getPath()), group).toList();
        return displayRecipe.stream();
    }

    protected Stream<ShapedRecipes> createDisplayRecipe(MinecraftKey displayKey, String displayGroup) { return Stream.empty(); }
}












