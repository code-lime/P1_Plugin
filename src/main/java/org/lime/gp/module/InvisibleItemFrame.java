package org.lime.gp.module;

import com.google.gson.JsonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.chat.Apply;
import org.lime.gp.extension.JManager;
import org.lime.gp.lime;
import org.lime.gp.player.menu.MenuCreator;

import java.util.UUID;

public class InvisibleItemFrame implements Listener {
    public static core.element create() {
        return core.element.create(InvisibleItemFrame.class)
                .withInstance()
                .withInit(InvisibleItemFrame::init);
    }
    public static void init() {
        AnyEvent.addEvent("item_frame.invisible", AnyEvent.type.other, buidler -> buidler.createParam(UUID::fromString, "[uuid]"), (p, uuid) -> {
            if (!(Bukkit.getEntity(uuid) instanceof ItemFrame itemFrame)) return;
            JManager.set(itemFrame.getPersistentDataContainer(), "invisible", JsonNull.INSTANCE);
            boolean hasItem = !itemFrame.getItem().getType().isAir();
            itemFrame.setVisible(!hasItem);
            itemFrame.setGlowing(!hasItem);
        });
        lime.repeat(InvisibleItemFrame::update, 0.5);
    }
    public static void update() {
        Bukkit.getWorlds().forEach(w -> w.getEntitiesByClass(ItemFrame.class).forEach(e -> {
            if (!JManager.has(e.getPersistentDataContainer(), "invisible")) return;
            boolean hasItem = !e.getItem().getType().isAir();
            if (hasItem == e.isVisible()) {
                e.setVisible(!hasItem);
                e.setGlowing(!hasItem);
            }
        }));
    }
    @EventHandler public static void on(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!(e.getRightClicked() instanceof ItemFrame itemFrame)) return;
        Player player = e.getPlayer();
        if (!player.isSneaking()) return;
        e.setCancelled(true);
        MenuCreator.show(player, "item_frame", Apply.of().add("frame_uuid", itemFrame.getUniqueId().toString()).add("frame_invisible", JManager.has(itemFrame.getPersistentDataContainer(), "invisible") ? "true" : "false"));
    }
}



