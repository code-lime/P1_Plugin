package org.lime.gp.block.component.data.recipe;

import net.minecraft.world.item.ItemEnchantedBook;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.block.component.data.anvil.AnvilLoader;

import java.util.*;
import java.util.stream.Collectors;

public class RepairRecipe extends IRecipe {
    public static final RepairRecipe Instance = new RepairRecipe();
    private static final List<Material> whitelist = new ArrayList<>();

    static {
        whitelist.add(Material.ENCHANTED_BOOK);
        for (Material material : Material.values()) {
            if (material.getMaxDurability() > 0) {
                whitelist.add(material);
                whitelist.addAll(getRepairs(material));
            }
        }
    }

    private RepairRecipe() {}

    private static int d(int i) {
        return i * 2 + 1;
    }
    private static ItemStack tryGetItem(ItemStack first, ItemStack second) {
        net.minecraft.world.item.ItemStack itemstack = CraftItemStack.asNMSCopy(first);
        int i = 0;

        net.minecraft.world.item.ItemStack itemstack1 = itemstack.cloneItemStack(false);
        net.minecraft.world.item.ItemStack itemstack2 = CraftItemStack.asNMSCopy(second);
        Map<Enchantment, Integer> map = EnchantmentManager.getEnchantments(itemstack1);
        if (!itemstack2.isEmpty()) {
            boolean flag = itemstack2.is(net.minecraft.world.item.Items.ENCHANTED_BOOK) && !ItemEnchantedBook.getEnchantments(itemstack2).isEmpty();
            int k;
            int l;
            int i1;
            if (itemstack1.isDamageableItem() && itemstack1.getItem().isValidRepairItem(itemstack, itemstack2)) {
                k = Math.min(itemstack1.getDamageValue(), itemstack1.getMaxDamage() / 4);
                if (k <= 0) return null;

                for (i1 = 0; k > 0 && i1 < itemstack2.getCount(); ++i1) {
                    l = itemstack1.getDamageValue() - k;
                    itemstack1.setDamageValue(l);
                    ++i;
                    k = Math.min(itemstack1.getDamageValue(), itemstack1.getMaxDamage() / 4);
                }

            } else {
                if (!flag && (!itemstack1.is(itemstack2.getItem()) || !itemstack1.isDamageableItem())) return null;

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
                            if (flag2 && !flag1) return null;
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
        return CraftItemStack.asBukkitCopy(itemstack1);
    }

    private static List<Material> getRepairs(Material item) {
        ItemStack _item = new ItemStack(item);
        return Arrays.stream(Material.values()).filter(mat -> _item.isRepairableBy(new ItemStack(mat))).collect(Collectors.toList());
    }

    @Override public boolean check(List<ItemStack> items) { return items.size() == 2 && tryGetItem(items.get(0), items.get(1)) != null; }
    @Override public ItemStack craft(List<ItemStack> items) { return tryGetItem(items.get(0), items.get(1)); }
    @Override public ItemStack checkCraft(List<ItemStack> items) { return items.size() == 2 ? tryGetItem(items.get(0), items.get(1)) : null; }
    @Override public void addToWhitelist() { AnvilLoader.whitelistMaterial.addAll(whitelist); }
    @Override public int getClicks() { return 3; }
    @Override public int getItemCount() { return 2; }
}
