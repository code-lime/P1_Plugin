package org.lime.gp.player.module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.lime.gp.admin.Administrator;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;

public class Spectator implements Listener {
    public static CoreElement create() {
        return CoreElement.create(Spectator.class)
                .withInit(Spectator::init)
                .withInstance();
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
            boolean seeSpectator = owner.isOp() || owner.getGameMode() == GameMode.CREATIVE || (owner.getGameMode() == GameMode.SPECTATOR && !Ghost.isGhost(owner));
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

    @EventHandler public static void on(PlayerTeleportEvent e) {
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE)
            return;
        if (Administrator.Permissions.ASPECTATORTP.check(e.getPlayer()))
            return;
        e.getPlayer().sendMessage(Component.text("У вас не хватает прав").color(NamedTextColor.RED));
        e.setCancelled(true);
    }
}
