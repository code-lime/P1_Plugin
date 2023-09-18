package org.lime.gp.craft.book;

import com.comphenix.protocol.events.PacketContainer;
import com.google.common.collect.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.RegistryMaterials;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.PacketPlayOutRecipeUpdate;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.RecipeBookServer;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingManager;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.World;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;
import org.jetbrains.annotations.NotNull;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.craft.recipe.*;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.item.Items;
import org.lime.gp.lime;
import org.lime.gp.player.perm.Perms;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Recipes<T extends AbstractRecipe> implements net.minecraft.world.item.crafting.Recipes<T> {
    public static CoreElement create() {
        return CoreElement.create(Recipes.class);
    }

    private static String createStaticPrefix() {
        long id = System.currentTimeMillis() - 1655000000000L;
        StringBuilder chs = new StringBuilder();
        while (id != 0) {
            int _id = (int)(id % 36);
            char ch = (char)(_id < 10 ? ('0' + _id) : ('a' + _id - 10));
            chs.append(ch);
            id /= 36;
        }
        return chs.toString();
    }
    private static final String STATIC_PREFIX = createStaticPrefix();
    public static Recipes<CauldronRecipe> CAULDRON = Recipes.register("cauldron");
    public static Recipes<ClickerRecipe> CLICKER = Recipes.register("clicker");
    public static Recipes<ItemFrameRecipe> ITEM_FRAME = Recipes.register("item_frame");
    public static Recipes<LaboratoryRecipe> LABORATORY = Recipes.register("laboratory");
    public static Recipes<ConverterRecipe> CONVERTER = Recipes.register("converter");
    public static Recipes<WaitingRecipe> WAITING = Recipes.register("waiting");

    public static final CraftingManager CRAFTING_MANAGER = MinecraftServer.getServer().getRecipeManager();
    public static final PlayerList PLAYER_LIST = MinecraftServer.getServer().getPlayerList();

    static {
        try {
            HashMap<net.minecraft.world.item.crafting.Recipes<?>, Object2ObjectLinkedOpenHashMap<MinecraftKey, IRecipe<?>>> recipesMap = new HashMap<>(CRAFTING_MANAGER.recipes);
            for (net.minecraft.world.item.crafting.Recipes<?> recipes : BuiltInRegistries.RECIPE_TYPE)
                recipesMap.computeIfAbsent(recipes, v -> new Object2ObjectLinkedOpenHashMap<>());
            CRAFTING_MANAGER.recipes = recipesMap.entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (Exception e) {
            lime.logStackTrace(e);
            throw null;
        }
    }

    private final String id;
    private Recipes(String id) { this.id = id; }
    public String id() { return id; }
    @Override public String toString() { return id; }

    private int loaded_index = -1;
    private ImmutableSet<String> cache = ImmutableSet.of();
    private final Map<UUID, ImmutableSet<String>> playerCache = new HashMap<>();
    public ImmutableSet<String> getCacheWhitelistKeys() {
        int index = Items.getLoadedIndex();
        if (loaded_index != index) {
            loaded_index = index;
            cache = CRAFTING_MANAGER.getAllRecipesFor(this).stream().flatMap(AbstractRecipe::getWhitelistKeys).collect(ImmutableSet.toImmutableSet());
            playerCache.clear();
        }
        return cache;
    }
    public ImmutableSet<String> getCacheWhitelistKeys(UUID player) {
        return playerCache.computeIfAbsent(player, uuid -> getAllRecipes(uuid).flatMap(AbstractRecipe::getWhitelistKeys).collect(ImmutableSet.toImmutableSet()));
    }

    public List<T> getAllRecipes() { return CRAFTING_MANAGER.getAllRecipesFor(this); }
    public Stream<T> getAllRecipes(Player player) { return getAllRecipes(Perms.getCanData(player)); }
    public Stream<T> getAllRecipes(UUID player) { return getAllRecipes(Perms.getCanData(player)); }
    public Stream<T> getAllRecipes(Perms.ICanData canData) { return getAllRecipes().stream().filter(v -> canData.isCanCraft(v.getId().getPath())); }

    public static <T extends AbstractRecipe> Recipes<T> register(final String id) {
        try 
        {
            RegistryMaterials<net.minecraft.world.item.crafting.Recipes<?>> reg = (RegistryMaterials<net.minecraft.world.item.crafting.Recipes<?>>)BuiltInRegistries.RECIPE_TYPE;
            boolean back = ReflectionAccess.frozen_RegistryMaterials.get(reg);
            ReflectionAccess.frozen_RegistryMaterials.set(reg, false);
            try { return IRegistry.register(reg, new MinecraftKey("lime." + STATIC_PREFIX, id), new Recipes<>(id)); }
            finally { if (back) reg.freeze(); /*ReflectionAccess.frozen_RegistryMaterials.set(reg, back);*/ }
        } catch (Throwable e) {
            lime.logStackTrace(e);
            throw null;
        }
    }
}








