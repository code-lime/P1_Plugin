package org.lime.gp.extension;

import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.chat.ChatHelper;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static net.minecraft.world.item.ItemStack.TAG_LORE;

public class ItemNMS {
    public static Optional<NBTTagList> getLore(ItemStack item) {
        NBTTagCompound tag = item.getTag();
        if (tag == null) return Optional.empty();
        if (!tag.contains(ItemStack.TAG_DISPLAY, NBTBase.TAG_COMPOUND)) return Optional.empty();
        NBTTagCompound display = tag.getCompound(ItemStack.TAG_DISPLAY);
        if (!display.contains(TAG_LORE, NBTBase.TAG_LIST)) return Optional.empty();
        return Optional.of(display.getList(TAG_LORE, NBTBase.TAG_STRING));
    }
    public static ItemStack setLore(ItemStack item, NBTTagList list) {
        NBTTagCompound tag = item.getTag();
        if (tag == null) return item;
        if (!tag.contains(ItemStack.TAG_DISPLAY, NBTBase.TAG_COMPOUND)) {
            if (list == null) return item;
            tag.put(ItemStack.TAG_DISPLAY, new NBTTagCompound());
        }
        NBTTagCompound display = tag.getCompound(ItemStack.TAG_DISPLAY);
        if (!display.contains(TAG_LORE, NBTBase.TAG_LIST) && list == null) return item;
        if (list == null) display.remove(TAG_LORE);
        else display.put(TAG_LORE, list);
        return item;
    }
    public static ItemStack addLore(ItemStack item, Stream<? extends ComponentLike> stream) {
        NBTTagList lore = getLore(item).orElseGet(NBTTagList::new);
        stream.map(ComponentLike::asComponent).map(ChatHelper::toNMS).map(IChatBaseComponent.ChatSerializer::toJson).map(NBTTagString::valueOf).forEach(lore::add);
        return setLore(item, lore);
    }
    public static ItemStack addEnchant(ItemStack item, Enchantment enchantment, int level) {
        NBTTagCompound tag = item.getOrCreateTag();
        if (!tag.contains(ItemStack.TAG_ENCH, 9)) tag.put(ItemStack.TAG_ENCH, new NBTTagList());
        NBTTagList nbttaglist = tag.getList(ItemStack.TAG_ENCH, 10);
        nbttaglist.add(EnchantmentManager.storeEnchantment(EnchantmentManager.getEnchantmentId(enchantment), (byte)level));
        return item;
    }

    public static Optional<TextColor> getColor(ItemStack item) {
        NBTTagCompound tag = item.getTag();
        if (tag == null) return Optional.empty();
        if (!tag.contains(ItemStack.TAG_DISPLAY, NBTBase.TAG_COMPOUND)) return Optional.empty();
        NBTTagCompound display = tag.getCompound(ItemStack.TAG_DISPLAY);
        if (!display.contains(ItemStack.TAG_COLOR, NBTBase.TAG_ANY_NUMERIC)) return Optional.empty();
        return Optional.of(TextColor.color(display.getInt(ItemStack.TAG_COLOR)));
    }
    public static void setColor(ItemStack item, TextColor color) {
        NBTTagCompound tag = item.getTag();
        if (tag == null) return;
        if (!tag.contains(ItemStack.TAG_DISPLAY, NBTBase.TAG_COMPOUND)) {
            if (color == null) return;
            tag.put(ItemStack.TAG_DISPLAY, new NBTTagCompound());
        }
        NBTTagCompound display = tag.getCompound(ItemStack.TAG_DISPLAY);
        if (!display.contains(ItemStack.TAG_COLOR, NBTBase.TAG_ANY_NUMERIC) && color == null) return;
        if (color == null) display.remove(ItemStack.TAG_COLOR);
        else display.putInt(ItemStack.TAG_COLOR, color.value());
    }

    public static final String TAG_CUSTOM_MODEL_DATA = "CustomModelData";

    public static Optional<Integer> getCustomModelData(ItemStack item) {
        NBTTagCompound tag = item.getTag();
        if (tag == null) return Optional.empty();
        if (!tag.contains(TAG_CUSTOM_MODEL_DATA, NBTBase.TAG_ANY_NUMERIC)) return Optional.empty();
        return Optional.of(tag.getInt(TAG_CUSTOM_MODEL_DATA));
    }
    public static void setCustomModelData(ItemStack item, Integer custom_model_data) {
        NBTTagCompound tag = item.getTag();
        if (tag == null) return;
        if (!tag.contains(TAG_CUSTOM_MODEL_DATA, NBTBase.TAG_ANY_NUMERIC) && custom_model_data == null) return;
        if (custom_model_data == null) tag.remove(TAG_CUSTOM_MODEL_DATA);
        else tag.putInt(TAG_CUSTOM_MODEL_DATA, custom_model_data);
    }

    public static NBTTagCompound getUnhandledTags(ItemMeta item) {
        Map<String, NBTBase> unhandledTags = ReflectionAccess.unhandledTags_CraftMetaItem.get(item);
        return ReflectionAccess.initMap_NBTTagCompound.newInstance(unhandledTags);
    }
}







