package org.lime.gp.module.biome.holiday;

import com.destroystokyo.paper.util.maplist.IBlockDataList;
import com.google.gson.JsonObject;

import io.papermc.paper.util.WorldUtil;
import io.papermc.paper.util.math.ThreadUnsafeRandom;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkProviderServer;
import net.minecraft.server.level.PlayerChunk;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.BlockSnow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.levelgen.HeightMap;
import org.bukkit.craftbukkit.v1_19_R3.event.CraftEventFactory;
import org.bukkit.event.Listener;
import org.lime.core;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.lime;
import org.lime.gp.module.biome.BiomeModify;
import org.lime.system;

import java.util.Optional;

public class Snowy implements Listener {
    public static core.element create() {
        return core.element.create(Snowy.class)
                .withInstance()
                .withInit(Snowy::init)
                .disable()
                .<JsonObject>addConfig("config", v -> v
                        .withParent("snowy")
                        .withDefault(system.json.object()
                                .add("enable", false)
                                .build())
                        .withInvoke(Snowy::config)
                );
    }

    
    private final static MinecraftServer server = MinecraftServer.getServer();
    private final static IRegistryCustom.Dimension dimension = server.registryAccess();
    
    private final static IRegistry<BiomeBase> data = dimension.registryOrThrow(Registries.BIOME);
    private final static Holder<BiomeBase> snowy_taiga = data.getHolderOrThrow(Biomes.SNOWY_TAIGA);
    private static boolean ENABLE = false;
    public static void init() {
        lime.repeatTicks(Snowy::tickServer, 5);
    }

    private static BiomeModify.ActionCloseable closeable = null;
    public static void config(JsonObject json) {
        ENABLE = json.get("enable").getAsBoolean();

        server.getAllLevels().forEach(world -> {
            if (ENABLE) {
                world.paperConfig().environment.frostedIce.enabled = false;
                world.paperConfig().environment.frostedIce.delay.min = 20000;
                world.paperConfig().environment.frostedIce.delay.max = 40000;
            } else {
                world.paperConfig().environment.frostedIce.enabled = true;
                world.paperConfig().environment.frostedIce.delay.min = 2000;
                world.paperConfig().environment.frostedIce.delay.max = 4000;
            }
            world.paperConfig().environment.disableIceAndSnow = true;
        });

        if ((closeable != null) == ENABLE) return;
        if (ENABLE) {
            BiomeModify.ActionCloseable _closeable = BiomeModify.appendModify(Snowy::modify);
            if (closeable != null) closeable.close();
            closeable = _closeable;
        } else {
            closeable.close();
            closeable = null;
        }
    }
    public static void modify(int id, String name, NBTTagCompound element) {
        element.putString("precipitation", "snow");
        element.putFloat("temperature", -1f);
        element.putString("category", "taiga");
    }
    public static void tickServer() {
        ThreadUnsafeRandom randomTickRandom = new ThreadUnsafeRandom(1000);
        server.getAllLevels().forEach(world -> {
            ChunkProviderServer chunkProviderServer = world.getChunkSource();
            int randomTickSpeed = world.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
            ReflectionAccess.entityTickingChunks_ChunkProviderServer.get(chunkProviderServer).iterator().forEachRemaining(chunk -> {
                PlayerChunk holder = chunk.playerChunk;
                if (holder == null || !(boolean) ReflectionAccess.anyPlayerCloseEnoughForSpawning_PlayerChunkMap.call(chunkProviderServer.chunkMap, new Object[] { holder, chunk.getPos(), false })) return;
                tickChunk(world, chunk, randomTickSpeed, randomTickRandom);
            });
        });
    }
    private static Optional<IBlockData> shouldSnow(BiomeBase biomeBase, IWorldReader world, BlockPosition pos) {
        if (biomeBase.warmEnoughToRain(pos)) return Optional.empty();
        return shouldSnow(world, pos, true);
    }
    private static Optional<IBlockData> shouldSnow(IWorldReader world, BlockPosition pos, boolean light) {
        if (pos.getY() >= world.getMinBuildHeight() && pos.getY() < world.getMaxBuildHeight() && (!light || world.getBrightness(EnumSkyBlock.BLOCK, pos) < 10)) {
            IBlockData blockState = world.getBlockState(pos);
            if ((blockState.isAir() || blockState.is(Blocks.SNOW)) && Blocks.SNOW.defaultBlockState().canSurvive(world, pos))
                return Optional.of(blockState);
        }
        return Optional.empty();
    }
    public static void throwSnow(WorldServer world, BlockPosition blockposition, net.minecraft.world.entity.Entity entity) {
        shouldSnow(world, blockposition, false).ifPresent(data -> {
            if (data.isAir()) {
                data = Blocks.SNOW.defaultBlockState();
            } else {
                int layers = data.getValue(BlockSnow.LAYERS);
                if (layers == 8 || !system.rand_is((0.25 / layers) / layers)) return;
                data = data.setValue(BlockSnow.LAYERS, layers + 1);
            }
            CraftEventFactory.handleBlockFormEvent(world, blockposition, data, entity);
        });
    }
    private static void tickChunk(WorldServer world, Chunk chunk, int randomTickSpeed, ThreadUnsafeRandom randomTickRandom) {
        ChunkCoordIntPair chunkcoordintpair = chunk.getPos();
        int minBlockX = chunkcoordintpair.getMinBlockX();
        int minBlockZ = chunkcoordintpair.getMinBlockZ();
        boolean isRaining = world.isRaining();
        BlockPosition.MutableBlockPosition blockposition = new BlockPosition.MutableBlockPosition();
        if (ENABLE && randomTickRandom.nextInt(32) == 0) {
            world.getRandomBlockPosition(minBlockX, 0, minBlockZ, 15, blockposition);
            int normalY = chunk.getHeight(HeightMap.Type.MOTION_BLOCKING, blockposition.getX() & 0xF, blockposition.getZ() & 0xF) + 1;
            int downY = normalY - 1;
            blockposition.setY(normalY);
            BiomeBase biomebase = snowy_taiga.value();
            blockposition.setY(downY);
            if (biomebase.shouldFreeze(world, blockposition)) {
                CraftEventFactory.handleBlockFormEvent(world, blockposition, Blocks.FROSTED_ICE.defaultBlockState(), null);
            }
            if (isRaining) {
                blockposition.setY(normalY);
                shouldSnow(biomebase, world, blockposition).ifPresent(data -> {
                    if (data.isAir()) {
                        data = Blocks.SNOW.defaultBlockState();
                    } else {
                        int layers = data.getValue(BlockSnow.LAYERS);
                        if (layers == 8 || !system.rand_is((0.25 / layers) / layers)) return;
                        data = data.setValue(BlockSnow.LAYERS, layers + 1);
                    }
                    CraftEventFactory.handleBlockFormEvent(world, blockposition, data, null);
                });
                blockposition.setY(downY);
                IBlockData iblockdata = world.getBlockState(blockposition);
                BiomeBase.Precipitation biomebase_precipitation = biomebase.getPrecipitationAt(blockposition);
                if (biomebase_precipitation == BiomeBase.Precipitation.RAIN && biomebase.coldEnoughToSnow(blockposition)) {
                    biomebase_precipitation = BiomeBase.Precipitation.SNOW;
                }
                iblockdata.getBlock().handlePrecipitation(iblockdata, world, blockposition, biomebase_precipitation);
            }
        }
        if (!ENABLE && randomTickSpeed > 0) {
            ChunkSection[] sections = chunk.getSections();
            int minSection = WorldUtil.getMinSection(world);
            for (int sectionIndex = 0; sectionIndex < sections.length; ++sectionIndex) {
                ChunkSection section = sections[sectionIndex];
                if (section == null || section.tickingList.size() == 0) continue;
                int yPos = sectionIndex + minSection << 4;
                for (int a2 = 0; a2 < randomTickSpeed; ++a2) {
                    int tickingBlocks = section.tickingList.size();
                    int index = randomTickRandom.nextInt(4096);
                    if (index >= tickingBlocks) continue;
                    long raw = section.tickingList.getRaw(index);
                    int location = IBlockDataList.getLocationFromRaw(raw);
                    int randomX = location & 0xF;
                    int randomY = location >>> 8 & 0xFF | yPos;
                    int randomZ = location >>> 4 & 0xF;
                    BlockPosition.MutableBlockPosition blockposition2 = blockposition.set(minBlockX + randomX, randomY, minBlockZ + randomZ);
                    IBlockData iblockdata = IBlockDataList.getBlockDataFromRaw(raw);
                    tickBlock(world, chunk, blockposition2, iblockdata, randomTickRandom);
                }
            }
        }
    }
    private static void tickBlock(WorldServer world, Chunk chunk, BlockPosition.MutableBlockPosition pos, IBlockData state, ThreadUnsafeRandom randomTickRandom) {
        if (state.is(Blocks.SNOW)) tickSnow(world, chunk, pos, state, randomTickRandom);
    }
    private static void tickSnow(WorldServer world, Chunk chunk, BlockPosition.MutableBlockPosition pos, IBlockData state, ThreadUnsafeRandom randomTickRandom) {
        if (chunk.getNoiseBiome(pos.getX(), pos.getY(), pos.getZ()).value().coldEnoughToSnow(pos)) return;
        int layers = state.getOptionalValue(BlockSnow.LAYERS).orElse(BlockSnow.LAYERS.max);
        if (layers <= BlockSnow.LAYERS.min) {
            if (CraftEventFactory.callBlockFadeEvent(world, pos, Blocks.AIR.defaultBlockState()).isCancelled()) return;
            BlockSnow.dropResources(state, world, pos);
            world.removeBlock(pos, false);
        } else {
            if (!world.getBlockState(pos.above()).isAir()) return;
            state = Blocks.SNOW.defaultBlockState().setValue(BlockSnow.LAYERS, layers - 1);
            if (CraftEventFactory.callBlockFadeEvent(world, pos, state).isCancelled()) return;
            world.setBlock(pos, state, 3);
        }
    }
}





















