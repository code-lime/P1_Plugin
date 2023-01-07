package org.lime.gp.craft.recipe;

import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.item.crafting.ShapedRecipes;
import net.minecraft.world.level.World;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R2.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.gp.craft.Crafts;
import org.lime.gp.craft.slot.OutputSlot;
import org.lime.gp.craft.slot.RecipeAmountSlot;
import org.lime.gp.craft.slot.RecipeSlot;
import org.lime.gp.extension.ItemNMS;
import org.lime.gp.item.Items;
import org.lime.system;

import java.util.*;
import java.util.stream.Stream;

public class ClickerRecipe extends AbstractRecipe {
    public final ImmutableList<RecipeSlot> input;

    private interface Action {
        Stream<ShapedRecipes> createDisplayRecipe(ClickerRecipe recipe, MinecraftKey displayKey, String displayGroup);
        ItemStack assemble(ClickerRecipe recipe, IInventory inventory);
        default boolean matches(ClickerRecipe recipe, IInventory inventory, World world) {
            List<ItemStack> items = inventory.getContents().stream().filter(v -> !v.isEmpty()).toList();
            int length = items.size();
            if (recipe.input.size() != length) return false;
            for (int i = 0; i < items.size(); i++)
                if (!recipe.input.get(i).test(items.get(i)))
                    return false;
            return true;
        }

        static Action ofDefault(OutputSlot output) {
            return new Action() {
                @Override public Stream<ShapedRecipes> createDisplayRecipe(ClickerRecipe recipe, MinecraftKey displayKey, String displayGroup) {
                    NonNullList<RecipeItemStack> slots = NonNullList.withSize(3*3, RecipeItemStack.EMPTY);
                    List<RecipeItemStack> items = recipe.input.stream().map(v -> v.getWhitelistIngredientsShow().map(IDisplayRecipe::amountToName)).map(RecipeItemStack::of).toList();
                    int count = Math.min(items.size(), slots.size());
                    for (int i = 0; i < count; i++) slots.set(i, items.get(i));
                    return Stream.of(new ShapedRecipes(displayKey, displayGroup, 3, 3, slots, IDisplayRecipe.nameWithPostfix(output.nms(), Component.text(" +" + recipe.clicks + " кликов").color(NamedTextColor.LIGHT_PURPLE))));
                }
                @Override public ItemStack assemble(ClickerRecipe recipe, IInventory inventory) {
                    return output.nms();
                }
            };
        }
        static Action ofRepair(system.IRange repair) {
            return new Action() {
                @Override public Stream<ShapedRecipes> createDisplayRecipe(ClickerRecipe recipe, MinecraftKey displayKey, String displayGroup) {
                    system.Toast1<Integer> index = system.toast(0);
                    return recipe.input.get(0).getWhitelistIngredientsShow().map(firstItem -> {
                        index.val0++;
                        NonNullList<RecipeItemStack> slots = NonNullList.withSize(3*3, RecipeItemStack.EMPTY);
                        List<RecipeItemStack> items = Stream.concat(Stream.of(Stream.of(withDurability(firstItem, repair.getMax(firstItem.getMaxDamage())))), recipe.input.stream().skip(1).map(v -> v.getWhitelistIngredientsShow().map(IDisplayRecipe::amountToName))).map(RecipeItemStack::of).toList();
                        int count = Math.min(items.size(), slots.size());
                        for (int i = 0; i < count; i++) slots.set(i, items.get(i));
                        return new ShapedRecipes(
                                new MinecraftKey(displayKey.getNamespace() + "." + index.val0, displayKey.getPath()),
                                displayGroup,
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
                @Override public ItemStack assemble(ClickerRecipe recipe, IInventory inventory) {
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
                    return CraftItemStack.asNMSCopy(base_item);
                }
            };
        }
        static Action ofCombine(List<Enchantment> enchantments) {
            return new Action() {
                @Override public Stream<ShapedRecipes> createDisplayRecipe(ClickerRecipe recipe, MinecraftKey displayKey, String displayGroup) {
                    system.Toast1<Integer> index = system.toast(0);
                    List<String> whitelistSecond = recipe.input.get(1).getWhitelistKeys().toList();
                    return Stream.concat(
                            /*whitelistSecond.contains(Items.getMaterialKey(Material.ENCHANTED_BOOK))
                                    ? enchantments.stream()
                                    .map(v -> v instanceof CraftEnchantment ce ? ce : null)
                                    .filter(Objects::nonNull)
                                    .flatMap(enchantment -> {
                                        org.bukkit.inventory.ItemStack ebook = new org.bukkit.inventory.ItemStack(Material.ENCHANTED_BOOK);
                                        EnchantmentStorageMeta meta = (EnchantmentStorageMeta)ebook.getItemMeta();
                                        meta.addStoredEnchant(enchantment, 1, true);
                                        ebook.setItemMeta(meta);
                                        ItemStack _ebook = CraftItemStack.asNMSCopy(ebook);

                                        return recipe.input.get(0)
                                                .getWhitelistIngredientsShow()
                                                .map(firstItem -> {
                                                    index.val0++;
                                                    NonNullList<RecipeItemStack> slots = NonNullList.withSize(3*3, RecipeItemStack.EMPTY);
                                                    List<RecipeItemStack> items = Stream.concat(Stream.of(Stream.of(firstItem)), Stream.of(Stream.of(IDisplayRecipe.amountToName(_ebook)))).map(RecipeItemStack::of).toList();
                                                    int count = Math.min(items.size(), slots.size());
                                                    for (int i = 0; i < count; i++) slots.set(i, items.get(i));
                                                    ItemStack _out = firstItem.copy();
                                                    _out.enchant(enchantment.getHandle(), 1);
                                                    return new ShapedRecipes(
                                                            new MinecraftKey(displayKey.getNamespace() + "." + index.val0, displayKey.getPath()),
                                                            displayGroup,
                                                            3, 3,
                                                            slots,
                                                            IDisplayRecipe.nameWithPostfix(_out, Component.text(" +" + recipe.clicks + " кликов").color(NamedTextColor.LIGHT_PURPLE))
                                                    );
                                                });

                                    })
                                    : Stream.empty(),*/
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
                    ) ;
                }
                @Override public ItemStack assemble(ClickerRecipe recipe, IInventory inventory) {
                    if (inventory.getItem(1).isEmpty()) return inventory.getItem(0);

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

                    return CraftItemStack.asNMSCopy(base_item);
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

    public static ClickerRecipe ofDefault(MinecraftKey key, String group, List<RecipeSlot> input, OutputSlot output, int clicks, String clicker_type) {
        return new ClickerRecipe(key, group, input, Action.ofDefault(output), clicks, clicker_type);
    }
    public static ClickerRecipe ofRepair(MinecraftKey key, String group, List<RecipeSlot> input, system.IRange repair, int clicks, String clicker_type) {
        return new ClickerRecipe(key, group, input, Action.ofRepair(repair), clicks, clicker_type);
    }
    public static ClickerRecipe ofCombine(MinecraftKey key, String group, List<RecipeSlot> input, List<Enchantment> enchantments, int clicks, String clicker_type) {
        return new ClickerRecipe(key, group, input, Action.ofCombine(enchantments), clicks, clicker_type);
    }

    private ClickerRecipe(MinecraftKey key, String group, List<RecipeSlot> input, Action action, int clicks, String clicker_type) {
        super(key, group, Recipes.CLICKER);
        this.input = ImmutableList.copyOf(input);
        this.action = action;
        this.clicks = clicks;
        this.clicker_type = clicker_type;
    }

    @Override public boolean matches(IInventory inventory, World world) {
        return this.action.matches(this, inventory, world);
    }
    @Override public ItemStack assemble(IInventory inventory) {
        return this.action.assemble(this, inventory);
    }

    @Override public NonNullList<ItemStack> getRemainingItems(IInventory inventory) {
        return Crafts.getRemainingItems(input.stream().collect(HashMap::new, (map, v) -> map.put(map.size(), v), Map::putAll), inventory);
    }

    @Override public Stream<String> getWhitelistKeys() { return input.stream().flatMap(RecipeSlot::getWhitelistKeys).distinct(); }

    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem() { return ItemStack.EMPTY; }

    /*private static int d(int i) {
        return i * 2 + 1;
    }
    private static Optional<ItemStack> tryGetItem(ItemStack first, ItemStack second) {
        net.minecraft.world.item.ItemStack itemstack = first.cloneItemStack(false);
        int i = 0;

        net.minecraft.world.item.ItemStack itemstack1 = itemstack.cloneItemStack(false);
        net.minecraft.world.item.ItemStack itemstack2 = second.cloneItemStack(false);
        Map<Enchantment, Integer> map = EnchantmentManager.getEnchantments(itemstack1);
        if (!itemstack2.isEmpty()) {
            boolean flag = itemstack2.is(net.minecraft.world.item.Items.ENCHANTED_BOOK) && !ItemEnchantedBook.getEnchantments(itemstack2).isEmpty();
            int k;
            int l;
            int i1;
            if (itemstack1.isDamageableItem() && itemstack1.getItem().isValidRepairItem(itemstack, itemstack2)) {
                k = Math.min(itemstack1.getDamageValue(), itemstack1.getMaxDamage() / 4);
                if (k <= 0) return Optional.empty();

                for (i1 = 0; k > 0 && i1 < itemstack2.getCount(); ++i1) {
                    l = itemstack1.getDamageValue() - k;
                    itemstack1.setDamageValue(l);
                    ++i;
                    k = Math.min(itemstack1.getDamageValue(), itemstack1.getMaxDamage() / 4);
                }

            } else {
                if (!flag && (!itemstack1.is(itemstack2.getItem()) || !itemstack1.isDamageableItem())) return Optional.empty();

                if (itemstack1.isDamageableItem() && !flag) {
                    k = itemstack.getMaxDamage() - itemstack.getDamageValue();
                    i1 = itemstack2.getMaxDamage() - itemstack2.getDamageValue();
                    l = i1 + itemstack1.getMaxDamage() * 12 / 100;
                    int j1 = k + l;
                    int k1 = itemstack1.getMaxDamage() - j1;
                    if (k1 < 0) {
                        k1 = 0;
                    }

                    if (k1 < itemstack1.getDamageValue()) {
                        itemstack1.setDamageValue(k1);
                        i += 2;
                    }
                }

                Map<Enchantment, Integer> map1 = EnchantmentManager.getEnchantments(itemstack2);
                boolean flag1 = false;
                boolean flag2 = false;
                Iterator<?> iterator = map1.keySet().iterator();

                label159:
                while (true) {
                    Enchantment enchantment;
                    do {
                        if (!iterator.hasNext()) {
                            if (flag2 && !flag1) return Optional.empty();
                            break label159;
                        }

                        enchantment = (Enchantment) iterator.next();
                    } while (enchantment == null);

                    int l1 = map.getOrDefault(enchantment, 0);
                    int i2 = map1.get(enchantment);
                    i2 = l1 == i2 ? i2 + 1 : Math.max(i2, l1);
                    boolean flag3 = enchantment.canEnchant(itemstack);

                    for (Enchantment enchantment1 : map.keySet()) {
                        if (enchantment1 != enchantment && !enchantment.isCompatibleWith(enchantment1)) {
                            flag3 = false;
                            ++i;
                        }
                    }

                    if (!flag3) {
                        flag2 = true;
                    } else {
                        flag1 = true;
                        if (i2 > enchantment.getMaxLevel()) {
                            i2 = enchantment.getMaxLevel();
                        }

                        map.put(enchantment, i2);
                        int j2 = switch (enchantment.getRarity()) {
                            case COMMON -> 1;
                            case UNCOMMON -> 2;
                            case RARE -> 4;
                            case VERY_RARE -> 8;
                        };

                        if (flag) {
                            j2 = Math.max(1, j2 / 2);
                        }

                        i += j2 * i2;
                        if (itemstack.getCount() > 1) {
                            i = 40;
                        }
                    }
                }
            }
        }
        if (i <= 0) itemstack1 = net.minecraft.world.item.ItemStack.EMPTY;
        if (!itemstack1.isEmpty()) {
            int k2 = itemstack1.getBaseRepairCost();
            if (!itemstack2.isEmpty() && k2 < itemstack2.getBaseRepairCost()) k2 = itemstack2.getBaseRepairCost();
            k2 = d(k2);
            itemstack1.setRepairCost(k2);
            EnchantmentManager.setEnchantments(map, itemstack1);
        }
        return itemstack1.isEmpty() ? Optional.empty() : Optional.of(itemstack1);
    }

    public static final ClickerRecipe ANVIL_DEFAULT = new ClickerRecipe(new MinecraftKey("lime", "anvil_default"), "") {
        private static final List<String> whitelistKeys = Streams.concat(
                Stream.of(Material.ENCHANTED_BOOK),
                Arrays.stream(Material.values())
                        .filter(v -> v.getMaxDurability() > 0)
                        .flatMap(v -> Streams.concat(Stream.of(v), getRepairs(v)))
        ).distinct().map(Items::getMaterialKey).toList();
        private static Stream<Material> getRepairs(Material item) {
            org.bukkit.inventory.ItemStack _item = new org.bukkit.inventory.ItemStack(item);
            return Arrays.stream(Material.values()).filter(mat -> _item.isRepairableBy(new org.bukkit.inventory.ItemStack(mat)));
        }

        @Override public NonNullList<ItemStack> getRemainingItems(IInventory inventory) { return NonNullList.create(); }

        @Override public boolean matches(IInventory inventory, World world) { return ClickerRecipe.tryGetItem(inventory.getItem(0), inventory.getItem(1)).isPresent(); }
        @Override public ItemStack assemble(IInventory inventory) { return ClickerRecipe.tryGetItem(inventory.getItem(0), inventory.getItem(1)).orElseThrow(); }
        @Override public Stream<String> getWhitelistKeys() { return whitelistKeys.stream(); }
        @Override protected Stream<ShapedRecipes> createDisplayRecipe(MinecraftKey displayKey, String displayGroup) { return Stream.empty(); }
    };*/

    static ItemStack withDurability(ItemStack item, double damage) {
        item = IDisplayRecipe.genericItem(item);
        int maxDamage = item.getMaxDamage();
        item.setDamageValue((int)Math.min(maxDamage, damage));
        return item;
    }
    @Override protected Stream<ShapedRecipes> createDisplayRecipe(MinecraftKey displayKey, String displayGroup) {
        return action.createDisplayRecipe(this, displayKey, displayGroup);
    }
}















