package org.lime.gp.block.component.display.event;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.lime.gp.lime;

import net.minecraft.core.SectionPosition;
import net.minecraft.world.level.ChunkCoordIntPair;

public class ChunkCoordCache implements Listener {
    public static org.lime.core.element create() {
        return org.lime.core.element.create(ChunkCoordCache.class)
                .withInstance()
                .withInit(ChunkCoordCache::init);
    }

    private static final HashMap<UUID, ChunkCoordIntPair> playerChunks = new HashMap<>();

    private static void init() {
        lime.repeatTicks(ChunkCoordCache::update, 1);
    }
    private static void update() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Location location = player.getLocation();
            ChunkCoordIntPair coord = new ChunkCoordIntPair(
                SectionPosition.blockToSectionCoord(location.getBlockX()),
                SectionPosition.blockToSectionCoord(location.getBlockZ())
            );
            ChunkCoordIntPair old = playerChunks.put(player.getUniqueId(), coord);
            if (coord.equals(old)) return;
            PlayerChunkMoveEvent.execute(player, coord);
        });
    }

    public static Optional<ChunkCoordIntPair> getCoord(UUID uuid) {
        return Optional.ofNullable(playerChunks.get(uuid));
    }

    @EventHandler(ignoreCancelled = true) public static void on(PlayerQuitEvent e) {
        playerChunks.remove(e.getPlayer().getUniqueId());
    }
    @EventHandler(ignoreCancelled = true) public static void on(PlayerTeleportEvent e) {
        if (e.getFrom().getWorld() != e.getTo().getWorld())
            playerChunks.remove(e.getPlayer().getUniqueId());
    }
}