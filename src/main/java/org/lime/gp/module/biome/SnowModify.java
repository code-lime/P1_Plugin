package org.lime.gp.module.biome;

import com.destroystokyo.paper.util.maplist.IBlockDataList;
import io.papermc.paper.util.WorldUtil;
import io.papermc.paper.util.math.ThreadUnsafeRandom;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.SectionPosition;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.SnowAccumulationHeightEvent;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeTemperatureEvent;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkSection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.event.CraftEventFactory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.lime.gp.lime;
import org.lime.gp.module.biome.time.SeasonKey;
import org.lime.gp.module.biome.weather.WeatherBiomes;
import org.lime.plugin.CoreElement;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.RandomUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class SnowModify implements Listener {
    public static CoreElement create() {
        return CoreElement.create(SnowModify.class)
                .withInit(SnowModify::init)
                .withInstance();
    }
    private static void init() {
        lime.repeatTicks(SnowModify::update, 1);
    }
    private static void update() {
        /*
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (!player.isInWater()) return;
            if (player instanceof CraftPlayer cplayer) {
                cplayer.getHandle().setIsInPowderSnow(true);
            }
        });
        */
        if (tickChunks.isEmpty()) return;
        HashSet<Toast2<WorldServer, SectionPosition>> tickChunks = SnowModify.tickChunks;
        SnowModify.tickChunks = new HashSet<>();
        tickChunks.removeIf(kv -> kv.invokeGet((world, position) -> {
            tickSnow(world, position, 70);
            return true;
        }));
    }
    private static HashSet<Toast2<WorldServer, SectionPosition>> tickChunks = new HashSet<>();
    private static final BlockPosition.MutableBlockPosition chunkTickMutablePosition = new BlockPosition.MutableBlockPosition();
    private static final ThreadUnsafeRandom randomTickRandom = new ThreadUnsafeRandom(RandomSource.create().nextLong());
    private static int tickSnow(WorldServer world, SectionPosition position, int count) {
        ChunkCoordIntPair coord = position.chunk();
        Chunk chunk = world.getChunkIfLoaded(coord.x, coord.z);
        if (chunk == null) return 0;
        return tickSnow(world, coord, chunk.getSection(chunk.getSectionIndexFromSectionY(position.y())), count);
    }
    private static int tickSnow(WorldServer world, ChunkCoordIntPair coord, ChunkSection section, int count) {
        int snowTicks = 0;
        if (section == null || section.tickingList.size() == 0) return 0;
        int offsetX = coord.getMinBlockX();
        int offsetZ = coord.getMinBlockZ();
        int yPos = section.bottomBlockY();
        for (int i = 0; i < count; ++i) {
            int tickingBlocks = section.tickingList.size();
            int index = randomTickRandom.a(4096);
            if (index >= tickingBlocks) continue;
            long raw = section.tickingList.getRaw(index);
            int location = IBlockDataList.getLocationFromRaw(raw);
            int randomX = location & 0xF;
            int randomY = location >>> 8 & 0xFF | yPos;
            int randomZ = location >>> 4 & 0xF;
            BlockPosition.MutableBlockPosition position = chunkTickMutablePosition.set(offsetX + randomX, randomY, offsetZ + randomZ);
            IBlockData iblockdata = IBlockDataList.getBlockDataFromRaw(raw);
            if (iblockdata.is(Blocks.SNOW) || iblockdata.is(Blocks.FROSTED_ICE)) {
                snowTicks++;
                iblockdata.randomTick(world, position, randomTickRandom);
            }
        }
        return snowTicks;
    }

    private static SeasonKey seasonKey;
    public static void setBiome(SeasonKey seasonKey) {
        boolean enableSnow = seasonKey == SeasonKey.Frosty;
        SnowModify.seasonKey = seasonKey;

        MinecraftServer.getServer().getAllLevels().forEach(world -> {
            if (enableSnow) {
                world.paperConfig().environment.frostedIce.enabled = false;
                world.paperConfig().environment.frostedIce.delay.min = 20000;
                world.paperConfig().environment.frostedIce.delay.max = 40000;
            } else {
                world.paperConfig().environment.frostedIce.enabled = true;
                world.paperConfig().environment.frostedIce.delay.min = 2000;
                world.paperConfig().environment.frostedIce.delay.max = 4000;
            }
        });
    }

    @EventHandler private static void on(BlockSnowTickEvent e) {
        BlockPosition position = e.getPos();
        WorldServer world = e.getWorld();
        BiomeBase biome = world.getBiome(position).value();
        if (biome.coldEnoughToSnow(position)) return;
        tickChunks.add(Toast.of(world, SectionPosition.of(position)));
        IBlockData oldBlock = e.getState();
        int layer = Math.max(oldBlock.getValue(BlockSnow.LAYERS) - RandomUtils.rand(1, 2), 0);
        IBlockData newBlock = layer == 0 ? Blocks.AIR.defaultBlockState() : oldBlock.setValue(BlockSnow.LAYERS, layer);
        if (CraftEventFactory.callBlockFadeEvent(world, position, newBlock).isCancelled()) return;
        BlockSnow.dropResources(oldBlock, world, position);
        if (newBlock.isAir()) world.removeBlock(position, false);
        else world.setBlock(position, newBlock, Block.UPDATE_ALL);
    }
    private static final IRegistry<BiomeBase> BIOME_REGISTRY = MinecraftServer.getServer().registryAccess().registryOrThrow(Registries.BIOME);
    @EventHandler private static void on(BiomeTemperatureEvent e) {
        BiomeBase biome = e.getBiome();
        String key = BIOME_REGISTRY.getKey(biome).toString();
        WeatherBiomes.selectBiome(seasonKey, key)
                .flatMap(v -> v.biomeData.snow())
                .ifPresentOrElse(status -> e.setTemperature(status ? -0.5f : 0.8f), () -> {
                    if (!biome.hasPrecipitation() || seasonKey != SeasonKey.Frosty) return;
                    e.setTemperature(0);
                });
        /*if (!SnowModify.enableSnow || !e.getBiome().hasPrecipitation()) return;
        WeatherBiomes.selectBiome(seasonKey, e.getBiome())
        e.setTemperature(0);*/
    }
    private static float generateRandom(float x, float y) {
        int seed = (int)(2166136261L % Integer.MAX_VALUE);
        seed = (seed * 16777619) ^ Float.hashCode(x);
        seed = (seed * 16777619) ^ Float.hashCode(y);
        return new Random(seed).nextFloat();
    }
    @EventHandler private static void on(SnowAccumulationHeightEvent e) {
        int height = e.getHeight();
        BlockPosition position = e.getPosition();
        if (height <= 1) return;
        height = Math.round((height - 1) * generateRandom(position.getX(), position.getZ())) + 1;
        e.setHeight(height);
    }
}













