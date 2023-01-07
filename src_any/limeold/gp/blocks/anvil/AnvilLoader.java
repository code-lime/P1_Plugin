package org.lime.gp.block.component.data.anvil;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.lime.core;
import org.lime.gp.block.component.data.recipe.IRecipe;
import org.lime.gp.block.component.data.recipe.RepairRecipe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AnvilLoader implements Listener {
    public static core.element create() {
        /*Blocks.addDefaultBlocks(new BlockInfo("anvil")
                .add(v -> InfoComponent.GenericDynamicComponent.of("anvil", v, AnvilInstance::new))
                .add(v -> new Components.DisplayComponent(v, new JsonObject()))
                .addReplace(Material.ANVIL,
                        block -> block.getBlockData() instanceof Directional directional ? directional.getFacing() : BlockFace.NORTH,
                        (face, info) -> info.list(AnvilInstance.class)
                                .forEach(v -> v.type(AnvilInstance.AnvilType.anvil).direction(AnvilInstance.Direction.of(face)))
                )
                .addReplace(Material.CHIPPED_ANVIL,
                        block -> block.getBlockData() instanceof Directional directional ? directional.getFacing() : BlockFace.NORTH,
                        (face, info) -> info.list(AnvilInstance.class)
                                .forEach(v -> v.type(AnvilInstance.AnvilType.chipped_anvil).direction(AnvilInstance.Direction.of(face)))
                )
                .addReplace(Material.DAMAGED_ANVIL,
                        block -> block.getBlockData() instanceof Directional directional ? directional.getFacing() : BlockFace.NORTH,
                        (face, info) -> info.list(AnvilInstance.class)
                                .forEach(v -> v.type(AnvilInstance.AnvilType.damaged_anvil).direction(AnvilInstance.Direction.of(face)))
                )
        );*/
        return core.element.create(AnvilLoader.class)
                .withInstance()
                .<JsonPrimitive>addConfig("config", v -> v.withParent("anvil_order").withDefault(new JsonPrimitive(true)).withInvoke(j -> ANVIL_ORDER = j.getAsBoolean()))
                .<JsonObject>addConfig("anvil", v -> v.withInvoke(AnvilLoader::config).withDefault(new JsonObject()));
    }

    public static final ConcurrentHashMap<String, IRecipe> recipeList = new ConcurrentHashMap<>();
    public static final ConcurrentLinkedQueue<Material> whitelistMaterial = new ConcurrentLinkedQueue<>();
    public static int MAX_CLICKS = 1;
    public static int MAX_STACK = 1;
    public static boolean ANVIL_ORDER = true;

    private static HashMap<String, IRecipe> createDefault() {
        HashMap<String, IRecipe> recipes = new HashMap<>();
        recipes.put("default.repair", RepairRecipe.Instance);
        return recipes;
    }
    public static List<String> crafts() {
        return new ArrayList<>(recipeList.keySet());
    }
    public static boolean isWhitelist(Material material) {
        return whitelistMaterial.contains(material);
    }
    public static void config(JsonObject json) {
        HashMap<String, IRecipe> recipeList = new HashMap<>();
        json.getAsJsonObject().entrySet().forEach(dat -> recipeList.put(dat.getKey(), IRecipe.parse(dat.getKey(), dat.getValue().getAsJsonObject())));

        AnvilLoader.recipeList.clear();
        AnvilLoader.whitelistMaterial.clear();

        AnvilLoader.recipeList.putAll(recipeList);
        AnvilLoader.recipeList.putAll(createDefault());

        AnvilLoader.recipeList.values().forEach(IRecipe::addToWhitelist);
        MAX_CLICKS = 1;
        MAX_STACK = 1;
        AnvilLoader.recipeList.forEach((k,v) -> {
            MAX_CLICKS = Math.max(v.getClicks(), MAX_CLICKS);
            MAX_STACK = Math.max(v.getItemCount(), MAX_STACK);
        });

        Set<Material> materials = new HashSet<>(whitelistMaterial);
        AnvilLoader.whitelistMaterial.clear();
        AnvilLoader.whitelistMaterial.addAll(materials);
    }
}


















