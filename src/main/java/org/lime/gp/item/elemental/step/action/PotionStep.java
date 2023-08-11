package org.lime.gp.item.elemental.step.action;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.lime.display.transform.LocalLocation;
import org.lime.gp.item.elemental.step.IStep;

public record PotionStep(PotionEffect effect, Vector radius, boolean self) implements IStep {
    @Override public void execute(Player player, LocalLocation location) {
        if (location.isZero()) {
            if (!self) return;
            player.addPotionEffect(effect);
            return;
        }
        World world = player.getWorld();
        world.getNearbyPlayers(location.position().toLocation(world), radius.getX(), radius.getY(), radius.getZ()).forEach(other -> {
            if (!self && player == other) return;
            other.addPotionEffect(effect);
        });
    }
}
