package org.lime.gp.item;

import net.minecraft.world.inventory.ContainerChest;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftInventoryView;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.extension.inventory.ReadonlyInventory;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.list.*;
import org.lime.gp.sound.Sounds;

public class QToNext implements Listener {
    public static CoreElement create() {
        return CoreElement.create(QToNext.class)
                .withInstance();
    }
    @EventHandler public static void on(InventoryClickEvent e) {
        if (e.getView() instanceof CraftInventoryView view
                && view.getHandle() instanceof ContainerChest containerChest
                && containerChest.getContainer() instanceof ReadonlyInventory) return;
        switch (e.getClick()) {
            case DROP -> {
                ItemStack item = e.getCurrentItem();
                if (item == null) return;
                Items.getOptional(NextSetting.class, item)
                        .ifPresent(_v -> Items.getOptional(QToNextSetting.class, item).ifPresent(qtn -> {
                            _v.next()
                                    .map(v -> v instanceof ItemCreator c ? c : null)
                                    .map(next -> next.apply(item))
                                    .ifPresent(v -> {
                                        Sounds.playSound(qtn.sound, e.getWhoClicked().getLocation());
                                        e.setCurrentItem(item);
                                        e.setCancelled(true);
                                    });
                        }));
            }
        }
    }
}
