package org.lime.gp.craft.recipe;

import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeCrafting;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.item.crafting.ShapedRecipes;
import net.minecraft.world.level.World;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.gp.craft.Crafts;
import org.lime.gp.craft.book.Recipes;
import org.lime.gp.craft.slot.output.IOutputSlot;
import org.lime.gp.craft.slot.RecipeAmountSlot;
import org.lime.gp.craft.slot.RecipeSlot;
import org.lime.gp.craft.slot.output.IOutputVariable;
import org.lime.gp.extension.ItemNMS;
import org.lime.gp.item.Items;
import org.lime.gp.lime;
import org.lime.system.range.IRange;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.*;
import java.util.stream.Stream;

public class ClickerRecipe extends AbstractRecipe {
    public final ImmutableList<RecipeSlot> input;

    private interface Action {
        void test(ClickerRecipe recipe);
        boolean checkItem(ItemStack item);
        Stream<RecipeCrafting> createDisplayRecipe(ClickerRecipe recipe, MinecraftKey displayKey, String displayGroup, CraftingBookCategory category);
        Stream<ItemStack> assembleList(ClickerRecipe recipe, IInventory inventory, IRegistryCustom custom, IOutputVariable variable);
        default boolean matches(ClickerRecipe recipe, IInventory inventory, World world) {
            List<ItemStack> items = inventory.getContents().stream().filter(v -> !v.isEmpty()).toList();
            int length = items.size();
            if (recipe.input.size() != length) return false;
            for (int i = 0; i < items.size(); i++)
                if (!recipe.input.get(i).test(items.get(i)))
                    return false;
            return true;
        }

        static Action ofDefault(List<IOutputSlot> output, boolean replace) {
            return new Action() {
                @Override public void test(ClickerRecipe recipe) { }
                @Override public boolean checkItem(ItemStack item) {
                    for (IOutputSlot slot : output)
                        if (slot.test(item))
                            return true;
                    return false;
                }

                @Override public Stream<RecipeCrafting> createDisplayRecipe(ClickerRecipe recipe, MinecraftKey displayKey, String displayGroup, CraftingBookCategory category) {
                    NonNullList<RecipeItemStack> slots = NonNullList.withSize(3*3, RecipeItemStack.EMPTY);
                    List<RecipeItemStack> items = recipe.input.stream().map(v -> v.getWhitelistIngredientsShow().map(IDisplayRecipe::amountToName)).map(RecipeItemStack::of).toList();
                    int count = Math.min(items.size(), slots.size());
                    for (int i = 0; i < count; i++) slots.set(i, items.get(i));
                    return Stream.of(new ShapedRecipes(displayKey, displayGroup, category, 3, 3, slots, IDisplayRecipe.nameWithPostfix(output.get(0).create(true, IOutputVariable.empty()), Component.text(" +" + recipe.clicks + " кликов").color(NamedTextColor.LIGHT_PURPLE))));
                }
                @Override public Stream<ItemStack> assembleList(ClickerRecipe recipe, IInventory inventory, IRegistryCustom custom, IOutputVariable variable) {
                    return replace
                            ? output.stream().map(v -> v.create(false, variable))
                            : Stream.concat(Stream.of(output.get(0).modify(inventory.getItem(0), false, variable)), output.stream().skip(1).map(v -> v.create(false, variable)));
                }
            };
        }
        static Action ofRepair(IRange repair) {
            return new Action() {
                @Override public void test(ClickerRecipe recipe) {
                    recipe.input.forEach(slot -> {
                        if (!(slot instanceof RecipeAmountSlot)) {
                            lime.logOP("WARNING LOAD RECIPE '"+recipe.getId()+"'! Slot not amounted!");
                        }
                    });
                }
                @Override public boolean checkItem(ItemStack item) { return false; }
                @Override public Stream<RecipeCrafting> createDisplayRecipe(ClickerRecipe recipe, MinecraftKey displayKey, String displayGroup, CraftingBookCategory category) {
                    Toast1<Integer> index = Toast.of(0);
                    return recipe.input.get(0).getWhitelistIngredientsShow().map(firstItem -> {
                        index.val0++;
                        NonNullList<RecipeItemStack> slots = NonNullList.withSize(3*3, RecipeItemStack.EMPTY);
                        List<RecipeItemStack> items = Stream.concat(Stream.of(Stream.of(withDurability(firstItem, repair.getMax(firstItem.getMaxDamage())))), recipe.input.stream().skip(1).map(v -> v.getWhitelistIngredientsShow().map(IDisplayRecipe::amountToName))).map(RecipeItemStack::of).toList();
                        int count = Math.min(items.size(), slots.size());
                        for (int i = 0; i < count; i++) slots.set(i, items.get(i));
                        return new ShapedRecipes(
                                new MinecraftKey(displayKey.getNamespace() + "." + index.val0, displayKey.getPath()),
                                displayGroup,
                                category,
                                3, 3,
                                slots,
                                ItemNMS.addLore(
                                        IDisplayRecipe.nameWithPostfix(firstItem, Component.text(" +" + recipe.clicks + " кликов").color(NamedTextColor.LIGHT_PURPLE)),
                                        Stream.of(
                                                Component.empty(),
                                                Component.text("Починка: ").color(NamedTextColor.GRAY).append(Component.text(repair.displayText()).color(NamedTextColor.GREEN)).decoration(TextDecoration.ITALIC, false)
                                        )
                                )
                        );
                    });
                }
                @Override public Stream<ItemStack> assembleList(ClickerRecipe recipe, IInventory inventory, IRegistryCustom custom, IOutputVariable variable) {
                    org.bukkit.inventory.ItemStack base_item = CraftItemStack.asBukkitCopy(inventory.getItem(0));
                    if (base_item.getItemMeta() instanceof Damageable damageable) {
                        double maxDamage = Items.getMaxDamage(base_item);
                        double damage = damageable.getDamage();

                        int amount = Integer.MAX_VALUE;
                        int length = recipe.input.size();
                        for (int i = 1; i < length; i++) {
                            RecipeAmountSlot amountSlot = (RecipeAmountSlot)recipe.input.get(i);
                            amount = Math.min(amount, inventory.getItem(i).getCount() / amountSlot.amount);
                        }
                        if (amount == Integer.MAX_VALUE) amount = 0;
                        for (int i = 0; i < amount; i++) damage -= repair.getValue(maxDamage);
                        damage = Math.max(0, damage);
                        damageable.setDamage((int) Math.floor(damage));
                        base_item.setItemMeta(damageable);
                    }
                    return Stream.of(CraftItemStack.asNMSCopy(base_item));
                }
            };
        }
        static Action ofCombine(List<Enchantment> enchantments) {
            return new Action() {
                @Override public void test(ClickerRecipe recipe) {
                    if (recipe.input.size() == 2) return;
                    lime.logOP("WARNING LOAD RECIPE '"+recipe.getId()+"'! Input length only 2! Now: " + recipe.input.size());
                }
                @Override public boolean checkItem(ItemStack item) { return false; }
                @Override public Stream<RecipeCrafting> createDisplayRecipe(ClickerRecipe recipe, MinecraftKey displayKey, String displayGroup, CraftingBookCategory category) {
                    Toast1<Integer> index = Toast.of(0);
                    List<String> whitelistSecond = recipe.input.get(1).getWhitelistKeys().toList();
                    return Stream.concat(
                            whitelistSecond.contains(Items.getMaterialKey(Material.ENCHANTED_BOOK))
                                    ? recipe.input.get(0)
                                    .getWhitelistIngredientsShow()
                                    .map(firstItem -> {
                                        index.val0++;

                                        ItemStack _out = firstItem.copy();

                                        NonNullList<RecipeItemStack> slots = NonNullList.withSize(3*3, RecipeItemStack.EMPTY);
                                        List<RecipeItemStack> items = Stream.concat(Stream.of(Stream.of(firstItem)), Stream.of(enchantments.stream()
                                                .map(v -> v instanceof CraftEnchantment ce ? ce : null)
                                                .filter(Objects::nonNull)
                                                .map(enchantment -> {
                                                    ItemNMS.addEnchant(_out, enchantment.getHandle(), 0);

                                                    org.bukkit.inventory.ItemStack ebook = new org.bukkit.inventory.ItemStack(Material.ENCHANTED_BOOK);
                                                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta)ebook.getItemMeta();
                                                    meta.addStoredEnchant(enchantment, 0, true);
                                                    ebook.setItemMeta(meta);
                                                    ItemStack _ebook = CraftItemStack.asNMSCopy(ebook);
                                                    return IDisplayRecipe.amountToName(_ebook);
                                                })))
                                                .map(RecipeItemStack::of)
                                                .toList();
                                        int count = Math.min(items.size(), slots.size());
                                        for (int i = 0; i < count; i++) slots.set(i, items.get(i));
                                        return new ShapedRecipes(
                                                new MinecraftKey(displayKey.getNamespace() + "." + index.val0, displayKey.getPath()),
                                                displayGroup,
                                                category,
                                                3, 3,
                                                slots,
                                                IDisplayRecipe.nameWithPostfix(_out, Component.text(" +" + recipe.clicks + " кликов").color(NamedTextColor.LIGHT_PURPLE))
                                        );
                                    })
                                    : Stream.empty(),
                            recipe.input.get(0)
                                    .getWhitelistIngredientsShow()
                                    .filter(v -> Items.getGlobalKeyByItem(v).map(whitelistSecond::contains).orElse(false))
                                    .map(firstItem -> {
                                        index.val0++;
                                        NonNullList<RecipeItemStack> slots = NonNullList.withSize(3*3, RecipeItemStack.EMPTY);
                                        List<RecipeItemStack> items = Stream.concat(Stream.of(Stream.of(firstItem)), Stream.of(Stream.of(firstItem))).map(RecipeItemStack::of).toList();
                                        int count = Math.min(items.size(), slots.size());
                                        for (int i = 0; i < count; i++) slots.set(i, items.get(i));
                                        return new ShapedRecipes(
                                                new MinecraftKey(displayKey.getNamespace() + "." + index.val0, displayKey.getPath()),
                                                displayGroup,
                                                category,
                                                3, 3,
                                                slots,
                                                ItemNMS.addLore(
                                                        IDisplayRecipe.nameWithPostfix(firstItem, Component.text(" +" + recipe.clicks + " кликов").color(NamedTextColor.LIGHT_PURPLE)),
                                                        Stream.of(
                                                                Component.empty(),
                                                                Component.text("Починка: ").color(NamedTextColor.GRAY).append(Component.text("Объединение").color(NamedTextColor.GREEN)).decoration(TextDecoration.ITALIC, false)
                                                        )
                                                )
                                        );
                                    })
                    );
                }
                @Override public Stream<ItemStack> assembleList(ClickerRecipe recipe, IInventory inventory, IRegistryCustom custom, IOutputVariable variable) {
                    if (inventory.getItem(1).isEmpty()) return Stream.of(inventory.getItem(0));

                    org.bukkit.inventory.ItemStack base_item = inventory.getItem(0).asBukkitCopy();
                    org.bukkit.inventory.ItemStack second_item = inventory.getItem(1).asBukkitCopy();

                    ItemMeta base_meta = base_item.getItemMeta();
                    ItemMeta second_meta = second_item.getItemMeta();

                    if (base_meta instanceof Damageable base_damage
                            && second_meta instanceof Damageable second_damage
                            && Items.getGlobalKeyByItem(base_item).equals(Items.getGlobalKeyByItem(second_item))
                    ) {
                        int max_damage = Items.getMaxDamage(base_item);
                        int durability = (max_damage - base_damage.getDamage()) + (max_damage - second_damage.getDamage());
                        base_damage.setDamage(Math.min(max_damage, Math.max(0, max_damage - durability)));
                    }
                    HashMap<Enchantment, Integer> result_enchants = new HashMap<>(base_meta.getEnchants());
                    if (base_meta instanceof EnchantmentStorageMeta enchantmentStorageMeta) result_enchants.putAll(enchantmentStorageMeta.getStoredEnchants());
                    second_meta.getEnchants()
                            .forEach((enchantment, level) -> {
                                for (Enchantment e : result_enchants.keySet())
                                    if (e != enchantment && e.conflictsWith(enchantment))
                                        return;
                                result_enchants.compute(enchantment, (e, v) -> {
                                    if (v == null) return level;
                                    int max = e.getMaxLevel();
                                    return Math.min(max, level.equals(v)
                                            ? level + 1
                                            : Math.max(level, v)
                                    );
                                });
                            });
                    if (second_meta instanceof EnchantmentStorageMeta enchantmentStorageMeta) enchantmentStorageMeta.getStoredEnchants()
                            .forEach((enchantment, level) -> {
                                for (Enchantment e : result_enchants.keySet())
                                    if (e != enchantment && e.conflictsWith(enchantment))
                                        return;
                                result_enchants.compute(enchantment, (e, v) -> {
                                    if (v == null) return level;
                                    int max = e.getMaxLevel();
                                    return Math.min(max, level.equals(v)
                                            ? level + 1
                                            : Math.max(level, v)
                                    );
                                });
                            });
                    base_meta.getEnchants().keySet().forEach(base_meta::removeEnchant);
                    if (base_meta instanceof EnchantmentStorageMeta enchantmentStorageMeta) {
                        enchantmentStorageMeta.getStoredEnchants().keySet().forEach(enchantmentStorageMeta::removeStoredEnchant);
                        result_enchants.forEach((e,lvl) -> enchantmentStorageMeta.addStoredEnchant(e,lvl,true));
                    } else {
                        result_enchants.forEach((e,lvl) -> base_meta.addEnchant(e,lvl,true));
                    }

                    base_item.setItemMeta(base_meta);

                    return Stream.of(CraftItemStack.asNMSCopy(base_item));
                }
                @Override public boolean matches(ClickerRecipe recipe, IInventory inventory, World world) {
                    return Action.super.matches(recipe, inventory, world)
                            && (inventory.getItem(1).is(net.minecraft.world.item.Items.ENCHANTED_BOOK)
                            || Items.getGlobalKeyByItem(inventory.getItem(0)).equals(Items.getGlobalKeyByItem(inventory.getItem(1))));
                }
            };
        }
    }

    private final Action action;

    public final int clicks;
    public final String clicker_type;

    public static ClickerRecipe ofDefault(MinecraftKey key, String group, CraftingBookCategory category, List<RecipeSlot> input, List<IOutputSlot> output, boolean replace, int clicks, String clicker_type) {
        return new ClickerRecipe(key, group, category, input, Action.ofDefault(output, replace), clicks, clicker_type);
    }
    public static ClickerRecipe ofRepair(MinecraftKey key, String group, CraftingBookCategory category, List<RecipeSlot> input, IRange repair, int clicks, String clicker_type) {
        return new ClickerRecipe(key, group, category, input, Action.ofRepair(repair), clicks, clicker_type);
    }
    public static ClickerRecipe ofCombine(MinecraftKey key, String group, CraftingBookCategory category, List<RecipeSlot> input, List<Enchantment> enchantments, int clicks, String clicker_type) {
        return new ClickerRecipe(key, group, category, input, Action.ofCombine(enchantments), clicks, clicker_type);
    }

    private ClickerRecipe(MinecraftKey key, String group, CraftingBookCategory category, List<RecipeSlot> input, Action action, int clicks, String clicker_type) {
        super(key, group, category, Recipes.CLICKER);
        this.input = ImmutableList.copyOf(input);
        this.action = action;
        this.clicks = clicks;
        this.clicker_type = clicker_type;

        this.action.test(this);
    }
    @Override public boolean matches(IInventory inventory, World world) {
        return this.action.matches(this, inventory, world);
    }
    @Override public ItemStack assemble(IInventory inventory, IRegistryCustom custom, IOutputVariable variable) {
        return this.action.assembleList(this, inventory, custom, variable).findFirst().get();
    }

    public Stream<ItemStack> assembleList(IInventory inventory, IRegistryCustom custom, IOutputVariable variable) {
        return this.action.assembleList(this, inventory, custom, variable);
    }

    @Override public NonNullList<ItemStack> getRemainingItems(IInventory inventory) {
        return Crafts.getRemainingItems(input.stream().collect(HashMap::new, (map, v) -> map.put(map.size(), v), Map::putAll), inventory);
    }

    @Override public Stream<String> getWhitelistKeys() { return input.stream().flatMap(RecipeSlot::getWhitelistKeys).distinct(); }

    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(IRegistryCustom custom) { return ItemStack.EMPTY; }

    static ItemStack withDurability(ItemStack item, double damage) {
        item = IDisplayRecipe.genericItem(item);
        int maxDamage = item.getMaxDamage();
        item.setDamageValue((int)Math.min(maxDamage, damage));
        return item;
    }
    @Override protected Stream<RecipeCrafting> createDisplayRecipe(MinecraftKey displayKey, String displayGroup, CraftingBookCategory category) {
        return action.createDisplayRecipe(this, displayKey, displayGroup, category);
    }
}















