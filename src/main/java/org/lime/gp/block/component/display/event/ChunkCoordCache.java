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
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.lime;
import org.lime.gp.block.component.display.BlockDisplay;

import net.minecraft.core.BlockPosition;
import org.lime.plugin.CoreElement;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast1;

public class ChunkCoordCache implements Listener {
    public static CoreElement create() {
        return CoreElement.create(ChunkCoordCache.class)
                .withInstance()
                .withInit(ChunkCoordCache::init);
    }

    public record Cache(int x, int z, World world, int counter) {
        public static int distance(Cache coord1, Cache coord2) {
            return Math.max(Math.abs(coord1.x - coord2.x), Math.abs(coord1.z - coord2.z));
        }
        public int distance(Cache coord) {
            return distance(this, coord);
        }
        public static Cache of(Location location, int counter) {
            int x = location.getBlockX();
            int z = location.getBlockZ();
            return new Cache(x / BlockDisplay.CHUNK_SIZE, z / BlockDisplay.CHUNK_SIZE, location.getWorld(), counter);
        }
        public static Cache of(BlockPosition pos, World world, int counter) {
            int x = pos.getX();
            int z = pos.getZ();
            return new Cache(x / BlockDisplay.CHUNK_SIZE, z / BlockDisplay.CHUNK_SIZE, world, counter);
        }
    }

    private static final ConcurrentHashMap<UUID, Cache> playerChunks = new ConcurrentHashMap<>();

    private static void init() {
        lime.repeatTicks(ChunkCoordCache::update, 1);
    }
    private static void update() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (!ExtMethods.isPlayerLoaded(player)) {
                playerChunks.remove(player.getUniqueId());
                return;
            }
            Location location = player.getLocation();
            Toast1<Boolean> changed = Toast.of(false);
            Cache cache = playerChunks.compute(player.getUniqueId(), (k,v) -> {
                int counter = v == null ? 0 : Math.min(5, v.counter() + 1);
                Cache coord = Cache.of(location, counter);
                if (coord.equals(v)) return coord;
                changed.val0 = true;
                return coord;
            });
            if (changed.val0)
                PlayerChunkMoveEvent.execute(player, cache);
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