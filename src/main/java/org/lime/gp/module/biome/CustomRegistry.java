package org.lime.gp.module.biome;

import com.google.common.collect.BiMap;
import com.google.common.collect.Iterators;
import net.minecraft.core.Holder;
import net.minecraft.core.IRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.chunk.DataPaletteBlock;
import org.jetbrains.annotations.NotNull;
import org.lime.gp.module.biome.time.weather.BiomeHolder;

import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CustomRegistry<T> implements Registry<T> {
    private final Registry<T> registry;
    private final BiMap<? extends T, Integer> byValueMap;
    private final BiMap<Integer, ? extends T> byIndexMap;
    private CustomRegistry(Registry<T> registry, BiMap<Integer, ? extends T> biMap) {
        this.registry = registry;
        this.byIndexMap = biMap;
        this.byValueMap = biMap.inverse();
    }
    public static <T>CustomRegistry<T> of(Registry<T> registry, BiMap<Integer, ? extends T> map) { return new CustomRegistry<>(registry, map); }

    @Override public int getId(T value) {
        Integer id = byValueMap.get(value);
        return id == null ? registry.getId(value) : id;
    }
    @Override public T byId(int index) {
        T value = byIndexMap.get(index);
        return value == null ? registry.byId(index) : value;
    }
    @Override public int size() { return registry.size() + byIndexMap.size(); }
    @NotNull @Override public Iterator<T> iterator() { return Iterators.concat(registry.iterator(), byIndexMap.values().iterator()); }

    public static CustomRegistry<Holder<BiomeBase>> createBiomeRegistry(IRegistry<BiomeBase> biomeRegistry, BiMap<Integer, BiomeHolder> map) {
        return of(biomeRegistry.asHolderIdMap(), map);
    }
    public static CustomRegistry<Holder<BiomeBase>> createBiomeRegistry(BiMap<Integer, BiomeHolder> map) {
        return createBiomeRegistry(MinecraftServer.getServer().registryAccess().registryOrThrow(Registries.BIOME), map);
    }

    private static ChunkSection createChunkSection(int chunkPos, World level, CustomRegistry<Holder<BiomeBase>> biomeRegistry, Holder<BiomeBase> plains) {
        DataPaletteBlock<IBlockData> states = new DataPaletteBlock<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), DataPaletteBlock.d.SECTION_STATES, level == null || level.chunkPacketBlockController == null ? null : level.chunkPacketBlockController.getPresetBlockStates(level, null, ChunkSection.getBottomBlockY(chunkPos)));
        DataPaletteBlock<Holder<BiomeBase>> biomes = new DataPaletteBlock<>(biomeRegistry, plains, DataPaletteBlock.d.SECTION_BIOMES, null);
        return new ChunkSection(chunkPos, states, biomes);
    }
    public static ChunkSection createChunkSection(int chunkPos, World level, IRegistry<BiomeBase> biomeRegistry, BiMap<Integer, BiomeHolder> map) {
        return createChunkSection(chunkPos, level, createBiomeRegistry(biomeRegistry, map), biomeRegistry.getHolderOrThrow(Biomes.PLAINS));
    }
    public static Stream<ChunkSection> readSections(PacketDataSerializer buffer, World level, CustomRegistry<Holder<BiomeBase>> biomeRegistry, Holder<BiomeBase> plains) {
        return IntStream.rangeClosed(level.getMinSection(), level.getMaxSection())
                .mapToObj(i -> createChunkSection(i, level, biomeRegistry, plains))
                .peek(section -> section.read(buffer));
    }
}













