package org.lime.gp.access;

import com.google.common.collect.ImmutableList;
import io.papermc.paper.chunk.PlayerChunkLoader;
import io.papermc.paper.util.maplist.IteratorSafeOrderedReferenceSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryMaterials;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.PacketPlayOutLogin;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ChunkProviderServer;
import net.minecraft.server.level.PlayerChunk;
import net.minecraft.server.level.PlayerChunkMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.entity.animal.horse.EntityHorseAbstract;
import net.minecraft.world.entity.monster.EntityStrider;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.food.FoodMetaData;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.inventory.ContainerWorkbench;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.ItemCarrotStick;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R3.map.CraftMapView;
import org.lime.reflection;
import org.lime.system.execute.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class ReflectionAccess {
    public static final reflection.constructor<PacketPlayOutTileEntityData> init_PacketPlayOutTileEntityData = reflection.constructor.of(PacketPlayOutTileEntityData.class, BlockPosition.class, TileEntityTypes.class, NBTTagCompound.class);
    public static final reflection.field<FoodMetaData> foodData_EntityHuman = reflection.field.ofMojang(EntityHuman.class, "foodData");
    public static final reflection.field<net.minecraft.world.item.ItemStack> lastItemInMainHand_EntityHuman = reflection.field.ofMojang(EntityHuman.class, "lastItemInMainHand");

    public static final reflection.field<WorldMap> worldMap_CraftMapView = reflection.field.of(CraftMapView.class, "worldMap");
    public static final reflection.field<EntityHuman> entityhuman_FoodMetaData = reflection.field.of(FoodMetaData.class, "entityhuman");

    public static final reflection.field<Float> destroySpeed_BlockData = reflection.field.<Float>ofMojang(BlockBase.BlockData.class, "destroySpeed").nonFinal();

    public static final Class<?> a_ClientboundLevelChunkPacketData = Execute.<Class<?>>func(() -> {
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
    public static final Func4<Integer, Integer, TileEntityTypes<?>, NBTTagCompound, Object> init_a_ClientboundLevelChunkPacketData_Func = init_a_ClientboundLevelChunkPacketData::newInstance;
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

    public static final reflection.field<List<TickingBlockEntity>> blockEntityTickers_World = reflection.field.ofMojang(World.class, "blockEntityTickers");

    public static final reflection.field<Map<MinecraftKey, LootTableInfo.b>> dynamicDrops_LootTableInfo = reflection.field.ofMojang(LootTableInfo.class, "dynamicDrops");

    public static final reflection.field<EnumSet<ClientboundPlayerInfoUpdatePacket.a>> actions_ClientboundPlayerInfoUpdatePacket = reflection.field.<EnumSet<ClientboundPlayerInfoUpdatePacket.a>>ofMojang(ClientboundPlayerInfoUpdatePacket.class, "actions").nonFinal();
    public static final reflection.field<List<ClientboundPlayerInfoUpdatePacket.b>> entries_ClientboundPlayerInfoUpdatePacket = reflection.field.<List<ClientboundPlayerInfoUpdatePacket.b>>ofMojang(ClientboundPlayerInfoUpdatePacket.class, "entries").nonFinal();
    public static final reflection.field<IRegistryCustom.Dimension> synchronizedRegistries_PlayerList = reflection.field.ofMojang(PlayerList.class, "synchronizedRegistries");
    public static final reflection.field<RegistryOps<NBTBase>> BUILTIN_CONTEXT_OPS_PacketPlayOutLogin = reflection.field.ofMojang(PacketPlayOutLogin.class, "BUILTIN_CONTEXT_OPS");
    public static final reflection.field<byte[]> buffer_ClientboundLevelChunkPacketData = reflection.field.<byte[]>ofMojang(ClientboundLevelChunkPacketData.class, "buffer").nonFinal();
    public static final reflection.field<LongOpenHashSet> sentChunks_PlayerLoaderData_PlayerChunkLoader = reflection.field.ofMojang(PlayerChunkLoader.PlayerLoaderData.class, "sentChunks");
    public static final reflection.field<Double> lastLocX_PlayerLoaderData_PlayerChunkLoader = reflection.field.ofMojang(PlayerChunkLoader.PlayerLoaderData.class, "lastLocX");
    public static final Class<?> class_CraftMetaItem = Execute.<String, Class<?>>funcEx(Class::forName).throwable().invoke(CraftItemStack.class.getName().replace("CraftItemStack", "CraftMetaItem"));//unhandledTags
    public static final reflection.field<Map<String, NBTBase>> unhandledTags_CraftMetaItem = reflection.field.of(class_CraftMetaItem, "unhandledTags");
    public static final reflection.constructor<NBTTagCompound> initMap_NBTTagCompound = reflection.constructor.of(NBTTagCompound.class, Map.class);

    public static final reflection.field<IntProvider> RAIN_DELAY_WorldServer = reflection.field.<IntProvider>ofMojang(WorldServer.class, "RAIN_DELAY").nonFinal();
    public static final reflection.field<IntProvider> RAIN_DURATION_WorldServer = reflection.field.<IntProvider>ofMojang(WorldServer.class, "RAIN_DURATION").nonFinal();
    public static final reflection.field<Float> MAX_MOVEMENT_SPEED_EntityHorseAbstract = reflection.field.<Float>ofMojang(EntityHorseAbstract.class, "MAX_MOVEMENT_SPEED").nonFinal();
    public static final reflection.field<Entity> canInteractWith_ItemCarrotStick = reflection.field.<Entity>ofMojang(ItemCarrotStick.class, "canInteractWith").nonFinal();
    public static final reflection.field<RecipeItemStack> TEMPT_ITEMS_EntityStrider = reflection.field.<RecipeItemStack>ofMojang(EntityStrider.class, "TEMPT_ITEMS").nonFinal();
}
















