package org.lime.gp.module;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.lime.core;
import org.lime.gp.lime;

public class FixCursorSlot implements Listener {
    public static core.element create() {
        return core.element.create(FixCursorSlot.class)
                .withInstance();
    }

    @EventHandler public static void on(InventoryCloseEvent e) {
        HumanEntity player = e.getPlayer();
        ItemStack cursor = player.getItemOnCursor();
        if (!cursor.getType().isAir()) return;
        player.setItemOnCursor(cursor);
        ((Player)player).updateInventory();
    }
    @EventHandler public static void on(InventoryOpenEvent e) {
        HumanEntity player = e.getPlayer();
        ItemStack cursor = player.getItemOnCursor();
        if (!cursor.getType().isAir()) return;
        player.setItemOnCursor(cursor);
        ((Player)player).updateInventory();
    }
    @EventHandler public static void on(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        lime.nextTick(player::updateInventory);
    }
}
