package org.lime.gp.player.module;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.player.menu.MenuCreator;

import java.util.UUID;

public class Interact implements Listener {
    public static CoreElement create() {
        return CoreElement.create(Interact.class)
                .withInstance();
    }
    @EventHandler(ignoreCancelled = true) public static void on(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player clicked)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        UUID other_uuid = clicked.getUniqueId();
        UserRow.getBy(other_uuid).ifPresent(other_row -> {
            event.setCancelled(true);
            MenuCreator.show(
                    event.getPlayer(),
                    Death.isDamageLay(clicked.getUniqueId()) ? "phone.user.die" : "phone.user",
                    Apply.of().add("other_id", String.valueOf(other_row.id)).add("other_uuid", other_uuid.toString())
            );
        });
    }
}
