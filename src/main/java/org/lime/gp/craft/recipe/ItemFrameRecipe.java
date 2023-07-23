package org.lime.gp.craft.recipe;

import net.minecraft.core.IRegistryCustom;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.level.World;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.entity.ItemFrame;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.lime.core;
import org.lime.gp.extension.inventory.ReadonlyInventory;
import org.lime.gp.craft.slot.output.IOutputSlot;
import org.lime.gp.craft.slot.RecipeSlot;
import org.lime.gp.item.Items;
import org.lime.gp.lime;

import java.util.*;
import java.util.stream.Stream;

public class ItemFrameRecipe extends AbstractRecipe {
    public static core.element create() {
        return core.element.create(ItemFrameRecipe.class)
                .withInit(ItemFrameRecipe::init);
    }
    public static void init() {
        lime.repeat(ItemFrameRecipe::sec, 1);
    }
    private static final NamespacedKey ITEM_FRAME_RECIPE_SEC = new NamespacedKey(lime._plugin, "item_frame_recipe_sec");
    public static void sec() {
        Set<String> cache = Recipes.ITEM_FRAME.getCacheWhitelistKeys();
        Bukkit.getWorlds().forEach(world -> world.getEntitiesByClass(ItemFrame.class).forEach(itemFrame -> {
            PersistentDataContainer container = itemFrame.getPersistentDataContainer();
            org.bukkit.inventory.ItemStack item = itemFrame.getItem();
            Items.getGlobalKeyByItem(item).filter(cache::contains).ifPresentOrElse(key -> {
                ReadonlyInventory inventory = ReadonlyInventory.ofBukkit(Collections.singletonList(item), itemFrame.getLocation());
                World _world = ((CraftWorld)world).getHandle();
                List<ItemFrameRecipe> recipes = Recipes.ITEM_FRAME.getAllRecipes();
                for (ItemFrameRecipe recipe : recipes) {
                    if (recipe.matches(inventory, _world)) {
                        int sec = container.getOrDefault(ITEM_FRAME_RECIPE_SEC, PersistentDataType.INTEGER, 0) + 1;
                        if (recipe.seconds <= sec) {
                            itemFrame.setItem(recipe.assemble(inventory, _world.registryAccess()).asBukkitCopy());
                            container.remove(ITEM_FRAME_RECIPE_SEC);
                        } else {
                            container.set(ITEM_FRAME_RECIPE_SEC, PersistentDataType.INTEGER, sec);
                        }
                        return;
                    }
                }
            }, () -> container.remove(ITEM_FRAME_RECIPE_SEC));
        }));
    }

    public final RecipeSlot input;
    public final IOutputSlot output;
    public final int seconds;
    public ItemFrameRecipe(MinecraftKey key, RecipeSlot input, IOutputSlot output, int seconds) {
        super(key, "", CraftingBookCategory.MISC, Recipes.ITEM_FRAME);
        this.input = input;
        this.output = output;
        this.seconds = seconds;
    }

    @Override public boolean matches(IInventory inventory, World world) {
        List<ItemStack> items = inventory.getContents().stream().filter(v -> !v.isEmpty()).toList();
        int length = items.size();
        if (length != 1) return false;
        return input.test(items.get(0));
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(IRegistryCustom custom) { return output.nms(false); }
    @Override public Stream<String> getWhitelistKeys() { return input.getWhitelistKeys(); }
}
