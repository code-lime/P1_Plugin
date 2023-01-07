package org.lime.gp.access;

import com.google.common.collect.ImmutableList;
import io.papermc.paper.util.maplist.IteratorSafeOrderedReferenceSet;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryMaterials;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.ChunkProviderServer;
import net.minecraft.server.level.PlayerChunk;
import net.minecraft.server.level.PlayerChunkMap;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.food.FoodMetaData;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.inventory.ContainerWorkbench;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.v1_18_R2.map.CraftMapView;
import org.lime.reflection;
import org.lime.system;

import java.util.Map;
import java.util.NoSuchElementException;

public class ReflectionAccess {
    public static final reflection.constructor<PacketPlayOutTileEntityData> init_PacketPlayOutTileEntityData = reflection.constructor.of(PacketPlayOutTileEntityData.class, BlockPosition.class, TileEntityTypes.class, NBTTagCompound.class);
    public static final reflection.field<FoodMetaData> foodData_EntityHuman = reflection.field.ofMojang(EntityHuman.class, "foodData");

    public static final reflection.field<WorldMap> worldMap_CraftMapView = reflection.field.of(CraftMapView.class, "worldMap");
    public static final reflection.field<EntityHuman> entityhuman_FoodMetaData = reflection.field.of(FoodMetaData.class, "entityhuman");

    public static final reflection.field<Float> destroySpeed_BlockData = reflection.field.<Float>ofMojang(BlockBase.BlockData.class, "destroySpeed").nonFinal();

    public static final Class<?> a_ClientboundLevelChunkPacketData = system.<Class<?>>func(() -> {
        try {
            for (Class<?> tClass : ClientboundLevelChunkPacketData.class.getDeclaredClasses())
                if (tClass.getSimpleName().equals("a"))
                    return tClass;
            throw new NoSuchElementException(ClientboundLevelChunkPacketData.class.getName() + "$a");
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }).invoke();
    public static final reflection.constructor<?> init_a_ClientboundLevelChunkPacketData = reflection.constructor.of(a_ClientboundLevelChunkPacketData, Integer.TYPE, Integer.TYPE, TileEntityTypes.class, NBTTagCompound.class);
    public static final system.Func4<Integer, Integer, TileEntityTypes<?>, NBTTagCompound, Object> init_a_ClientboundLevelChunkPacketData_Func = init_a_ClientboundLevelChunkPacketData::newInstance;
    public static final reflection.field<NBTTagCompound> tag_a_ClientboundLevelChunkPacketData = reflection.field.<NBTTagCompound>of(a_ClientboundLevelChunkPacketData, "d").nonFinal();

    public static final reflection.field<IteratorSafeOrderedReferenceSet<Chunk>> entityTickingChunks_ChunkProviderServer = reflection.field.<IteratorSafeOrderedReferenceSet<Chunk>>ofMojang(ChunkProviderServer.class, "entityTickingChunks").nonFinal();
    public static final reflection.method anyPlayerCloseEnoughForSpawning_PlayerChunkMap = reflection.method.ofMojang(
            PlayerChunkMap.class,
            "anyPlayerCloseEnoughForSpawning",
            PlayerChunk.class,
            ChunkCoordIntPair.class,
            Boolean.TYPE
    );
    public static final reflection.method playHurtSound_EntityLiving = reflection.method.ofMojang(
            EntityLiving.class,
            "playHurtSound",
            DamageSource.class
    );

    public static final reflection.field<Boolean> frozen_RegistryMaterials = reflection.field.ofMojang(RegistryMaterials.class, "frozen");

    public static final reflection.field<NonNullList<ItemStack>> items_InventoryCrafting = reflection.field.<NonNullList<ItemStack>>ofMojang(InventoryCrafting.class, "items").nonFinal();
    public static final reflection.field<Integer> width_InventoryCrafting = reflection.field.<Integer>ofMojang(InventoryCrafting.class, "width").nonFinal();
    public static final reflection.field<Integer> height_InventoryCrafting = reflection.field.<Integer>ofMojang(InventoryCrafting.class, "height").nonFinal();
    public static final reflection.field<CraftInventoryView> bukkitEntity_ContainerWorkbench = reflection.field.<CraftInventoryView>ofMojang(ContainerWorkbench.class, "bukkitEntity").nonFinal();
    public static final reflection.field<CraftInventoryView> bukkitEntity_ContainerChest = reflection.field.<CraftInventoryView>ofMojang(ContainerChest.class, "bukkitEntity").nonFinal();

    public static final reflection.field<Float> creatureGenerationProbability_BiomeSettingsMobs = reflection.field.<Float>ofMojang(BiomeSettingsMobs.class, "creatureGenerationProbability").nonFinal();
    public static final reflection.field<Map<EnumCreatureType, WeightedRandomList<BiomeSettingsMobs.c>>> spawners_BiomeSettingsMobs = reflection.field.<Map<EnumCreatureType, WeightedRandomList<BiomeSettingsMobs.c>>>ofMojang(BiomeSettingsMobs.class, "spawners").nonFinal();
    public static final reflection.field<ImmutableList<BiomeSettingsMobs.c>> items_WeightedRandomList = reflection.field.<ImmutableList<BiomeSettingsMobs.c>>ofMojang(WeightedRandomList.class, "items").nonFinal();
    public static final reflection.field<Map<EntityTypes<?>, BiomeSettingsMobs.b>> mobSpawnCosts_BiomeSettingsMobs = reflection.field.<Map<EntityTypes<?>, BiomeSettingsMobs.b>>ofMojang(BiomeSettingsMobs.class, "mobSpawnCosts").nonFinal();

    public static final reflection.field<EnumCreatureType> category_EntityTypes = reflection.field.<EnumCreatureType>ofMojang(EntityTypes.class, "category").nonFinal();

    public static final reflection.field<Map<MinecraftKey, LootTableInfo.b>> dynamicDrops_LootTableInfo = reflection.field.ofMojang(LootTableInfo.class, "dynamicDrops");
}
















