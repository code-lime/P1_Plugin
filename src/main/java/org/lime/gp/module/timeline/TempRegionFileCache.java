package org.lime.gp.module.timeline;

import com.google.common.collect.Streams;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.chunk.ChunkConverter;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkRegionLoader;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileCache;
import org.apache.commons.io.FileUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.lime;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;
import org.lime.system.utils.IterableUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TempRegionFileCache extends RegionFileCache {
    private final Path folder;
    private final boolean closeDelete;

    private TempRegionFileCache(Path folder, boolean closeDelete) {
        super(folder, false);
        this.closeDelete = closeDelete;
        this.folder = folder;
    }

    private int iterator = -1;
    public int appendRawRegion(InputStream stream) throws IOException {
        iterator++;
        Files.copy(stream, this.folder.resolve("r." + iterator + ".0.mca"), StandardCopyOption.REPLACE_EXISTING);
        return iterator;
    }

    private static class NoneLock extends ReentrantLock {
        public static final NoneLock INSTANCE = new NoneLock();
        public static NoneLock getInstance() { return INSTANCE; }

        private NoneLock() {}

        @Override public void lock() { }
        @Override public void lockInterruptibly() throws InterruptedException { }
        @Override public void unlock() { }
        @Override public boolean tryLock() { return true; }
        @Override public boolean isLocked() { return false; }
        @Override public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException { return true; }
    }

    private static final NamespacedKey KEY_LAST_WORLD_SAVE_TIME = new NamespacedKey(lime._plugin, "lastWorldSaveTime");
    public Map<ChunkCoordIntPair, ProtoChunk> appendAndReadRawRegion(InputStream stream, WorldServer world, int regionX, int regionZ) throws IOException {
        ChunkCoordIntPair min = ChunkCoordIntPair.minFromRegion(regionX, regionZ);
        ChunkCoordIntPair max = ChunkCoordIntPair.maxFromRegion(regionX, regionZ);

        RegionFile region = getRegionFile(appendRawRegion(stream));
        Map<ChunkCoordIntPair, ProtoChunk> chunks = new HashMap<>();
        for (ChunkCoordIntPair pos : IterableUtils.iterable(ChunkCoordIntPair.rangeClosed(min, max))) {
            NBTTagCompound nbt = read(pos, region);
            ProtoChunk chunk = ChunkRegionLoader.read(world, world.getPoiManager(), pos, nbt);
            chunk.persistentDataContainer.set(KEY_LAST_WORLD_SAVE_TIME, PersistentDataType.LONG, ChunkRegionLoader.getLastWorldSaveTime(nbt));
            chunks.put(pos, chunk);
        }
        return chunks;
    }
    public static long getLastWorldSaveTime(ProtoChunk proto) {
        return proto.persistentDataContainer.getOrDefault(KEY_LAST_WORLD_SAVE_TIME, PersistentDataType.LONG, 0L);
    }

    public int getIteratorIndex() { return iterator; }

    private RegionFile getRegionFile(int index) throws IOException {
        return getRegionFile(new ChunkCoordIntPair(index, 0), true, true);
    }

    @Override public synchronized RegionFile getRegionFile(ChunkCoordIntPair chunkcoordintpair, boolean existingOnly, boolean lock) throws IOException {
        RegionFile region = super.getRegionFile(chunkcoordintpair, existingOnly, lock);
        if (region != null) ReflectionAccess.fileLock_RegionFile.set(region, NoneLock.getInstance());
        return region;
    }

    public ProtoChunk readSingleProto(WorldServer world, int index, ChunkCoordIntPair pos) throws IOException {
        RegionFile regionfile = getRegionFile(index);
        return regionfile == null
                ? createEmptyChunk(world, pos)
                : ChunkRegionLoader.read(world, world.getPoiManager(), pos, read(pos, regionfile));
    }
    public List<ProtoChunk> readListProto(WorldServer world, ChunkCoordIntPair pos) throws IOException {
        List<ProtoChunk> chunks = new ArrayList<>();
        for (int i = 0; i < iterator; i++) {
            chunks.add(readSingleProto(world, i, pos));
        }
        return chunks;
    }

    public Stream<Toast2<Integer, Map<ChunkCoordIntPair, ProtoChunk>>> readAllProto(WorldServer world, int regionX, int regionZ) {
        ChunkCoordIntPair min = ChunkCoordIntPair.minFromRegion(regionX, regionZ);
        ChunkCoordIntPair max = ChunkCoordIntPair.maxFromRegion(regionX, regionZ);

        return IntStream.rangeClosed(0, iterator)
                .mapToObj(i -> {
                    try {
                        RegionFile region = getRegionFile(i);
                        Map<ChunkCoordIntPair, ProtoChunk> chunks = new HashMap<>();
                        for (ChunkCoordIntPair pos : IterableUtils.iterable(ChunkCoordIntPair.rangeClosed(min, max))) {
                            ProtoChunk chunk = ChunkRegionLoader.read(world, world.getPoiManager(), pos, read(pos, region));
                            chunks.put(pos, chunk);
                        }
                        return Toast.of(i, chunks);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(e);
                    }
                });
    }

    @Override public synchronized void close() throws IOException {
        super.close();
        if (closeDelete)
            FileUtils.deleteDirectory(this.folder.toFile());
    }

    private static ProtoChunk createEmptyChunk(WorldServer world, ChunkCoordIntPair pos) {
        return new ProtoChunk(pos, ChunkConverter.EMPTY, world, world.registryAccess().registryOrThrow(Registries.BIOME), null);
    }
    public static TempRegionFileCache createInCache(Path cacheDirectory) {
        Path folderPath = cacheDirectory.resolve(UUID.randomUUID().toString());
        File folderFile = folderPath.toFile();
        if (!folderFile.exists())
            folderFile.mkdirs();
        return new TempRegionFileCache(folderPath, true);
    }
    public static TempRegionFileCache overrideFolder(Path folder) {
        File folderFile = folder.toFile();
        if (!folderFile.exists())
            folderFile.mkdirs();
        return new TempRegionFileCache(folder, false);
    }
    public static TempRegionFileCache loadFolder(Path folder) {
        File folderFile = folder.toFile();
        if (!folderFile.exists())
            folderFile.mkdirs();
        TempRegionFileCache regionFileCache = new TempRegionFileCache(folder, false);
        regionFileCache.iterator = Arrays.stream(Objects.requireNonNull(folderFile.list((dir, name) -> name.startsWith("r.") && name.endsWith(".mca"))))
                .flatMap(v -> Optional.ofNullable(getRegionFileCoordinates(Path.of(v))).stream())
                .mapToInt(ChunkCoordIntPair::getRegionX)
                .max()
                .orElse(-1);
        return regionFileCache;
    }
}
















