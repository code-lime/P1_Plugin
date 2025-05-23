package org.lime.gp.block.component.display;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.state.IBlockData;
import org.lime.gp.block.component.display.block.IBlock;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CacheBlockDisplay {
    public interface IConcurrentCacheInfo extends ICacheInfo
    {
        @Override default boolean isConcurrent() { return true; }
    }
    public interface ICacheInfo {
        IBlock cache(UUID uuid);
        default boolean isConcurrent() { return false; }

        static IConcurrentCacheInfo of(ConcurrentHashMap<UUID, IBlock> cache) { return cache::get; }
        static ICacheInfo of(IBlock data) { return uuid -> data; }
        static ICacheInfo of(IBlockData data) { return of(IBlock.of(data)); }
    }
    private static final ConcurrentHashMap<Toast2<BlockPosition, UUID>, ICacheInfo> cacheBlocks = new ConcurrentHashMap<>();

    @SuppressWarnings("all")
    public static boolean trySetCacheBlock(TileEntityLimeSkull skull, ICacheInfo info) {
        return trySetCacheBlock(skull.getBlockPos(), skull.getLevel().getMinecraftWorld().uuid, info);
    }
    public static boolean trySetCacheBlock(BlockPosition position, UUID worldUUID, ICacheInfo info) {
        return cacheBlocks.putIfAbsent(Toast.of(position, worldUUID), info) != info;
    }

    @SuppressWarnings("all")
    public static boolean replaceCacheBlock(TileEntityLimeSkull skull, ICacheInfo info) {
        return replaceCacheBlock(skull.getBlockPos(), skull.getLevel().getMinecraftWorld().uuid, info);
    }
    public static boolean replaceCacheBlock(BlockPosition position, UUID worldUUID, ICacheInfo info) {
        return cacheBlocks.put(Toast.of(position, worldUUID), info) != info;
    }

    @SuppressWarnings("all")
    public static void resetCacheBlock(TileEntityLimeSkull skull) {
        resetCacheBlock(skull.getBlockPos(), skull.getLevel().getMinecraftWorld().uuid);
    }
    public static void resetCacheBlock(BlockPosition position, UUID worldUUID) {
        cacheBlocks.remove(Toast.of(position, worldUUID));
    }
    public static Optional<ICacheInfo> getCacheBlock(BlockPosition position, UUID worldUUID) {
        return Optional.ofNullable(cacheBlocks.get(Toast.of(position, worldUUID)));
    }
    
    @SuppressWarnings("all")
    public static boolean isConcurrent(TileEntityLimeSkull skull) {
        return isConcurrent(skull.getBlockPos(), skull.getLevel().getMinecraftWorld().uuid);
    }
    public static boolean isConcurrent(BlockPosition position, UUID worldUUID) {
        ICacheInfo info = cacheBlocks.get(Toast.of(position, worldUUID));
        return info != null && info.isConcurrent();
    }

    public static void reset() {
        cacheBlocks.clear();
    }
}
