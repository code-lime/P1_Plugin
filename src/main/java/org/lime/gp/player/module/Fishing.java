package org.lime.gp.player.module;

import org.lime.core;
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

public class Fishing implements Listener {
    public static core.element create() {
        return core.element.create(Fishing.class)
                .withInstance();
    }
    @EventHandler(priority = EventPriority.HIGH) public static void on(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Entity ent = e.getCaught();
        if (ent == null) return;
        if (!(ent instanceof Item)) return;
        ItemStack item = ((Item)ent).getItemStack();
        EntityType type;
        switch (item.getType()) {
            case COD: type = EntityType.COD; break;
            case SALMON: type = EntityType.SALMON; break;
            case TROPICAL_FISH: type = EntityType.TROPICAL_FISH; break;
            case PUFFERFISH: type = EntityType.PUFFERFISH; break;
            default: return;
        }
        Location location = ent.getLocation();
        Vector from = location.toVector();
        Vector to = e.getPlayer().getEyeLocation().toVector().add(new Vector(0, 3, 0));
        Vector velocity = to.subtract(from).normalize().multiply(1.5);
        item.setAmount(0);
        location.getWorld().spawnEntity(location, type, CreatureSpawnEvent.SpawnReason.CUSTOM, v -> v.setVelocity(velocity));
    }
}
