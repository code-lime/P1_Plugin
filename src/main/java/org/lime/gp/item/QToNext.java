package org.lime.gp.item;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.lime.core;
import org.lime.gp.sound.Sounds;

public class QToNext implements Listener {
    public static core.element create() {
        return core.element.create(QToNext.class)
                .withInstance();
    }
    @EventHandler public static void on(InventoryClickEvent e) {
        switch (e.getClick()) {
            case DROP: {
                ItemStack item = e.getCurrentItem();
                if (item == null) return;
                Items.getOptional(Settings.NextSetting.class, item)
                        .ifPresent(_v -> Items.getOptional(Settings.QToNextSetting.class, item).ifPresent(qtn -> {
                                    Items.getItemCreator(_v.next)
                                    .map(v -> v instanceof Items.ItemCreator c ? c : null)
                                    .map(next -> next.apply(item))
                                    .ifPresent(v -> {
                                        Sounds.playSound(qtn.sound, e.getWhoClicked().getLocation());
                                        e.setCurrentItem(item);
                                        e.setCancelled(true);
                                    });
                        }));
                break;
            }
        }
    }
}
