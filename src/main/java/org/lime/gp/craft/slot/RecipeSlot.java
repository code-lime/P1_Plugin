package org.lime.gp.craft.slot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeItemStack;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.lime.gp.craft.recipe.IDisplayRecipe;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.item.Items;
import org.lime.gp.lime;
import org.lime.system;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class RecipeSlot {
    public static final RecipeSlot none = new RecipeSlot() {
        @Override public boolean test(net.minecraft.world.item.ItemStack item) { return item != null && item.isEmpty(); }
        @Override public net.minecraft.world.item.ItemStack result(int count) { return null; }
        @Override public Stream<String> getWhitelistKeys() { return Stream.empty(); }
    };

    protected RecipeSlot() { }

    public abstract boolean test(net.minecraft.world.item.ItemStack item);
    public abstract net.minecraft.world.item.ItemStack result(int count);
    public abstract Stream<String> getWhitelistKeys();
    public RecipeItemStack getRecipeSlotNMS() { return RecipeItemStack.of(this.getWhitelistIngredientsShow().map(IDisplayRecipe::genericItem)); }
    public Stream<ItemStack> getWhitelistIngredientsShow() {
        return getWhitelistKeys()
                .map(Items::getItemCreator)
                .flatMap(Optional::stream)
                .filter(system.distinctBy(Items.IItemCreator::getKey))
                .flatMap(c -> system.funcEx(() -> c.createItem(1)).optional().invoke().stream())
                .map(CraftItemStack::asNMSCopy);
    }

    public RecipeAmountSlot withAmount(int amount) { return new RecipeAmountSlot(this, amount); }
    public RecipeAnyAmountSlot withAmountAny(int amount) { return new RecipeAnyAmountSlot(this, amount); }

    public static RecipeSlot of(String log_key, JsonElement json) {
        RecipeSlot result = system.func(() -> {
            if (json.isJsonObject()) {
                JsonObject slot = json.getAsJsonObject();
                String in = slot.get("input").getAsString();
                String out = !slot.has("output") || slot.get("output").isJsonNull() ? null : slot.get("output").getAsString();
                RecipeSlot recipeSlot = out == null ? of(Items.createCheck(in)) : of(Items.createCheck(in), () -> CraftItemStack.asNMSCopy(Items.getItemCreator(out).map(Items.IItemCreator::createItem).orElseGet(Items::empty)));
                return slot.has("count") ? recipeSlot.withAmount(slot.get("count").getAsInt()) : recipeSlot;
            }
            String key = json.getAsString();
            List<String> arr = Arrays.stream(key.split("\\*")).collect(Collectors.toList());
            Integer count = arr.size() > 1 ? ExtMethods.parseUnsignedInt(arr.get(arr.size() - 1)).orElse(null) : null;
            if (count == null) return of(Items.createCheck(key));
            arr.remove(arr.size() - 1);
            return of(Items.createCheck(String.join("*", arr))).withAmount(count);
        }).invoke();
        if (result.getWhitelistKeys().findAny().isEmpty()) lime.logOP("RecipeSlot in '"+log_key+"' '" + json + "' is EMPTY! Maybe error...");
        return result;
    }
    public static Stream<RecipeSlot> ofArray(String log_key, JsonElement json) {
        JsonPrimitive primitive;
        if (json.isJsonPrimitive() && (primitive = json.getAsJsonPrimitive()).isString()) return Arrays.stream(primitive.getAsString().split("[\\t ]+")).map(JsonPrimitive::new).map(v -> RecipeSlot.of(log_key, v));
        return Stream.of(of(log_key, json));
    }
    public static RecipeSlot of(Items.Checker checker) {
        return new RecipeSlot() {
            @Override public boolean test(net.minecraft.world.item.ItemStack item) { return checker.check(item); }
            @Override public net.minecraft.world.item.ItemStack result(int count) { return null; }
            @Override public Stream<String> getWhitelistKeys() { return checker.getWhitelistKeys(); }
        };
    }
    public static RecipeSlot of(Items.Checker checker, system.Func0<net.minecraft.world.item.ItemStack> result) {
        return new RecipeSlot() {
            @Override public boolean test(net.minecraft.world.item.ItemStack item) { return checker.check(item); }
            @Override public net.minecraft.world.item.ItemStack result(int count) {
                net.minecraft.world.item.ItemStack item = result.invoke();
                item.setCount(count);
                return item;
            }
            @Override public Stream<String> getWhitelistKeys() { return checker.getWhitelistKeys(); }
        };
    }
}



















