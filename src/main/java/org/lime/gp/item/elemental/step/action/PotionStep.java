package org.lime.gp.item.elemental.step.action;

import com.mojang.math.Transformation;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.system.utils.MathUtils;

public record PotionStep(PotionEffect effect, Vector radius, boolean self) implements IStep {
    @Override public void execute(Player player, DataContext context, Transformation location) {
        if (radius.isZero()) {
            if (!self) return;
            player.addPotionEffect(effect);
            return;
        }
        World world = player.getWorld();
        world.getNearbyPlayers(MathUtils.convert(location.getTranslation()).toLocation(world), radius.getX(), radius.getY(), radius.getZ()).forEach(other -> {
            if (!self && player == other) return;
            other.addPotionEffect(effect);
        });
    }
}
