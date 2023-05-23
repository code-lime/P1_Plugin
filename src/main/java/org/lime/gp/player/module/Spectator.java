package org.lime.gp.player.module;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.lime.core;
import org.lime.gp.lime;

public class Spectator {
    public static core.element create() {
        return core.element.create(Spectator.class)
                .withInit(Spectator::init);
    }
    public static void init() {
        lime.repeatTicks(() -> Bukkit.getOnlinePlayers().forEach(player -> {
            Entity target = player.getSpectatorTarget();
            if (target == null) return;
            Location location = target.getLocation();
            if (location.getWorld() != player.getWorld()) {
                for (int i = 0; i < 10; i++) {
                    lime.onceTicks(() -> player.setSpectatorTarget(target), i * 2);
                }
                player.teleport(location, PlayerTeleportEvent.TeleportCause.SPECTATE);
                player.setSpectatorTarget(target);
            } else {
                ((CraftPlayer)player).getHandle().connection.teleport(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(), PlayerTeleportEvent.TeleportCause.SPECTATE);
            }
        }), 1);
        lime.repeat(() -> Bukkit.getOnlinePlayers().forEach(owner -> {
            if (owner.getWorld() == lime.LoginWorld) {
                Bukkit.getOnlinePlayers().forEach(target -> {
                    if (owner.equals(target)) return;
                    if (owner.canSee(target)) owner.hidePlayer(lime._plugin, target);
                });
                return;
            }
            boolean seeSpectator = owner.isOp() || (owner.getGameMode() == GameMode.SPECTATOR && !Ghost.isGhost(owner));
            Bukkit.getOnlinePlayers().forEach(target -> {
                if (owner.equals(target)) return;
                boolean canSee = owner.canSee(target);
                boolean newCanSee = true;
                if (target.getGameMode() == GameMode.SPECTATOR) newCanSee = seeSpectator;
                if (canSee != newCanSee) {
                    if (newCanSee) owner.showPlayer(lime._plugin, target);
                    else owner.hidePlayer(lime._plugin, target);
                }
            });
        }), 1);
    }
}
