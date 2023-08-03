package org.lime.gp.item.elemental.step.action;

import com.destroystokyo.paper.ParticleBuilder;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.gp.item.elemental.step.IStep;

public record ParticleStep(ParticleBuilder particle, Vector delta, boolean self) implements IStep {
    @Override public void execute(Player player, Vector position) {
        Location location = new Location(player.getWorld(), position.getX(), position.getY(), position.getZ());
        ParticleBuilder particle = this.particle.source(player).location(location);
        if (delta.isZero()) {
            if (!self) return;
            particle.receivers(player).spawn();
            return;
        }
        particle.receivers(location.getNearbyPlayers(delta.getX(), delta.getY(), delta.getZ())).spawn();
    }
}
