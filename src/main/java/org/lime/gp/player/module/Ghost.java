package org.lime.gp.player.module;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.lime.gp.lime;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import org.lime.plugin.CoreElement;

public class Ghost implements Listener {
    public static CoreElement create() {
        return CoreElement.create(Ghost.class)
                .withInit(Ghost::init)
                .withInstance();
    }

    public static boolean isGhost(Player player) {
        return player.getScoreboardTags().contains("ghost");
    }
    public static Optional<UUID> getGhostTarget(Player player) {
        Optional<UUID> result = Optional.empty();
        boolean show = false;
        for (String tag : player.getScoreboardTags()) {
            if (tag.equals("ghost")) {
                show = true;
                continue;
            }
            if (!tag.startsWith("ghost-")) continue;
            result = Optional.of(UUID.fromString(tag.substring(6)));
        }
        return show ? result : Optional.empty();
    }
    
    private static void init() {
        lime.repeat(Ghost::update, 1);
    }
    private static void update() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Set<String> tags = player.getScoreboardTags();
            if (!tags.contains("ghost")) return;
            UUID targetUUID = null;
            for (String tag : tags) {
                if (!tag.startsWith("ghost-")) continue;
                targetUUID = UUID.fromString(tag.substring(6));
                break;
            }
            if (targetUUID == null) {
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.teleport(Login.getMainLocation());
                }
                tags.remove("ghost");
                return;
            }
            Player target = Bukkit.getPlayer(targetUUID);
            if (player.getGameMode() != GameMode.SPECTATOR) {
                player.setGameMode(GameMode.SPECTATOR);
            }
            if (target == null) {
                player.teleport(new Location(lime.MainWorld, 0, 10000, 0));
            } else if (player.getSpectatorTarget() != target) {
                player.setSpectatorTarget(target);
            }
        });
    }

    @EventHandler private static void on(PlayerStartSpectatingEntityEvent event) {
        Player player = event.getPlayer();
        if (!isGhost(player)) return;
        getGhostTarget(player).ifPresentOrElse(targetUUID -> {
            if (event.getNewSpectatorTarget().getUniqueId().equals(targetUUID)) return;
            event.setCancelled(true);
        }, () -> {
            event.setCancelled(true);
        });
    }
    @EventHandler private static void on(PlayerStopSpectatingEntityEvent event) {
        /*
        Player player = event.getPlayer();
        if (!isGhost(player)) return;
        lime.nextTick(() -> {
            getGhostTarget(player).ifPresent(targetUUID -> {
                Entity target = player.getSpectatorTarget();
                if (target != null && target.getUniqueId().equals(targetUUID)) return;
                player.teleport(new Location(lime.MainWorld, 0, 10000, 0));
            });
        });
        */
    }
    @EventHandler private static void on(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (event.getCause() != TeleportCause.SPECTATE || !isGhost(player)) return;
        getGhostTarget(player).ifPresentOrElse(targetUUID -> {
            for (Player target : event.getTo().getNearbyPlayers(3)) {
                if (target.getUniqueId().equals(targetUUID))
                    return;
            }
            event.setCancelled(true);
        }, () -> {
            event.setCancelled(true);
        });
    }
}
