package org.lime.gp.craft;

import co.aikar.util.Counter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.INamable;
import net.minecraft.world.IInventory;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.World;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftNamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.permissions.ServerOperator;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.craft.book.RecipesBook;
import org.lime.gp.craft.recipe.*;
import org.lime.gp.craft.book.Recipes;
import org.lime.gp.craft.slot.output.IOutputSlot;
import org.lime.gp.craft.slot.RecipeSlot;
import org.lime.gp.craft.slot.output.IOutputVariable;
import org.lime.gp.item.data.Checker;
import org.lime.gp.lime;
import org.lime.gp.player.perm.Perms;
import org.lime.system;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Crafts {
    public static CoreElement create() {
        return CoreElement.create(Crafts.class)
                .withUninit(Crafts::uninit)
                .addCommand("removed.crafts", v -> v
                        .withUsage("/removed.crafts {empty_or_regex}")
                        .withCheck(ServerOperator::isOp)
                        .withTab((sender, args) -> removeCraftList.keySet())
                        .withExecutor((sender, args) -> switch (args.length) {
                            case 0 -> {
                                Component message = Component.text("Список: ");
                                HashMap<String, String> single = new HashMap<>();
                                List<String> empty = new ArrayList<>();
                                for (Map.Entry<String, List<String>> kv : removeCraftList.entrySet()) {
                                    String key = kv.getKey();
                                    List<String> list = kv.getValue();
                                    String showText;
                                    switch (list.size()) {
                                        case 0: empty.add(key); continue;
                                        case 1: single.put(key, list.get(0)); continue;
                                        default: showText = String.join("\n", list); break;
                                    }
                                    message = message.append(Component.text("\n").append(
                                            Component.text(" - " + key + " x" + list.size())
                                                    .clickEvent(ClickEvent.copyToClipboard(showText))
                                                    .hoverEvent(HoverEvent.showText(Component.text(showText)))
                                    ));
                                }
                                if (single.size() > 0) {
                                    List<String> clipboard = new ArrayList<>();
                                    Component showText = null;
                                    for (Map.Entry<String, String> kv : single.entrySet()) {
                                        if (showText == null) showText = Component.empty();
                                        else showText = showText.append(Component.newline());

                                        String key = kv.getKey();
                                        String value = kv.getValue();
                                        if (key.equals(value)) showText = showText.append(Component.text(value).color(NamedTextColor.GOLD));
                                        else showText = showText.append(Component.text(key + " - " + value));
                                        clipboard.add(key + " - " + value);
                                    }

                                    message = message.append(Component.text("\n").append(
                                            Component.text(" - Одиночные regex x" + single.size())
                                                    .color(NamedTextColor.AQUA)
                                                    .clickEvent(ClickEvent.copyToClipboard(String.join("\n", clipboard)))
                                                    .hoverEvent(HoverEvent.showText(showText))));
                                }
                                if (empty.size() > 0) {
                                    String showText = String.join("\n", empty);
                                    message = message.append(Component.text("\n").append(
                                            Component.text(" - Пустые regex x" + empty.size())
                                                    .color(NamedTextColor.AQUA)
                                                    .clickEvent(ClickEvent.copyToClipboard(showText))
                                                    .hoverEvent(HoverEvent.showText(Component.text(showText)))));
                                }
                                sender.sendMessage(message);
                                yield true;
                            }
                            default -> false;
                        })
                )
                .<JsonObject>addConfig("crafts", v -> v.withInvoke(Crafts::config).withDefault(new JsonObject()));
    }

    public static void uninit() { removeCrafts(new ArrayList<>()); }
    private static int index = 0;

    private final static List<IRecipe<?>> hardcodeCrafts = new ArrayList<>();
    public static void addDefaultCraft(IRecipe<?> recipe) {
        hardcodeCrafts.add(recipe);
    }
    public static void addDefaultCraft(String key, JsonObject recipe) {
        hardcodeCrafts.add(create(key, recipe));
    }

    public static void config(JsonObject json) {
        List<String> regexList = new ArrayList<>();
        if (json.has("remove")) {
            json.get("remove").getAsJsonArray().forEach(item -> regexList.add(item.getAsString()));
            json.remove("remove");
        }

        List<IRecipe<?>> craftList = new ArrayList<>(hardcodeCrafts);
        lime.combineParent(json, true, false).entrySet().forEach(kv -> craftList.add(create(kv.getKey(), kv.getValue().getAsJsonObject())));
        //craftList.add(ClickerRecipe.ANVIL_DEFAULT);

        index++;
        removeCrafts(regexList);
        regex_to_crafts.clear();

        craftList.forEach(Recipes.CRAFTING_MANAGER::addRecipe);
        lime.once(RecipesBook::reload, 1);
    }

    public static final HashMap<String, List<String>> removeCraftList = new HashMap<>();
    public static void removeCrafts(List<String> regexList) {
        Iterator<IRecipe<?>> recipes = new RecipeIteratorNMS();
        HashMap<String, List<String>> removeCraftList = new HashMap<>();
        regexList.forEach(regex -> removeCraftList.put(regex, new ArrayList<>()));
        while (recipes.hasNext()) {
            IRecipe<?> recipe = recipes.next();
            MinecraftKey key = recipe.getId();
            //if (keyed.key().toString().contains("armor_dye")) lime.logOP("ARMOR_DYE_COLOR");
            String namespace = key.getNamespace();
            switch (namespace) {
                case "minecraft": {
                    String value = key.getPath();
                    boolean remove = false;
                    for (String regex : regexList) {
                        if (system.compareRegex(value, regex)) {
                            //if (keyed.key().toString().contains("armor_dye")) lime.logOP("ARMOR_DYE_COLOR REMOVE IN REGEX '"+regex+"'");
                            removeCraftList.get(regex).add(value);
                            remove = true;
                        }
                    }
                    if (remove) break;
                    continue;
                }
                case "lime": break;
                default: continue;
            }
            //if (keyed.key().toString().contains("armor_dye")) lime.logOP("ARMOR_DYE_COLOR_REMOVE!");
            recipes.remove();
        }
        Crafts.removeCraftList.clear();
        removeCraftList.forEach((key, list) -> Crafts.removeCraftList.put(key, list.stream().sorted().toList()));
    }

    private static final ConcurrentHashMap<String, Perms.ICanData> regex_to_crafts = new ConcurrentHashMap<>();
    public static Perms.ICanData canDataByRegex(String craftRegex) {
        return regex_to_crafts.computeIfAbsent(craftRegex, regex -> {
            String unique = "REGEX." + regex + "." + index;
            ImmutableSet<String> crafts = Streams.stream(Bukkit.getServer().recipeIterator())
                    .map(v -> ((Keyed) v).getKey().getKey())
                    .filter(v -> system.compareRegex(v, regex))
                    .distinct()
                    .collect(ImmutableSet.toImmutableSet());
            return new Perms.ICanData() {
                @Override public String unique() { return unique; }
                @Override public Optional<Integer> role() { return Optional.empty(); }
                @Override public Optional<Integer> work() { return Optional.empty(); }
                @Override public boolean isCanBreak(String material) { return false; }
                @Override public boolean isCanPlace(String material) { return false; }
                @Override public boolean isCanPlace(String from_material, String to_material) { return false; }
                @Override public boolean isCanCraft(String craft) { return crafts.contains(craft); }
                @Override public Stream<String> getCanCrafts() { return crafts.stream(); }
                @Override public boolean isCanUse(String item) { return false; }
                @Override public Stream<String> getCanUse() { return Stream.empty(); }
                @Override public boolean isCanDamage(EntityType entity) { return false; }
                @Override public boolean isCanFarm(EntityType entity) { return false; }
                @Override public boolean isCanFishing() { return false; }
                @Override public double getBreakFarmReplace(Material material) { return 0; }
            };
        });
    }

    private static List<RecipeSlot> create(String log_key, JsonArray json) {
        return Streams.stream(json.iterator()).flatMap(v -> RecipeSlot.ofArray(log_key, v)).toList();
    }

    private enum CookingType {
        Blasting((key, group, category, output, input, exp, time) -> new RecipeBlasting(key, group, category, input.getRecipeSlotNMS(), output.create(true, IOutputVariable.empty()), exp, time) {
            @Override public net.minecraft.world.item.ItemStack getResultItem(IRegistryCustom custom) { return output.create(false, IOutputVariable.empty()); }
            @Override public net.minecraft.world.item.ItemStack assemble(IInventory inventory, IRegistryCustom custom) { return getResultItem(custom); }
            @Override public boolean matches(IInventory inventory, net.minecraft.world.level.World world) { return check(input, inventory); }
        }),
        Furnace((key, group, category, output, input, exp, time) -> new FurnaceRecipe(key, group, category, input.getRecipeSlotNMS(), output.create(true, IOutputVariable.empty()), exp, time) {
            @Override public net.minecraft.world.item.ItemStack getResultItem(IRegistryCustom custom) { return output.create(false, IOutputVariable.empty()); }
            @Override public net.minecraft.world.item.ItemStack assemble(IInventory inventory, IRegistryCustom custom) { return getResultItem(custom); }
            @Override public boolean matches(IInventory inventory, net.minecraft.world.level.World world) { return check(input, inventory); }
        }),
        Campfire((key, group, category, output, input, exp, time) -> new RecipeCampfire(key, group, category, input.getRecipeSlotNMS(), output.create(true, IOutputVariable.empty()), exp, time) {
            @Override public net.minecraft.world.item.ItemStack getResultItem(IRegistryCustom custom) { return output.create(false, IOutputVariable.empty()); }
            @Override public net.minecraft.world.item.ItemStack assemble(IInventory inventory, IRegistryCustom custom) { return getResultItem(custom); }
            @Override public boolean matches(IInventory inventory, net.minecraft.world.level.World world) { return check(input, inventory); }
        }),
        Smoking((key, group, category, output, input, exp, time) -> new RecipeSmoking(key, group, category, input.getRecipeSlotNMS(), output.create(true, IOutputVariable.empty()), exp, time) {
            @Override public net.minecraft.world.item.ItemStack getResultItem(IRegistryCustom custom) { return output.create(false, IOutputVariable.empty()); }
            @Override public net.minecraft.world.item.ItemStack assemble(IInventory inventory, IRegistryCustom custom) { return getResultItem(custom); }
            @Override public boolean matches(IInventory inventory, net.minecraft.world.level.World world) { return check(input, inventory); }
        });

        private static boolean check(RecipeSlot input, IInventory inventory) {
            return input.test(inventory.getItem(0));
        }

        private final system.Func7<MinecraftKey, String, CookingBookCategory, IOutputSlot, RecipeSlot, Float, Integer, RecipeCooking> init;
        CookingType(system.Func7<MinecraftKey, String, CookingBookCategory, IOutputSlot, RecipeSlot, Float, Integer, RecipeCooking> init) {
            this.init = init;
        }

        public RecipeCooking create(MinecraftKey key, String group, CookingBookCategory category, JsonObject json) {
            IOutputSlot output = IOutputSlot.of(json.get("output"));
            RecipeSlot input = RecipeSlot.of(key.toString(), json.get("input"));
            float experience = json.get("experience").getAsFloat();
            int cookTime = json.get("cookTime").getAsInt();

            return init.invoke(key, group, category, output, input, experience, cookTime);
        }
    }

    private static abstract class ShapelessRecipes extends net.minecraft.world.item.crafting.ShapelessRecipes implements VanillaType, IDisplayRecipe {
        public final List<RecipeSlot> recipes;
        public ShapelessRecipes(MinecraftKey id, String group, CraftingBookCategory category, List<RecipeSlot> recipes, net.minecraft.world.item.ItemStack output, NonNullList<RecipeItemStack> input) {
            super(id, group, category, output, input);
            this.recipes = checkRecipeCrafting(id.getPath(), recipes);
        }
        public abstract net.minecraft.world.item.ItemStack result(IOutputVariable variable);
        @Override public net.minecraft.world.item.ItemStack getResultItem(IRegistryCustom custom) {
            return result(IOutputVariable.empty());
        }
        @Override public net.minecraft.world.item.ItemStack assemble(InventoryCrafting inventory, IRegistryCustom custom) {
            return inventory.getOwner() instanceof Player player
                    ? result(IOutputVariable.of(player))
                    : result(IOutputVariable.empty());
        }
        @Override public boolean canCraftInDimensions(int width, int height) {
            return width * height >= recipes.size();
        }

        public Optional<HashMap<Integer, RecipeSlot>> craft(InventoryCrafting inventory) {
            ArrayList<system.Toast2<net.minecraft.world.item.ItemStack, Integer>> providedItems = new ArrayList<>();
            Counter<net.minecraft.world.item.ItemStack> matchedProvided = new Counter<>();
            Counter<RecipeSlot> matchedIngredients = new Counter<>();
            for (int j2 = 0; j2 < inventory.getContainerSize(); ++j2) {
                net.minecraft.world.item.ItemStack itemstack = inventory.getItem(j2);
                if (itemstack.isEmpty()) continue;
                itemstack = itemstack.copy(true);
                providedItems.add(system.toast(itemstack, j2));
                for (RecipeSlot ingredient : recipes) {
                    if (!ingredient.test(itemstack)) continue;
                    matchedProvided.increment(itemstack);
                    matchedIngredients.increment(ingredient);
                }
            }
            if (matchedProvided.isEmpty() || matchedIngredients.isEmpty()) return Optional.empty();
            ArrayList<RecipeSlot> ingredients = new ArrayList<>(recipes);
            providedItems.sort(Comparator.<system.Toast2<net.minecraft.world.item.ItemStack, Integer>>comparingInt(c2 -> (int)matchedProvided.getCount(c2.val0)).reversed());
            ingredients.sort(Comparator.comparingInt(c2 -> (int)matchedIngredients.getCount(c2)));
            HashMap<Integer, RecipeSlot> output = new HashMap<>();
            block2: for (system.Toast2<net.minecraft.world.item.ItemStack, Integer> provided : providedItems) {
                Iterator<RecipeSlot> itIngredient = ingredients.iterator();
                while (itIngredient.hasNext()) {
                    RecipeSlot ingredient = itIngredient.next();
                    if (!ingredient.test(provided.val0)) continue;
                    output.put(provided.val1, ingredient);
                    itIngredient.remove();
                    continue block2;
                }
                return Optional.empty();
            }
            return ingredients.isEmpty() ? Optional.of(output) : Optional.empty();
        }

        @Override public boolean matches(InventoryCrafting inventory, net.minecraft.world.level.World world) {
            return craft(inventory).isPresent();
        }

        @Override public NonNullList<net.minecraft.world.item.ItemStack> getRemainingItems(InventoryCrafting inventory) {
            return Crafts.getRemainingItems(craft(inventory).orElse(null), inventory);
        }
        private Optional<net.minecraft.world.item.crafting.RecipeCrafting> displayRecipe = null;
        @Override public Stream<net.minecraft.world.item.crafting.RecipeCrafting> getDisplayRecipe(IRegistryCustom custom) {
            return (displayRecipe == null ? (displayRecipe = createDisplayRecipe(new MinecraftKey(getId().getNamespace() + ".g", getId().getPath()), this.getGroup()).map(v -> IDisplayRecipe.removeLore(v, custom))) : displayRecipe).stream();
        }
        protected abstract Optional<net.minecraft.world.item.crafting.RecipeCrafting> createDisplayRecipe(MinecraftKey displayKey, String displayGroup);
    }
    private static abstract class ModifyRecipes extends net.minecraft.world.item.crafting.ShapelessRecipes implements VanillaType, IDisplayRecipe {
        public final RecipeSlot modifySlot;
        public final List<RecipeSlot> otherSlots;
        public ModifyRecipes(MinecraftKey id, String group, CraftingBookCategory category, RecipeSlot modifySlot, List<RecipeSlot> otherSlots, net.minecraft.world.item.ItemStack output, NonNullList<RecipeItemStack> input) {
            super(id, group, category, output, input);
            this.modifySlot = checkRecipeCrafting(id.getPath(), modifySlot);
            this.otherSlots = checkRecipeCrafting(id.getPath(), otherSlots);
        }
        public abstract net.minecraft.world.item.ItemStack result(ItemStack replace, IOutputVariable variable);
        @Override public net.minecraft.world.item.ItemStack getResultItem(IRegistryCustom custom) {
            return result(ItemStack.EMPTY, IOutputVariable.empty());
        }
        @Override public net.minecraft.world.item.ItemStack assemble(InventoryCrafting inventory, IRegistryCustom custom) {
            return inventory.getOwner() instanceof Player player
                    ? result(findModifyItem(inventory), IOutputVariable.of(player))
                    : result(findModifyItem(inventory), IOutputVariable.empty());
        }
        @Override public boolean canCraftInDimensions(int width, int height) {
            return width * height >= otherSlots.size() + 1;
        }

        private ItemStack findModifyItem(InventoryCrafting inventory) {
            for (int j2 = 0; j2 < inventory.getContainerSize(); ++j2) {
                net.minecraft.world.item.ItemStack itemstack = inventory.getItem(j2);
                if (itemstack.isEmpty()) continue;
                itemstack = itemstack.copy(true);
                if (modifySlot.test(itemstack))
                    return itemstack;
            }
            return ItemStack.EMPTY;
        }

        public Optional<HashMap<Integer, RecipeSlot>> craft(InventoryCrafting inventory) {
            Iterable<RecipeSlot> recipeSlots = Iterables.concat(Collections.singleton(modifySlot), otherSlots);

            ArrayList<system.Toast2<net.minecraft.world.item.ItemStack, Integer>> providedItems = new ArrayList<>();
            Counter<net.minecraft.world.item.ItemStack> matchedProvided = new Counter<>();
            Counter<RecipeSlot> matchedIngredients = new Counter<>();
            for (int j2 = 0; j2 < inventory.getContainerSize(); ++j2) {
                net.minecraft.world.item.ItemStack itemstack = inventory.getItem(j2);
                if (itemstack.isEmpty()) continue;
                itemstack = itemstack.copy(true);
                providedItems.add(system.toast(itemstack, j2));
                for (RecipeSlot ingredient : recipeSlots) {
                    if (!ingredient.test(itemstack)) continue;
                    matchedProvided.increment(itemstack);
                    matchedIngredients.increment(ingredient);
                }
            }
            if (matchedProvided.isEmpty() || matchedIngredients.isEmpty()) return Optional.empty();
            ArrayList<RecipeSlot> ingredients = new ArrayList<>();
            recipeSlots.forEach(ingredients::add);
            providedItems.sort(Comparator.<system.Toast2<net.minecraft.world.item.ItemStack, Integer>>comparingInt(c2 -> (int)matchedProvided.getCount(c2.val0)).reversed());
            ingredients.sort(Comparator.comparingInt(c2 -> (int)matchedIngredients.getCount(c2)));
            HashMap<Integer, RecipeSlot> output = new HashMap<>();
            block2: for (system.Toast2<net.minecraft.world.item.ItemStack, Integer> provided : providedItems) {
                Iterator<RecipeSlot> itIngredient = ingredients.iterator();
                while (itIngredient.hasNext()) {
                    RecipeSlot ingredient = itIngredient.next();
                    if (!ingredient.test(provided.val0)) continue;
                    output.put(provided.val1, ingredient);
                    itIngredient.remove();
                    continue block2;
                }
                return Optional.empty();
            }
            return ingredients.isEmpty() ? Optional.of(output) : Optional.empty();
        }

        @Override public boolean matches(InventoryCrafting inventory, net.minecraft.world.level.World world) {
            return craft(inventory).isPresent();
        }

        @Override public NonNullList<net.minecraft.world.item.ItemStack> getRemainingItems(InventoryCrafting inventory) {
            return Crafts.getRemainingItems(craft(inventory).orElse(null), inventory);
        }
        private Optional<net.minecraft.world.item.crafting.RecipeCrafting> displayRecipe = null;
        @Override public Stream<net.minecraft.world.item.crafting.RecipeCrafting> getDisplayRecipe(IRegistryCustom custom) {
            return (displayRecipe == null ? (displayRecipe = createDisplayRecipe(new MinecraftKey(getId().getNamespace() + ".g", getId().getPath()), this.getGroup()).map(v -> IDisplayRecipe.removeLore(v, custom))) : displayRecipe).stream();
        }
        protected abstract Optional<net.minecraft.world.item.crafting.RecipeCrafting> createDisplayRecipe(MinecraftKey displayKey, String displayGroup);
    }
    private static abstract class ShapedRecipes extends net.minecraft.world.item.crafting.ShapedRecipes implements VanillaType, IDisplayRecipe {
        public final List<RecipeSlot> recipes;
        public ShapedRecipes(MinecraftKey id, String group, CraftingBookCategory category, int width, int height, List<RecipeSlot> recipes, ItemStack output) {
            super(id, group, category, width, height, NonNullList.of(RecipeItemStack.of(), recipes.stream().map(RecipeSlot::getRecipeSlotNMS).toArray(RecipeItemStack[]::new)), output);
            this.recipes = checkRecipeCrafting(id.getPath(), recipes);
        }
        public abstract net.minecraft.world.item.ItemStack result(IOutputVariable variable);
        @Override public net.minecraft.world.item.ItemStack getResultItem(IRegistryCustom custom) {
            return result(IOutputVariable.empty());
        }
        @Override public net.minecraft.world.item.ItemStack assemble(InventoryCrafting inventory, IRegistryCustom custom) {
            return inventory.getOwner() instanceof Player player
                    ? result(IOutputVariable.of(player))
                    : result(IOutputVariable.empty());
        }

        public Optional<HashMap<Integer, RecipeSlot>> craft(InventoryCrafting inventory) {
            int width = getWidth();
            int height = getHeight();
            for (int i2 = 0; i2 <= inventory.getWidth() - width; ++i2) {
                for (int j2 = 0; j2 <= inventory.getHeight() - height; ++j2) {
                    Optional<HashMap<Integer, RecipeSlot>> match = this.matches(inventory, i2, j2, true);
                    if (match.isPresent()) return match;
                    match = this.matches(inventory, i2, j2, false);
                    if (match.isPresent()) return match;
                }
            }
            return Optional.empty();
        }
        private Optional<HashMap<Integer, RecipeSlot>> matches(InventoryCrafting inv, int offsetX, int offsetY, boolean flipped) {
            HashMap<Integer, RecipeSlot> map = new HashMap<>();
            int width = getWidth();
            int height = getHeight();
            for (int k2 = 0; k2 < inv.getWidth(); ++k2) {
                for (int l2 = 0; l2 < inv.getHeight(); ++l2) {
                    int i1 = k2 - offsetX;
                    int j1 = l2 - offsetY;
                    RecipeSlot recipeitemstack = RecipeSlot.none;
                    if (i1 >= 0 && j1 >= 0 && i1 < width && j1 < height) recipeitemstack = flipped ? recipes.get(width - i1 - 1 + j1 * width) : recipes.get(i1 + j1 * width);
                    int slot = k2 + l2 * inv.getWidth();
                    if (!recipeitemstack.test(inv.getItem(slot))) return Optional.empty();
                    map.put(slot, recipeitemstack);
                }
            }
            return Optional.of(map);
        }

        @Override public NonNullList<net.minecraft.world.item.ItemStack> getRemainingItems(InventoryCrafting inventory) {
            return Crafts.getRemainingItems(craft(inventory).orElse(null), inventory);
        }
        @Override public boolean matches(InventoryCrafting inventory, net.minecraft.world.level.World world) {
            return craft(inventory).isPresent();
        }

        private Optional<net.minecraft.world.item.crafting.RecipeCrafting> displayRecipe = null;
        @Override public Stream<net.minecraft.world.item.crafting.RecipeCrafting> getDisplayRecipe(IRegistryCustom custom) {
            return (displayRecipe == null ? (displayRecipe = createDisplayRecipe(new MinecraftKey(getId().getNamespace() + ".g", getId().getPath()), this.getGroup()).map(v -> IDisplayRecipe.removeLore(v, custom))) : displayRecipe).stream();
        }
        protected abstract Optional<net.minecraft.world.item.crafting.RecipeCrafting> createDisplayRecipe(MinecraftKey displayKey, String displayGroup);
    }

    private static <T extends Collection<RecipeSlot>>T checkRecipeCrafting(String key, T slots) {
        for (RecipeSlot slot : slots) {
            if (slot.checkCrafting()) continue;
            lime.logOP("RecipeSlot in '"+key+"' is try DUPE! Slot count not equals ONE! Maybe error...");
            return slots;
        }
        return slots;
    }
    private static <T extends RecipeSlot>T checkRecipeCrafting(String key, T slot) {
        if (slot.checkCrafting()) return slot;
        lime.logOP("RecipeSlot in '"+key+"' is try DUPE! Slot count not equals ONE! Maybe error...");
        return slot;
    }

    public interface VanillaType {
        Optional<String> vanillaType();
        static Optional<String> ofInventory(InventoryCrafting inv) { return inv instanceof VanillaType vt ? vt.vanillaType() : Optional.empty(); }
        static Optional<String> ofRecipe(IRecipe<?> recipe) { return recipe instanceof VanillaType vt ? vt.vanillaType() : Optional.empty(); }
    }

    public static <T extends IInventory>NonNullList<net.minecraft.world.item.ItemStack> getRemainingItems(Map<Integer, RecipeSlot> craft, T inventory) {
        NonNullList<net.minecraft.world.item.ItemStack> nonnulllist = NonNullList.withSize(inventory.getContainerSize(), net.minecraft.world.item.ItemStack.EMPTY);
        for (int i2 = 0; i2 < nonnulllist.size(); ++i2) {
            RecipeSlot recipeSlot = craft == null ? null : craft.get(i2);
            net.minecraft.world.item.ItemStack slot = inventory.getItem(i2);
            net.minecraft.world.item.ItemStack slotItem = recipeSlot == null ? null : recipeSlot.result(slot.getCount());
            if (slotItem == null) {
                Item item = slot.getItem();
                if (!item.hasCraftingRemainingItem()) continue;
                slotItem = new net.minecraft.world.item.ItemStack(item.getCraftingRemainingItem());
            }
            nonnulllist.set(i2, slotItem);
        }
        return nonnulllist;
    }

    private static <T extends INamable>T getByName(T[] values, String name, T nullable) {
        if (name == null) return nullable;
        for (T value : values) {
            if (value.getSerializedName().equals(name))
                return value;
        }
        throw new IllegalArgumentException("Name '"+name+"' not founded in values [" + Arrays.stream(values).map(v -> "'"+v+"'").collect(Collectors.joining(",")) + "]");
    }
    private static IRecipe<?> create(MinecraftKey key, JsonObject json) {
        String group = json.has("group") ? json.get("group").getAsString() : "";
        switch (json.get("type").getAsString()) {
            case "shapeless": {
                CraftingBookCategory category = getByName(
                        CraftingBookCategory.values(),
                        json.has("category") ? json.get("category").getAsString() : null,
                        CraftingBookCategory.MISC);

                IOutputSlot output = IOutputSlot.of(json.get("output"));
                List<RecipeSlot> recipes = create(key.toString(), json.get("input").getAsJsonArray());
                Optional<String> vanilla_type = json.has("vanilla_type") ? Optional.of(json.get("vanilla_type").getAsString()) : Optional.empty();
                return new ShapelessRecipes(key, group, category, recipes, output.create(true, IOutputVariable.empty()), NonNullList.of(RecipeItemStack.of(), recipes.stream().map(RecipeSlot::getRecipeSlotNMS).toArray(RecipeItemStack[]::new))) {
                    @Override public net.minecraft.world.item.ItemStack result(IOutputVariable variable) { return output.create(false, variable); }
                    @Override public Optional<String> vanillaType() { return vanilla_type; }
                    @Override public boolean matches(InventoryCrafting inventory, World world) {
                        return VanillaType.ofInventory(inventory).equals(vanilla_type) && super.matches(inventory, world);
                    }
                    @Override protected Optional<net.minecraft.world.item.crafting.RecipeCrafting> createDisplayRecipe(MinecraftKey displayKey, String displayGroup) {
                        return Optional.of(this);
                    }
                };
            }
            case "modify": {
                CraftingBookCategory category = getByName(
                        CraftingBookCategory.values(),
                        json.has("category") ? json.get("category").getAsString() : null,
                        CraftingBookCategory.MISC);

                IOutputSlot output = IOutputSlot.of(json.get("output"));
                JsonObject input = json.getAsJsonObject("input");
                RecipeSlot modifySlot = RecipeSlot.of(key.toString(), input.get("modify"));
                List<RecipeSlot> otherSlot = create(key.toString(), input.get("other").getAsJsonArray());
                Optional<String> vanilla_type = json.has("vanilla_type") ? Optional.of(json.get("vanilla_type").getAsString()) : Optional.empty();
                return new ModifyRecipes(key, group, category, modifySlot, otherSlot, output.create(true, IOutputVariable.empty()), NonNullList.of(RecipeItemStack.of(), Stream.concat(Stream.of(modifySlot), otherSlot.stream()).map(RecipeSlot::getRecipeSlotNMS).toArray(RecipeItemStack[]::new))) {
                    @Override public net.minecraft.world.item.ItemStack result(ItemStack modify, IOutputVariable variable) {
                        return modify.isEmpty() ? output.create(false, variable) : output.modify(modify, false, variable);
                    }
                    @Override public Optional<String> vanillaType() { return vanilla_type; }
                    @Override public boolean matches(InventoryCrafting inventory, World world) {
                        return VanillaType.ofInventory(inventory).equals(vanilla_type) && super.matches(inventory, world);
                    }
                    @Override protected Optional<net.minecraft.world.item.crafting.RecipeCrafting> createDisplayRecipe(MinecraftKey displayKey, String displayGroup) {
                        return Optional.of(this);
                    }
                };
            }
            case "shaped": {
                CraftingBookCategory category = getByName(
                    CraftingBookCategory.values(),
                    json.has("category") ? json.get("category").getAsString() : null,
                    CraftingBookCategory.MISC);

                IOutputSlot output = IOutputSlot.of(json.get("output"));
                int width = json.get("width").getAsInt();
                int height = json.get("height").getAsInt();
                List<RecipeSlot> recipes = create(key.toString(), json.get("input").getAsJsonArray());
                Optional<String> vanilla_type = json.has("vanilla_type") ? Optional.of(json.get("vanilla_type").getAsString()) : Optional.empty();
                if (recipes.size() != width * height) throw new IllegalArgumentException("In craft '"+key.getPath()+"' input.length != width*height");
                return new ShapedRecipes(key, group, category, width, height, recipes, output.create(true, IOutputVariable.empty())) {
                    @Override public net.minecraft.world.item.ItemStack result(IOutputVariable variable) { return output.create(false, variable); }
                    @Override public Optional<String> vanillaType() { return vanilla_type; }
                    @Override public boolean matches(InventoryCrafting inventory, net.minecraft.world.level.World world) {
                        return VanillaType.ofInventory(inventory).equals(vanilla_type) && super.matches(inventory, world);
                    }
                    @Override protected Optional<net.minecraft.world.item.crafting.RecipeCrafting> createDisplayRecipe(MinecraftKey displayKey, String displayGroup) {
                        return Optional.of(this);
                    }
                };
            }
            case "cauldron": return new CauldronRecipe(key, group, getByName(
                        CraftingBookCategory.values(),
                        json.has("category") ? json.get("category").getAsString() : null,
                        CraftingBookCategory.MISC),
                    create(key.toString(), json.get("input").getAsJsonArray()), IOutputSlot.of(json.get("output")));
            case "waiting": return new WaitingRecipe(key, group, getByName(
                        CraftingBookCategory.values(),
                        json.has("category") ? json.get("category").getAsString() : null,
                        CraftingBookCategory.MISC),
                    RecipeSlot.of(key.toString(), json.get("input")),
                    create(key.toString(), json.get("fuel").getAsJsonArray()),
                    create(key.toString(), json.get("catalyse").getAsJsonArray()),
                    !json.has("split_catalyse") || json.get("split_catalyse").getAsBoolean(),
                    IOutputSlot.of(json.get("output")),
                    json.get("total_sec").getAsInt(),
                    json.get("waiting_type").getAsString());
            case "laboratory": return new LaboratoryRecipe(key, group, getByName(
                        CraftingBookCategory.values(),
                        json.has("category") ? json.get("category").getAsString() : null,
                        CraftingBookCategory.MISC),
                    create(key.toString(), json.get("input_thirst").getAsJsonArray()), create(key.toString(), json.get("input_dust").getAsJsonArray()), IOutputSlot.of(json.get("output")));
            case "converter":
                return new ConverterRecipe(key,
                    group,
                    getByName(
                        CraftingBookCategory.values(),
                        json.has("category") ? json.get("category").getAsString() : null,
                        CraftingBookCategory.MISC),
                    create(key.toString(), json.get("input").getAsJsonArray()),
                        json.get("output").isJsonPrimitive()
                                ? ConverterRecipe.IConverterOutput.ofString(json.get("output").getAsString())
                                : ConverterRecipe.IConverterOutput.ofMap(
                                        json.get("output").isJsonArray()
                                            ? Streams.stream(json.get("output").getAsJsonArray().iterator()).map(IOutputSlot::of).collect(Collectors.toMap(v -> v, v -> Optional.empty()))
                                            : json.get("output").getAsJsonObject().entrySet().stream().collect(Collectors.toMap(kv -> IOutputSlot.ofString(kv.getKey()), kv -> kv.getValue().isJsonNull() ? Optional.empty() : Optional.of(kv.getValue().getAsString())))
                                ),
                    json.get("converter_type").getAsString(),
                    !json.has("replace") || json.get("replace").getAsBoolean()
            );
            case "clicker": {
                CraftingBookCategory category = getByName(
                    CraftingBookCategory.values(),
                    json.has("category") ? json.get("category").getAsString() : null,
                    CraftingBookCategory.MISC);

                List<RecipeSlot> input = create(key.toString(), json.get("input").getAsJsonArray());
                int clicks = json.get("clicks").getAsInt();
                String clicker_type = json.get("clicker_type").getAsString();
                if (json.has("repair")) return ClickerRecipe.ofRepair(key, group, category, input, system.IRange.parse(json.get("repair").getAsString()), clicks, clicker_type);
                else if (json.has("enchantments")) return ClickerRecipe.ofCombine(key, group, category, input, Streams.stream(json.get("enchantments").getAsJsonArray()).map(JsonElement::getAsString).map(NamespacedKey::minecraft).map(Enchantment::getByKey).toList(), clicks, clicker_type);
                else {
                    boolean replace = !json.has("replace") || json.get("replace").getAsBoolean();
                    if (json.get("output").isJsonArray()) {
                        return ClickerRecipe.ofDefault(key, group, category, input, Streams.stream(json.get("output").getAsJsonArray().iterator()).map(IOutputSlot::of).toList(), replace, clicks, clicker_type);
                    } else {
                        return ClickerRecipe.ofDefault(key, group, category, input, List.of(IOutputSlot.of(json.get("output"))), replace, clicks, clicker_type);
                    }
                }
            }
            case "item_frame": return new ItemFrameRecipe(key, RecipeSlot.of(key.toString(), json.get("input")), IOutputSlot.of(json.get("output")), json.get("seconds").getAsInt());
        }
        switch (json.get("type").getAsString()) {
            case "blasting": return CookingType.Blasting.create(key, group, getByName(
                    CookingBookCategory.values(),
                    json.has("category") ? json.get("category").getAsString() : null,
                    CookingBookCategory.MISC),
                json);
            case "furnace": return CookingType.Furnace.create(key, group, getByName(
                    CookingBookCategory.values(),
                    json.has("category") ? json.get("category").getAsString() : null,
                    CookingBookCategory.MISC),
                json);
            case "campfire": return CookingType.Campfire.create(key, group, getByName(
                    CookingBookCategory.values(),
                    json.has("category") ? json.get("category").getAsString() : null,
                    CookingBookCategory.MISC),
                json);
            case "smoking": return CookingType.Smoking.create(key, group, getByName(
                    CookingBookCategory.values(),
                    json.has("category") ? json.get("category").getAsString() : null,
                    CookingBookCategory.MISC),
                json);
        }
        throw new IllegalArgumentException("Craft type '"+json.get("type").getAsString()+"' not founded!");
    }
    private static IRecipe<?> create(String id, JsonObject json) {
        return create(CraftNamespacedKey.toMinecraft(new NamespacedKey(lime._plugin, id)), json);
    }
}














