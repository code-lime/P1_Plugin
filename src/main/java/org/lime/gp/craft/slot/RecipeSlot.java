package org.lime.gp.craft.slot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeItemStack;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.lime.gp.craft.recipe.IDisplayRecipe;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.data.IItemCreator;
import org.lime.gp.lime;
import org.lime.system.execute.Execute;
import org.lime.system.execute.Func0;
import org.lime.system.execute.Func1;
import org.lime.system.range.IRange;
import org.lime.system.utils.IterableUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class RecipeSlot {
    public static final RecipeSlot none = new RecipeSlot() {
        @Override public boolean test(ItemStack item) { return item != null && item.isEmpty(); }
        @Override public Optional<Integer> split(ItemStack item) { return Optional.empty(); }
        @Override public @Nullable ItemStack result(ItemStack slot) { return null; }
        @Override public Stream<String> getWhitelistKeys() { return Stream.empty(); }
        @Override public boolean checkCrafting() { return true; }
    };

    protected RecipeSlot() { }

    public abstract boolean test(ItemStack item);
    public abstract Optional<Integer> split(ItemStack item);
    public abstract @Nullable ItemStack result(ItemStack slot);
    public abstract Stream<String> getWhitelistKeys();
    public abstract boolean checkCrafting();

    public RecipeItemStack getRecipeSlotNMS() { return RecipeItemStack.of(this.getWhitelistIngredientsShow().map(IDisplayRecipe::genericItem)); }
    public RecipeItemStack getRecipeSlotNMS(Func1<ItemStack, ItemStack> map) { return RecipeItemStack.of(this.getWhitelistIngredientsShow().map(map)); }
    public Stream<ItemStack> getWhitelistIngredientsShow() {
        return getWhitelistKeys()
                .map(Items::getItemCreator)
                .flatMap(Optional::stream)
                .filter(IterableUtils.distinctBy(IItemCreator::getKey))
                .flatMap(c -> Execute.funcEx(() -> c.createItem(1)).optional().invoke().stream())
                .map(CraftItemStack::asNMSCopy);
    }

    public RecipeAmountSlot withAmount(int amount) { return new RecipeAmountSlot(this, amount); }
    public RecipeAnyAmountSlot withAmountAny(int amount) { return new RecipeAnyAmountSlot(this, amount); }

    public static RecipeSlot of(String log_key, JsonElement json) {
        List<String> logs = new ArrayList<>();
        logs.add("ORIGINAL: " + json);
        RecipeSlot result = Execute.func(() -> {
            if (json.isJsonObject()) {
                JsonObject slot = json.getAsJsonObject();
                String in = slot.get("input").getAsString();
                String out = !slot.has("output") || slot.get("output").isJsonNull() ? null : slot.get("output").getAsString();
                IRange durability = !slot.has("durability") || slot.get("durability").isJsonNull() ? null : IRange.parse(slot.get("durability").getAsString());
                if (out != null && durability != null)
                    lime.logOP("RecipeSlot in '"+log_key+"' '" + json + "' is DUPLICATE VARIABLE ('output' and 'durability')! Maybe error...");

                RecipeSlot recipeSlot;
                if (out != null) recipeSlot = of(Checker.createCheck(in), () -> CraftItemStack.asNMSCopy(Items.getItemCreator(out).map(IItemCreator::createItem).orElseGet(Items::empty)));
                else if (durability != null) recipeSlot = of(Checker.createCheck(in), durability);
                else recipeSlot = of(Checker.createCheck(in));

                return slot.has("count") ? recipeSlot.withAmount(slot.get("count").getAsInt()) : recipeSlot;
            }
            String key = json.getAsString();
            logs.add("STRING: " + key);
            List<String> arr = Arrays.stream(key.split("\\*")).collect(Collectors.toList());
            Integer count = arr.size() > 1 ? ExtMethods.parseUnsignedInt(arr.get(arr.size() - 1)).orElse(null) : null;
            logs.add("COUNT: " + count);
            if (count == null) return of(Checker.createCheck(key));
            arr.remove(arr.size() - 1);
            key = String.join("*", arr);
            logs.add("MODIFY REGEX: " + key);
            return of(Checker.createCheck(key)).withAmount(count);
        }).invoke();
        if (result.getWhitelistKeys().findAny().isEmpty())
            lime.logOP("RecipeSlot in '"+log_key+"' '" + json + "' is EMPTY! Maybe error...\n" + String.join("\n", logs));
        return result;
    }
    public static Stream<RecipeSlot> ofArray(String log_key, JsonElement json) {
        JsonPrimitive primitive;
        if (json.isJsonPrimitive() && (primitive = json.getAsJsonPrimitive()).isString()) return Arrays.stream(primitive.getAsString().split("[\\t ]+")).map(JsonPrimitive::new).map(v -> RecipeSlot.of(log_key, v));
        return Stream.of(of(log_key, json));
    }
    public static RecipeSlot of(Checker checker) {
        return new RecipeSlot() {
            @Override public boolean test(ItemStack item) { return checker.check(item); }
            @Override public Optional<Integer> split(ItemStack item) {
                if (!test(item)) return Optional.empty();
                int count = item.getCount();
                return count > 0 ? Optional.of(count) : Optional.empty();
            }
            @Override public @Nullable ItemStack result(ItemStack slot) { return null; }
            @Override public Stream<String> getWhitelistKeys() { return checker.getWhitelistKeys(); }
            @Override public boolean checkCrafting() { return true; }
        };
    }
    public static RecipeSlot of(Checker checker, Func0<ItemStack> result) {
        return new RecipeSlot() {
            @Override public boolean test(ItemStack item) { return checker.check(item); }
            @Override public Optional<Integer> split(ItemStack item) {
                if (!test(item)) return Optional.empty();
                int count = item.getCount();
                return count > 0 ? Optional.of(count) : Optional.empty();
            }
            @Override public ItemStack result(ItemStack slot) {
                ItemStack item = result.invoke();
                if (item.isEmpty()) return item;
                item.setCount(slot.getCount());
                return item;
            }
            @Override public Stream<String> getWhitelistKeys() { return checker.getWhitelistKeys(); }
            @Override public boolean checkCrafting() {
                return checker.getWhitelistCreators()
                        .allMatch(v -> v.tryGetMaxStackSize()
                                .map(_v -> _v == 1)
                                .orElse(false)
                        );
            }
        };
    }
    public static RecipeSlot of(Checker checker, IRange result) {
        return new RecipeSlot() {
            @Override public boolean test(ItemStack item) { return checker.check(item); }
            @Override public Optional<Integer> split(ItemStack item) {
                if (!test(item)) return Optional.empty();
                int count = item.getCount();
                return count > 0 ? Optional.of(count) : Optional.empty();
            }
            @Override public ItemStack result(ItemStack slot) {
                ItemStack item = slot.copy();
                return Items.hurtRemove(item, result.getIntValue(item.getMaxDamage())) ? ItemStack.EMPTY : item;
            }
            @Override public Stream<String> getWhitelistKeys() { return checker.getWhitelistKeys(); }
            @Override public boolean checkCrafting() {
                return checker.getWhitelistCreators()
                        .allMatch(v -> v.tryGetMaxStackSize()
                                .map(_v -> _v == 1)
                                .orElse(false)
                        );
            }
        };
    }
}



















