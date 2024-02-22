package org.lime.gp.module.timeline;

import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;

import java.util.HashMap;
import java.util.Map;

public class ProtoChunkUtils {
    public static void forceUpdateChunks(WorldServer world, Map<ChunkCoordIntPair, ProtoChunk> map) {
        HashMap<Chunk, ProtoChunk> chunks = new HashMap<>();
        map.forEach((pos, proto) -> {
            Chunk chunk = world.getChunk(pos.x, pos.z);
            chunk.clearAllBlockEntities();
            chunks.put(world.getChunk(pos.x, pos.z), proto);
        });
        chunks.entrySet()
                .parallelStream()
                .forEach(v -> forceUpdateChunk(v.getKey(), v.getValue(), false));
        chunks.forEach((chunk, proto) -> chunk.getBlockEntitiesPos().forEach(chunk::getBlockEntity));
    }
    public static void forceUpdateChunk(WorldServer world, ChunkCoordIntPair pos, ProtoChunk proto) {
        forceUpdateChunk(world.getChunk(pos.x, pos.z), proto, true);
    }
    private static void forceUpdateChunk(Chunk chunk, ProtoChunk proto, boolean flush) {
        if (flush) chunk.clearAllBlockEntities();
        forceUpdateSections(proto.getSections(), chunk.getSections());
        proto.getBlockEntityNbts()
                .forEach((pos, entity) -> {
                    chunk.setBlockEntityNbt(entity);
                    if (flush) chunk.getBlockEntity(pos);
                });
    }
    private static void forceUpdateSections(ChunkSection[] from, ChunkSection[] to) {
        int sections = Math.min(from.length, to.length);
        for (int y = 0; y < sections; y++)
            forceUpdateSection(from[y], to[y]);
    }
    private static void forceUpdateSection(ChunkSection from, ChunkSection to) {
        from.acquire();
        to.acquire();
        try {
            for (int _x = 0; _x < 16; _x++)
                for (int _y = 0; _y < 16; _y++)
                    for (int _z = 0; _z < 16; _z++)
                        to.setBlockState(_x, _y, _z, from.getBlockState(_x,_y,_z), false);
        }
        finally {
            from.release();
            to.release();
        }
    }
}

