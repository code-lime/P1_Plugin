package org.lime.gp.item;

import com.google.gson.JsonArray;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.system;

import java.util.*;

public class Enchants implements Listener {
    public static CoreElement create() {
        return CoreElement.create(Enchants.class)
                .withInstance()
                .withInit(Enchants::init)
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
    private static void config(JsonArray json) {
        List<Enchantment> allowEnchantments = new ArrayList<>();
        json.forEach(item -> allowEnchantments.add(Enchantment.getByKey(NamespacedKey.minecraft(item.getAsString()))));
        Enchants.allowEnchantments.clear();
        Enchants.allowEnchantments.addAll(allowEnchantments);
    }
    private static void init() {
        ExecuteItem.execute.add(Enchants::onExecute);
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
    public static boolean onExecute(ItemStack item, system.Toast1<ItemMeta> metaBox) {
        ItemMeta meta = metaBox.val0;
        boolean save = removeEnchants(() -> meta.getEnchants().keySet(), meta::removeEnchant, e -> meta.addEnchant(e, 1, true), false);
        if (meta instanceof EnchantmentStorageMeta esm)
            save = removeEnchants(() -> esm.getStoredEnchants().keySet(), esm::removeStoredEnchant, e -> esm.addStoredEnchant(e, 1, true), true) || save;

        return save;
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

}
