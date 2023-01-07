package org.lime.gp.craft.recipe;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatModifier;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipes;
import org.lime.gp.chat.ChatHelper;

import java.util.stream.Stream;

public interface IDisplayRecipe {
    Stream<ShapedRecipes> getDisplayRecipe();

    static ItemStack amountToName(ItemStack item) {
        return nameWithPostfix(item, Component.text(" x"+item.getCount()));
    }
    static ItemStack amountToName(ItemStack item, Component postfix) {
        return nameWithPostfix(item, Component.text(" x"+item.getCount()).append(postfix));
    }
    static ItemStack nameWithPostfix(ItemStack item, Component postfix) {
        IChatBaseComponent name = item.getHoverName();
        if (!item.hasCustomHoverName()) name = new ChatComponentText("").append(item.getHoverName()).setStyle(ChatModifier.EMPTY.withItalic(false));
        return genericItem(item).setHoverName(new ChatComponentText("")
                .append(name)
                .append(ChatHelper.toNMS(postfix.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET ? postfix.decoration(TextDecoration.ITALIC, false) : postfix))
        );
    }
    static ItemStack genericItem(ItemStack item) {
        item = item.copy();
        item.addTagElement("generic", NBTTagByte.valueOf(true));
        return item;
    }

}
