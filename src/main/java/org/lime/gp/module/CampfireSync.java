package org.lime.gp.module;

import com.google.gson.JsonPrimitive;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockCampfire;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_19_R3.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_19_R3.persistence.CraftPersistentDataContainer;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.lime.core;
import org.lime.gp.extension.JManager;
import org.lime.gp.lime;
import org.lime.gp.access.ReflectionAccess;

import java.util.ArrayList;
import java.util.List;

public class CampfireSync implements Listener {
    public static long CAMPFIRE_TIME_MS = 0;
    public static core.element create() {
        return core.element.create(CampfireSync.class)
                .<JsonPrimitive>addConfig("config", v -> v.withParent("campfire_time").withDefault(new JsonPrimitive(2.0*24.0*60.0*60.0)).withInvoke(j -> CAMPFIRE_TIME_MS = Math.round(j.getAsDouble() * 1000)))
                .withInit(CampfireSync::init)
                .withInstance();
    }
    public static void init() {
        lime.repeat(CampfireSync::update, 10);
    }
    
    @SuppressWarnings("all")
    public static void update() {
        String campfireType = TileEntityTypes.getKey(TileEntityTypes.CAMPFIRE).toString();
        MinecraftServer.getServer().getAllLevels().forEach(world -> {
            List<BlockPosition> positions = new ArrayList<>();
            ReflectionAccess.blockEntityTickers_World.get(world).forEach(item -> {
                if (campfireType.equals(item.getType()))
                    positions.add(item.getPos());
            });
            /*world.getChunkSource().chunkMap.updatingChunks.getVisibleMap().values().forEach(playerChunk -> {
                net.minecraft.world.level.chunk.Chunk chunk = playerChunk.getFullChunkNow();
                if (chunk == null) return;
                chunk.blockEntities.forEach((pos, tile) -> {
                    if (tile instanceof TileEntityCampfire)
                        positions.add(pos);
                });
            });*/
            positions.forEach(position -> world.getBlockEntity(position, TileEntityTypes.CAMPFIRE).ifPresent(campfire -> {
                CraftPersistentDataContainer container = campfire.persistentDataContainer;
                long ms = System.currentTimeMillis();
                IBlockData state = campfire.getBlockState();
                if (state.getValue(BlockCampfire.LIT)) {
                    long fire_ms = container.getOrDefault(LAST_FIRE_TIME, PersistentDataType.LONG, -1L);
                    if (fire_ms == -1) container.set(LAST_FIRE_TIME, PersistentDataType.LONG, ms);
                    else if (ms > fire_ms + CAMPFIRE_TIME_MS) {
                        state = state.setValue(BlockCampfire.LIT, false);
                        if (CraftEventFactory.callBlockFadeEvent(world, position, state).isCancelled()) return;
                        //Block.dropResources(state, world, pos);
                        if (state.isAir()) world.removeBlock(position, false);
                        else world.setBlock(position, state, Block.UPDATE_ALL);
                    }
                } else {
                    container.remove(LAST_FIRE_TIME);
                }
            }));
        });
    }
    public static final NamespacedKey LAST_FIRE_TIME = JManager.key("last_fire_time");
    /*
    @EventHandler public static void on(Ticker.TickEvent event) {
        if (event.getEntity() instanceof TileEntityCampfire campfire) {
            CraftPersistentDataContainer container = campfire.persistentDataContainer;
            long ms = System.currentTimeMillis();
            IBlockData state = campfire.getBlockState();
            if (state.getValue(BlockCampfire.LIT)) {
                long fire_ms = container.getOrDefault(LAST_FIRE_TIME, PersistentDataType.LONG, -1L);
                if (fire_ms == -1) container.set(LAST_FIRE_TIME, PersistentDataType.LONG, ms);
                else if (ms > fire_ms + CAMPFIRE_TIME_MS) {
                    state = state.setValue(BlockCampfire.LIT, false);
                    World world = event.getWorld();
                    BlockPosition position = event.getPosition();
                    if (CraftEventFactory.callBlockFadeEvent(world, position, state).isCancelled()) return;
                    //Block.dropResources(state, world, pos);
                    if (state.isAir()) world.removeBlock(position, false);
                    else world.setBlock(position, state, Block.UPDATE_ALL);
                }
            } else {
                container.remove(LAST_FIRE_TIME);
            }
        }
    }
   */
}















