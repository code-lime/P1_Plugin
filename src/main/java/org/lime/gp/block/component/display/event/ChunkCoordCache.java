package org.lime.gp.block.component.display.event;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.lime.gp.lime;
import org.lime.gp.block.component.display.BlockDisplay;

import net.minecraft.core.BlockPosition;
import org.lime.plugin.CoreElement;

public class ChunkCoordCache implements Listener {
    public static CoreElement create() {
        return CoreElement.create(ChunkCoordCache.class)
                .withInstance()
                .withInit(ChunkCoordCache::init);
    }

    public record Cache(int x, int z, World world) {
        public static int distance(Cache coord1, Cache coord2) {
            return Math.max(Math.abs(coord1.x - coord2.x), Math.abs(coord1.z - coord2.z));
        }
        public int distance(Cache coord) {
            return distance(this, coord);
        }
        public static Cache of(Location location) {
            int x = location.getBlockX();
            int z = location.getBlockZ();
            return new Cache(x / BlockDisplay.CHUNK_SIZE, z / BlockDisplay.CHUNK_SIZE, location.getWorld());
        }
        public static Cache of(BlockPosition pos, World world) {
            int x = pos.getX();
            int z = pos.getZ();
            return new Cache(x / BlockDisplay.CHUNK_SIZE, z / BlockDisplay.CHUNK_SIZE, world);
        }
    }

    private static final ConcurrentHashMap<UUID, Cache> playerChunks = new ConcurrentHashMap<>();

    private static void init() {
        lime.repeatTicks(ChunkCoordCache::update, 1);
    }
    private static void update() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Location location = player.getLocation();
            Cache coord = Cache.of(location);
            Cache old = playerChunks.put(player.getUniqueId(), coord);
            if (coord.equals(old)) return;
            PlayerChunkMoveEvent.execute(player, coord);
        });
    }

    public static Optional<Cache> getCoord(UUID uuid) {
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