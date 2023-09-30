package org.lime.gp.player.module.cinv;

import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.stream.Stream;

public class GroupElement {
    private final String name;
    private final ImmutableList<ItemElement> items;

    public String name() { return name; }

    public int size() { return items.size(); }
    public @Nullable ItemElement get(int index) { return index < 0 || index >= items.size() ? null : items.get(index); }

    public ItemStack show() {
        ItemStack show = items.get(0).show();
        ItemMeta showMeta = show.getItemMeta();

        ItemStack item = new ItemStack(show.getType());
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(showMeta.getCustomModelData());
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.YELLOW));
        item.setItemMeta(meta);
        item.setAmount(1);
        return item;
    }

    public GroupElement(String name, ImmutableList<ItemElement> items) {
        this.name = name;
        this.items = items;
    }

    public Stream<ItemElement> rawSearch(String search) {
        return items.stream().filter(v -> v.isSearch(search));
    }
}
