package org.lime.gp.craft.recipe;

import net.minecraft.core.IRegistryCustom;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.Recipes;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftCampfireRecipe;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftRecipe;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftNamespacedKey;
import org.bukkit.inventory.Recipe;
import org.lime.gp.craft.slot.output.IOutputVariable;

import java.util.List;
import java.util.stream.Stream;

public abstract class AbstractRecipe implements IRecipe<IInventory>, IDisplayRecipe {
    private final MinecraftKey key;
    private final Recipes<?> type;
    private final String group;
    private final CraftingBookCategory category;

    public AbstractRecipe(MinecraftKey key, String group, CraftingBookCategory category, Recipes<?> type) {
        this.group = group;
        this.key = key;
        this.category = category;
        this.type = type;
    }
    @Override public MinecraftKey getId() { return key; }
    @Override public Recipes<?> getType() { return type; }
    @Override public ItemStack getToastSymbol() { return new ItemStack(Blocks.BEDROCK); }
    @Override public Recipe toBukkitRecipe() { return new CraftCampfireRecipe(CraftNamespacedKey.fromMinecraft(key), new org.bukkit.inventory.ItemStack(Material.STONE, 1), CraftRecipe.toBukkit(RecipeItemStack.EMPTY), 0, 0); }
    @Override public RecipeSerializer<?> getSerializer() { return null; }

    public abstract ItemStack assemble(IInventory inventory, IRegistryCustom custom, IOutputVariable variable);
    @Override public final ItemStack assemble(IInventory inventory, IRegistryCustom custom) { return getResultItem(custom); }
    public abstract Stream<String> getWhitelistKeys();
    private List<RecipeCrafting> displayRecipe = null;
    @Override public Stream<RecipeCrafting> getDisplayRecipe(IRegistryCustom custom) {
        if (displayRecipe == null) displayRecipe = createDisplayRecipe(new MinecraftKey(key.getNamespace() + ".g", key.getPath()), group, category).map(v -> IDisplayRecipe.removeLore(v, custom)).toList();
        return displayRecipe.stream();
    }

    protected Stream<RecipeCrafting> createDisplayRecipe(MinecraftKey displayKey, String displayGroup, CraftingBookCategory category) { return Stream.empty(); }
}












