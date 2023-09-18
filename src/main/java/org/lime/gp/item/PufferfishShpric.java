package org.lime.gp.item;

import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPufferFish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.PlayerInventory;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.lime;

public class PufferfishShpric implements Listener {
    public static CoreElement create() {
        return CoreElement.create(PufferfishShpric.class)
                .withInstance();
    }

    public static final String SHPRIC = "Hospital.Tool.Shpric";
    public static final String SHPRIC_PUFFERFISH = "Resource.Tool.Shpric_Pufferfish";

    @EventHandler(ignoreCancelled = true) public static void on(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof CraftPufferFish pufferFish && e.getPlayer() instanceof CraftPlayer player) {
            PlayerInventory inventory = e.getPlayer().getInventory();
            if (Items.getGlobalKeyByItem(inventory.getItemInMainHand()).filter(SHPRIC::equals).isEmpty()) return;
            pufferFish.getHandle().kill();
            Items.createItem(SHPRIC_PUFFERFISH)
                    .ifPresentOrElse(inventory::setItemInMainHand, () -> lime.logOP("ITEM '" + SHPRIC_PUFFERFISH + "' NOT FOUNDED!"));
        }
    }
}
