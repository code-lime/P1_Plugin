package org.lime.gp.craft;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;

import org.bukkit.Color;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.lime.core;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.*;

import java.util.Optional;
import java.util.regex.Pattern;

public class Locked implements Listener {
    public static core.element create() {
        return core.element.create(Locked.class)
                .withInstance();
    }

    private static final int RED_COLOR_FROM = 0x07;
    private static final int RED_COLOR_TO = 0x08;

    @EventHandler public static void on(CraftItemEvent e) {
        if (isMapAuthor(e.getInventory())) {
            e.setCancelled(true);
            return;
        }
        if (e.getRecipe() instanceof Keyed keyed && !keyed.getKey().getNamespace().equals("lime")) {
            boolean isDye = keyed.getKey().getKey().equals("armor_dye");
            Optional.ofNullable(e.getInventory().getResult())
                    .ifPresent(result -> {
                        ItemMeta meta = result.getItemMeta();
                        boolean edited = false;
                        if (meta instanceof LeatherArmorMeta leatherArmorMeta) {
                            Color color = leatherArmorMeta.getColor();
                            if (color.getRed() == RED_COLOR_FROM) {
                                leatherArmorMeta.setColor(color.setRed(RED_COLOR_TO));
                                edited = true;
                            }
                        }
                        if (edited) result.setItemMeta(meta);
                    });
            for (ItemStack item : e.getInventory().getMatrix())
                if (Items.getIDByItem(item)
                        .map(Items.creators::get)
                        .filter(v -> v instanceof Items.ItemCreator c ? c.getOptional(DyeColorSetting.class).map(_v -> !_v.dyeColor || !isDye).orElse(true) : true)
                        .map(id -> { e.setCancelled(true); return true; })
                        .orElse(false)
                )
                    return;
        }
    }
    @EventHandler public static void on(PrepareItemCraftEvent e) {
        if (isMapAuthor(e.getInventory())) {
            e.getInventory().setResult(null);
            return;
        }
        if (e.getRecipe() instanceof Keyed keyed && !keyed.getKey().getNamespace().equals("lime")) {
            boolean isDye = keyed.getKey().getKey().equals("armor_dye");
            Optional.ofNullable(e.getInventory().getResult())
                    .ifPresent(result -> {
                        ItemMeta meta = result.getItemMeta();
                        boolean edited = false;
                        if (meta instanceof LeatherArmorMeta leatherArmorMeta) {
                            Color color = leatherArmorMeta.getColor();
                            if (color.getRed() == RED_COLOR_FROM) {
                                leatherArmorMeta.setColor(color.setRed(RED_COLOR_TO));
                                edited = true;
                            }
                        }
                        if (edited) result.setItemMeta(meta);
                    });
            for (ItemStack item : e.getInventory().getMatrix())
                if (Items.getIDByItem(item)
                        .map(Items.creators::get)
                        .filter(v -> v instanceof Items.ItemCreator c ? c.getOptional(DyeColorSetting.class).map(_v -> !_v.dyeColor || !isDye).orElse(true) : true)
                        .map(id -> { e.getInventory().setResult(null); return true; })
                        .orElse(false)
                )
                    return;
        }
    }
    @EventHandler public static void on(BlockCookEvent e) {
        if (!e.getRecipe().getKey().getNamespace().equals("lime")
        && Items.getIDByItem(e.getSource())
                .map(Items.creators::get)
                .map(id -> { e.setCancelled(true); return true; })
                .orElse(false)) return;
    }

    private static final TextReplacementConfig REMOVE_FORMATS = TextReplacementConfig.builder()
            .replacement("")
            .match(Pattern.compile("§."))
            .build();
    private static final Component PREFIX = Component.text("§r");
    @EventHandler public static void on(PrepareAnvilEvent e) {
        ItemStack secondItem = e.getInventory().getSecondItem();
        if (secondItem != null && secondItem.getType() != Material.AIR) {
            e.setResult(null);
            return;
        }
        ItemStack result = e.getResult();
        if (result != null && result.getType() != Material.AIR) {
            ItemMeta meta = result.getItemMeta();
            Optional.ofNullable(meta.displayName())
                    .map(v -> PREFIX.append(v.replaceText(REMOVE_FORMATS)))
                    .ifPresent(v -> {
                        meta.displayName(v);
                        result.setItemMeta(meta);
                    });
        }
    }
    @EventHandler public void on(InventoryClickEvent e) {
        if (e.getSlotType() == InventoryType.SlotType.RESULT && e.getClickedInventory() instanceof CartographyInventory inventory)
            for (ItemStack item : inventory)
                if (isMapAuthor(item))
                    e.setCancelled(true);
    }
    private static boolean isMapAuthor(ItemStack item) {
        if (item != null && item.getType() == Material.FILLED_MAP && item.getItemMeta() instanceof MapMeta map)
            return map.hasMapView() && map.getMapView().isLocked();
        return false;
    }
    private static boolean isMapAuthor(CraftingInventory inv) {
        for (ItemStack item : inv.getMatrix())
            if (isMapAuthor(item))
                return true;
        return false;
    }
}

