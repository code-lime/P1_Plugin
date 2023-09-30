package org.lime.gp.player.module.cinv;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.player.EntityHuman;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.data.IItemCreator;
import org.lime.gp.lime;
import org.lime.system.Regex;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ItemElement {
    private final IItemCreator creator;
    private final ItemStack show;
    private final List<String> searchList = new ArrayList<>();

    public String key() { return creator.getKey(); }
    public ItemStack show() { return show; }

    public ItemElement(IItemCreator creator) {
        this.creator = creator;
        this.show = createWithoutThrow(searchList);
    }

    /*
            if (click.isShiftClick() && click.isLeftClick()) {
                element.craftQuery(view.container, human);
                return;
            }
    */
    public void click(ViewContainer container, EntityPlayer player, ClickType click) {
        if (click.isShiftClick() && click.isLeftClick()) {
            craftQuery(container, player);
            return;
        }
        execute(player.getBukkitEntity(), click.isShiftClick());
    }
    private void execute(Player player, boolean shift) {
        try {
            ItemStack item = creator.createItem(1);
            if (shift) item.setAmount(item.getMaxStackSize());
            Items.dropGiveItem(player, item, false);
            player.sendMessage(Component.empty()
                    .append(Component.text("Item "))
                    .append(Component.text(key())
                            .color(NamedTextColor.YELLOW)
                            .hoverEvent(HoverEvent.showText(Component.text("Click to copy...")))
                            .clickEvent(ClickEvent.copyToClipboard(key()))
                    )
                    .append(Component.text(" gived!"))
            );
        } catch (Exception e) {
            lime.logStackTrace(e);
        }
    }
    private void craftQuery(ViewContainer container, EntityHuman human) {
        CraftQuery.openQuery(container, human, Checker.createRawCheck(creator));
    }

    public boolean isSearch(String search) {
        return search.startsWith("!")
                ? Regex.compareRegex(key(), search.substring(1))
                : Regex.filterRegex(searchList, String::toLowerCase, search.toLowerCase(), true).findAny().isPresent();
    }

    protected ItemStack createWithoutThrow(List<String> searchList) {
        searchList.add(creator.getKey());
        try {
            ItemStack item = creator.createItem();
            ItemMeta meta = item.getItemMeta();

            List<Component> components = new ArrayList<>();
            Optional.ofNullable(meta.lore()).ifPresent(components::addAll);

            if (!components.isEmpty()) components.add(Component.text(" - - - - - - - ").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));

            Optional.ofNullable(meta.displayName()).map(ChatHelper::getText).ifPresent(searchList::add);
            searchList.add(components.stream().map(ChatHelper::getText).collect(Collectors.joining("\n")));
            searchList.add(item.getType().name());
            if (meta.hasCustomModelData()) searchList.add(String.valueOf(meta.getCustomModelData()));

            List.of(
                    Component.text("Дополнительная информация:"),
                    Component.text("  Key: " + creator.getKey()),
                    Component.text("  Type: " + item.getType().name()),
                    Component.text("  ID: ")
                            .append(meta.hasCustomModelData()
                                    ? Component.text(meta.getCustomModelData())
                                    : Component.text("Не установлено").color(NamedTextColor.RED)
                            )
            ).forEach(component -> components.add(component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE).colorIfAbsent(NamedTextColor.YELLOW)));

            meta.lore(components);

            item.setItemMeta(meta);
            return item;
        }
        catch (Exception e) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(creator.getKey()));
            List<Component> components = new ArrayList<>();
            components.add(Component.text("ERROR CREATE ITEM: " + e.getMessage())
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)
            );

            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                components.add(Component.text(" " + stackTraceElement.toString())
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                );
            }

            meta.lore(components);
            item.setItemMeta(meta);
            item.setAmount(1);
            return item;
        }
    }
}

