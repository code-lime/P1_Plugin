package org.lime.gp.block.component.data.cauldron;

import com.google.gson.JsonObject;
import org.lime.core;
import org.lime.gp.display.Displays;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CauldronLoader {
    public static core.element create() {
        /*Blocks.addDefaultBlocks(new BlockInfo("cauldron")
                .add(v -> InfoComponent.GenericDynamicComponent.of("cauldron", v, CauldronInstance::new))
                .add(v -> new Components.LootComponent(v, List.of(Material.CAULDRON)))
                .addReplace(Material.CAULDRON, (info) -> info.list(CauldronInstance.class).forEach(cauldron -> cauldron.data(cauldron.new EmptyCauldron())))
                .addReplace(Material.WATER_CAULDRON,
                        block -> block.getBlockData() instanceof Levelled levelled ? levelled.getLevel() : -1,
                        (level, info) -> info.list(CauldronInstance.class).forEach(cauldron -> cauldron.data(cauldron.new WaterCauldron()).level(level))
                )
                .addReplace(Material.POWDER_SNOW_CAULDRON,
                        block -> block.getBlockData() instanceof Levelled levelled ? levelled.getLevel() : -1,
                        (level, info) -> info.list(CauldronInstance.class).forEach(cauldron -> cauldron.data(cauldron.new SnowCauldron()).level(level))
                )
                .addReplace(Material.LAVA_CAULDRON,
                        (info) -> info.list(CauldronInstance.class).forEach(cauldron -> cauldron.data(cauldron.new LavaCauldron()))
                )
        );*/
        return core.element.create(CauldronLoader.class)
                .withInit(CauldronLoader::init)
                .<JsonObject>addConfig("cauldron", v -> v.withInvoke(CauldronLoader::config).withDefault(new JsonObject()));
    }
    public static void init() {
        CauldronBlockInteraction.bootStrap();
        Displays.initDisplay(CauldronDisplay.manager());
    }
    public static void config(JsonObject json) {
        HashMap<String, Recipe> recipes = new HashMap<>();
        json.get("recipes").getAsJsonObject().entrySet().forEach(kv -> recipes.put(kv.getKey(), Recipe.parse(kv.getKey(), kv.getValue().getAsJsonObject())));
        waterRecipe = null;
        recipes.values().removeIf(v -> {
            int size = v.items.size();
            if (size == 0) {
                waterRecipe = v;
                return true;
            }
            return false;
        });
        CauldronLoader.recipes.clear();
        CauldronLoader.recipes.putAll(recipes);
        iitems.clear();
        recipes.values().forEach(v -> iitems.addAll(v.items));
    }

    public static final HashMap<String, Recipe> recipes = new HashMap<>();
    public static final List<Step> iitems = new ArrayList<>();
    public static Recipe waterRecipe = null;
    public static Recipe findRecipe(List<LoadedStep> steps) { return recipes.values().stream().filter(r -> r.check(steps)).findFirst().orElse(null); }
    public static Recipe getRecipe(String recipe) { return recipes.getOrDefault(recipe, null); }


}











