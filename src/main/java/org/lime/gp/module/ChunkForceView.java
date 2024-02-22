package org.lime.gp.module;

import io.papermc.paper.chunk.system.RegionizedPlayerChunkLoader;
import net.minecraft.server.level.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.player.api.ViewDistance;

public class ChunkForceView {
    public static void update() {
        Bukkit.getWorlds().forEach(world -> {
            if (!(world instanceof CraftWorld craftWorld)) return;
            WorldServer level = craftWorld.getHandle();
            level.chunkTaskScheduler
                    .chunkHolderManager
                    .getChunkHolders()
                    .forEach(chunk -> ReflectionAccess.playersSentChunkTo_PlayerChunk
                            .get(chunk.vanillaChunkHolder)
                            .clear()
                    );
        });
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (!(player instanceof CraftPlayer craftPlayer)) return;
            RegionizedPlayerChunkLoader.PlayerChunkLoaderData data = craftPlayer.getHandle().chunkLoader;
            ReflectionAccess.sentChunks_RegionizedPlayerChunkLoader_PlayerChunkLoaderData.get(data).clear();
            ReflectionAccess.lastChunkX_PlayerLoaderData_PlayerChunkLoader.set(data, Integer.MIN_VALUE);
            ViewDistance.clearPlayerView(player);
        });
    }
}
