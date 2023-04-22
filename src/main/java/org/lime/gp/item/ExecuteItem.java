package org.lime.gp.item;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.system;
import org.lime.gp.item.data.ItemCreator;
import org.lime.system.*;
public class ExecuteItem implements Listener {
    public static org.lime.core.element create() {
        return org.lime.core.element.create(ExecuteItem.class)
                .withInstance()
                .withInit(ExecuteItem::init);
    }
    
    public static final List<Func2<ItemStack, system.Toast1<ItemMeta>, Boolean>> execute = new ArrayList<>();

    public static void init() {
        execute.add(ExecuteItem::onExecute);
    }
    
    public static void replace(ItemStack original, ItemStack item) {
        original.setType(item.getType());
        original.setItemMeta(item.getItemMeta());
        original.setAmount(item.getAmount());
    }

    private static boolean onExecute(ItemStack item, system.Toast1<ItemMeta> metaBox) {
        return Items.getIDByItem(item).map(id -> {
            Material mat = Items.creatorMaterials.get(id);
            if (mat == null || mat.equals(item.getType())) return false;
            if (!(Items.creators.getOrDefault(id, null) instanceof ItemCreator creator) || !creator.updateReplace()) return false;
            replace(item, creator.createItem(item.getAmount()));
            metaBox.val0 = item.getItemMeta();
            return true;
        }).orElse(false);
    }

    private static boolean executeItem(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        system.Toast1<ItemMeta> metaBox = system.toast(meta);
        boolean save = false;
        for (Func2<ItemStack, system.Toast1<ItemMeta>, Boolean> func : execute)
            save = func.invoke(item, metaBox) || save;
        
        if (!save) return false;
        item.setItemMeta(metaBox.val0);
        return true;
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
