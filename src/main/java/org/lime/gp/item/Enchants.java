package org.lime.gp.item;

import com.google.gson.JsonArray;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.core;
import org.lime.system;

import java.util.*;

public class Enchants implements Listener {
    public static core.element create() {
        return core.element.create(Enchants.class)
                .withInstance()
                .<JsonArray>addConfig("allow_enchants", v -> v
                        .withDefault(system.json.array()
                                .add("vanishing_curse") //Проклятие утраты
                                .add("binding_curse") //Проклятие несъёмности
                                .add("blast_protection") //Взрывоустойчивость
                                .add("lure") //Приманка
                                .add("fortune") //Удача
                                .add("efficiency") //Эффективность
                                .add("silk_touch") //Шёлковое касание
                                .add("projectile_protection") //Защита от снарядов
                                .add("luck_of_the_sea") //Везучий рыбак
                                .add("unbreaking") //Прочность
                                .build()
                        )
                        .withInvoke(Enchants::config)
                );
    }

    private static final List<Enchantment> allowEnchantments = new ArrayList<>();
    public static void config(JsonArray json) {
        List<Enchantment> allowEnchantments = new ArrayList<>();
        json.forEach(item -> allowEnchantments.add(Enchantment.getByKey(NamespacedKey.minecraft(item.getAsString()))));
        Enchants.allowEnchantments.clear();
        Enchants.allowEnchantments.addAll(allowEnchantments);
    }

    @EventHandler public static void on(PrepareItemEnchantEvent e) {
        EnchantmentOffer[] offers = e.getOffers();

        int length = offers.length;
        for (int i = 0; i < length; i++) {
            EnchantmentOffer offer = offers[i];
            if (offer == null || !allowEnchantments.contains(offer.getEnchantment())) continue;
            offers[i] = null;
        }
    }

    public static void replace(ItemStack original, ItemStack item) {
        original.setType(item.getType());
        original.setItemMeta(item.getItemMeta());
        original.setAmount(item.getAmount());
    }
    private static boolean removeEnchants(system.Func0<Collection<Enchantment>> enchantments, system.Action1<Enchantment> remove, system.Action1<Enchantment> append, boolean not_empty) {
        boolean edit = false;
        for (Enchantment enchantment : enchantments.invoke()) {
            if (allowEnchantments.contains(enchantment)) continue;
            remove.invoke(enchantment);
            edit = true;
        }
        if (not_empty && enchantments.invoke().isEmpty()) {
            append.invoke(system.rand_is(0.25) ? system.rand(allowEnchantments) : system.rand(Enchantment.BINDING_CURSE, Enchantment.VANISHING_CURSE));
            edit = true;
        }
        return edit;
    }
    private static boolean executeItem(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        ItemMeta new_meta = Items.getIDByItem(item).map(id -> {
            Material mat = Items.creatorMaterials.getOrDefault(id, null);
            if (mat != null && !item.getType().equals(mat) && Items.creators.getOrDefault(id, null) instanceof Items.ItemCreator creator && creator.updateReplace()) {
                replace(item, creator.createItem(item.getAmount()));
                return item.getItemMeta();
            }
            return null;
        }).orElse(meta);

        boolean save = new_meta != meta;
        save = removeEnchants(() -> new_meta.getEnchants().keySet(), new_meta::removeEnchant, e -> new_meta.addEnchant(e, 1, true), false) || save;
        if (meta instanceof EnchantmentStorageMeta esm)
            save = removeEnchants(() -> esm.getStoredEnchants().keySet(), esm::removeStoredEnchant, e -> esm.addStoredEnchant(e, 1, true), true) || save;

        if (save) {
            item.setItemMeta(meta);
            return true;
        }
        return false;
    }
    @EventHandler public static void on(InventoryClickEvent e) {
        if (e.getWhoClicked().getInventory().equals(e.getClickedInventory())) {
            executeItem(e.getCurrentItem());
            executeItem(e.getCursor());
        }
    }
    @EventHandler public static void on(EntityPickupItemEvent e) { executeItem(e.getItem().getItemStack()); }
    @EventHandler public static void on(InventoryPickupItemEvent e) { executeItem(e.getItem().getItemStack()); }
    @EventHandler public static void on(PlayerDropItemEvent e) { executeItem(e.getItemDrop().getItemStack()); }
    @EventHandler public static void on(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof LivingEntity livingEntity)) return;
        EntityEquipment equipment = livingEntity.getEquipment();
        if (equipment == null) return;
        ItemStack[] items = equipment.getArmorContents();
        boolean save = false;
        for (ItemStack item : items) save = executeItem(item) || save;
        if (save) equipment.setArmorContents(items);
    }
    @EventHandler public static void on(PlayerJoinEvent e) {
        PlayerInventory inventory = e.getPlayer().getInventory();
        ItemStack[] items = inventory.getContents();
        boolean save = false;
        for (ItemStack item : items) save = executeItem(item) || save;
        if (save) inventory.setContents(items);
    }
}
