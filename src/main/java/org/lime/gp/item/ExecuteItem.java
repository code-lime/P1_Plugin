package org.lime.gp.item;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Func2;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast1;

import java.util.ArrayList;
import java.util.List;

public class ExecuteItem implements Listener {
    public static CoreElement create() {
        return CoreElement.create(ExecuteItem.class)
                .withInstance()
                .withInit(ExecuteItem::init);
    }
    
    public static final List<Func2<ItemStack, Toast1<ItemMeta>, Boolean>> execute = new ArrayList<>();

    private static boolean forceItemUpdate = false;

    public static void init() {
        execute.add(ExecuteItem::onExecute);

        AnyEvent.addEvent("force.item.update", AnyEvent.type.owner_console, v -> v.createParam("enable", "disable"), (p, v) -> {
            switch (v) {
                case "enable" -> {
                    forceItemUpdate = true;
                    lime.logOP("Enabled forceItemUpdate");
                }
                case "disable" -> {
                    forceItemUpdate = false;
                    lime.logOP("Disabled forceItemUpdate");
                }
            }
        });
    }
    
    public static void replace(ItemStack original, ItemStack item) {
        /*lime.logOP(Component.text("REPLACE ")
                .append(Component.text(original.getType().name())
                        .hoverEvent(HoverEvent.showText(Component.text(original.toString())))
                        .clickEvent(ClickEvent.copyToClipboard(original.toString()))
                        .color(NamedTextColor.AQUA)
                )
                .append(Component.text(" TO "))
                .append(Component.text(item.getType().name())
                        .hoverEvent(HoverEvent.showText(Component.text(item.toString())))
                        .clickEvent(ClickEvent.copyToClipboard(item.toString()))
                        .color(NamedTextColor.AQUA)
                )
        );*/
        original.setType(item.getType());
        original.setItemMeta(item.getItemMeta());
        original.setAmount(item.getAmount());
        /*lime.logOP(Component.text("RESULT: ")
                .append(Component.text(original.getType().name())
                        .hoverEvent(HoverEvent.showText(Component.text(original.toString())))
                        .clickEvent(ClickEvent.copyToClipboard(original.toString()))
                        .color(NamedTextColor.GOLD)
                )
        );*/
    }

    private static boolean onExecute(ItemStack item, Toast1<ItemMeta> metaBox) {
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
        Toast1<ItemMeta> metaBox = Toast.of(meta);
        boolean save = false;
        for (Func2<ItemStack, Toast1<ItemMeta>, Boolean> func : execute)
            save = func.invoke(item, metaBox) || save;
        
        if (!save) return false;
        item.setItemMeta(metaBox.val0);
        return true;
    }
    @EventHandler public static void on(InventoryOpenEvent e) {
        if (!forceItemUpdate) return;
        e.getInventory().forEach(ExecuteItem::executeItem);
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
