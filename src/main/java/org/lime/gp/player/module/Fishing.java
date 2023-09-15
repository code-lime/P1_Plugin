package org.lime.gp.player.module;

import org.bukkit.entity.FishHook;
import org.bukkit.inventory.EquipmentSlot;
import org.lime.core;
import org.lime.gp.item.settings.list.FishingRodSetting;
import org.lime.plugin.CoreElement;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.lime.gp.item.Items;
import org.lime.gp.item.Vest;
import org.lime.gp.item.settings.list.BaitSetting;

import java.util.ArrayList;
import java.util.List;

public class Fishing implements Listener {
    public static CoreElement create() {
        return CoreElement.create(Fishing.class)
                .withInstance();
    }
    @EventHandler(priority = EventPriority.HIGH) public static void on(PlayerFishEvent e) {
        switch (e.getState()) {
            case FISHING -> {
                List<String> tags = new ArrayList<>();
                FishHook hook = e.getHook();
                if (Vest.tryRemoveSingleItem(e.getPlayer(), item -> Items.getOptional(BaitSetting.class, item).map(bait -> {
                    tags.addAll(bait.tags);
                    return true;
                }).orElse(false))) {
                    hook.getScoreboardTags().addAll(tags);
                }
                if (e.getHand() != null)
                    Items.getOptional(FishingRodSetting.class, e.getPlayer().getInventory().getItem(e.getHand()))
                            .ifPresent(v -> hook.setVelocity(hook.getVelocity().multiply(v.launchMultiply)));
            }
            case CAUGHT_FISH -> {
                Entity ent = e.getCaught();
                if (ent == null) return;
                if (!(ent instanceof Item)) return;
                ItemStack item = ((Item)ent).getItemStack();
                if (Items.hasIDByItem(item)) return;
                EntityType type;
                switch (item.getType()) {
                    case COD -> type = EntityType.COD;
                    case SALMON -> type = EntityType.SALMON;
                    case TROPICAL_FISH -> type = EntityType.TROPICAL_FISH;
                    case PUFFERFISH -> type = EntityType.PUFFERFISH;
                    default -> {
                        return;
                    }
                }
                Location location = ent.getLocation();
                Vector from = location.toVector();
                Vector to = e.getPlayer().getEyeLocation().toVector().add(new Vector(0, 3, 0));
                Vector velocity = to.subtract(from).normalize().multiply(1.5);
                item.setAmount(0);
                location.getWorld().spawnEntity(location, type, CreatureSpawnEvent.SpawnReason.CUSTOM, v -> v.setVelocity(velocity));
            }
        }
    }
}
