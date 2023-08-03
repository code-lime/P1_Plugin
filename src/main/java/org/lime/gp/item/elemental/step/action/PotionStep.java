package org.lime.gp.item.elemental.step.action;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.lime.gp.item.elemental.step.IStep;

public record PotionStep(PotionEffect effect, Vector delta, boolean self) implements IStep {
    @Override public void execute(Player player, Vector position) {
        if (position.isZero()) {
            if (!self) return;
            player.addPotionEffect(effect);
            return;
        }
        player.getWorld().getNearbyPlayers(player.getLocation(), delta.getX(), delta.getY(), delta.getZ()).forEach(other -> {
            if (!self && player == other) return;
            other.addPotionEffect(effect);
        });
    }
}
