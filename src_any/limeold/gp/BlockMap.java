package org.lime.gp.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.lime.Position;
import org.lime.gp.lime;
import org.lime.gp.extension.PacketManager;
import org.lime.system;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class BlockMap {
    public static final ConcurrentHashMap<Position, BlocksOld.Info> byPosition = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Position> byUUID = new ConcurrentHashMap<>();

    private static class ClientInfo {
        public static class Chunk {
            public final HashSet<Position> positions = new HashSet<>();

            public void send(WorldServer world, long chunkID, system.Action1<Packet<?>> addPacket) {
                /*int chunkX = (int) chunkID;
                int chunkZ = (int) (chunkID >> 32);

                if (positions.size() > 16) {
                    positions.clear();
                    net.minecraft.world.level.chunk.Chunk chunk = world.getChunkIfLoaded(chunkX, chunkZ);
                    if (chunk != null) addPacket.invoke(new PacketPlayOutMapChunk(chunk, true));
                    else lime.logOP("Send chunk: " + chunkX + " " + chunkZ + (chunk == null ? " EMPTY" : ""));
                }
                else */
                {
                    positions.forEach(pos -> {
                        BlockPosition blockPosition = new BlockPosition(pos.x, pos.y, pos.z);
                        IBlockData block = world.getBlockStateIfLoaded(blockPosition);
                        if (block != null) addPacket.invoke(new PacketPlayOutBlockChange(blockPosition, block));
                        else lime.logOP("Send block: " + pos.toString(false) + (block == null ? " EMPTY" : ""));
                    });
                }
            }

            public void markDirty(Position position) {
                positions.add(position);
            }
        }

        public final HashMap<Long, ClientInfo.Chunk> chunks = new HashMap<>();

        public void send(WorldServer world, system.Action1<Packet<?>> addPacket) {
            chunks.forEach((id, chunk) -> chunk.send(world, id, addPacket));
        }

        public void markDirty(Position position) {
            chunks.compute(org.bukkit.Chunk.getChunkKey(position.x >> 4, position.z >> 4), (chunkID, chunk) -> {
                if (chunk == null) chunk = new ClientInfo.Chunk();
                chunk.markDirty(position);
                return chunk;
            });
        }
    }
    private static final ConcurrentHashMap<UUID, HashSet<Position>> sendToClient = new ConcurrentHashMap<>();
    public static Stream<BlocksOld.Info> blocks() {
        return byPosition.values().stream();
    }
    public static Stream<Map.Entry<Position, BlocksOld.Info>> blockWithPositions() {
        return byPosition.entrySet().stream();
    }
    public static <T> Stream<system.Toast2<Position, T>> instances(Class<T> instanceClass) {
        return blockWithPositions()
                .flatMap(v ->
                        v.getValue()
                                .instances
                                .values()
                                .stream()
                                .map(_v -> instanceClass.isInstance(_v) ? system.toast(v.getKey(), (T) _v) : null)
                                .filter(Objects::nonNull)
                );
    }
    public static Optional<BlocksOld.Info> byPosition(Position position) {
        return Optional.ofNullable(byPosition.getOrDefault(position, null));
    }
    public static Optional<BlocksOld.Info> byPosition(net.minecraft.world.level.World world, BlockPosition position) {
        return Optional.ofNullable(world).flatMap(_world -> byPosition(new Position(_world.getWorld(), position.getX(), position.getY(), position.getZ())));
    }
    public static Optional<BlocksOld.Info> byUUID(UUID uuid) {
        return positionByUUID(uuid).flatMap(BlockMap::byPosition);
    }
    public static Optional<Position> positionByUUID(UUID uuid) {
        return Optional.ofNullable(byUUID.getOrDefault(uuid, null));
    }
    public static void removeIfPosition(system.Func2<Position, BlocksOld.Info, Boolean> filter) {
        BlockMap.byPosition.entrySet().removeIf(kv -> {
            if (!filter.invoke(kv.getKey(), kv.getValue())) return false;
            byUUID.remove(kv.getValue().uuid);
            return true;
        });
    }
    public static Optional<BlocksOld.Info> getForceInfo(net.minecraft.world.level.World world, BlockPosition position) {
        return BlockMap.byPosition(world, position)
                .or(() -> Optional.ofNullable(world.getBlockEntity(position) instanceof TileEntitySkull _s ? _s : null).flatMap(BlocksOld::syncGetBlock));
    }
    public static Optional<system.Toast2<BlocksOld.Info, TileEntitySkull>> getForceTileInfo(net.minecraft.world.level.World world, BlockPosition position) {
        return Optional.ofNullable(world.getBlockEntity(position) instanceof TileEntitySkull _s ? _s : null).flatMap(_s -> BlocksOld.syncGetBlock(_s).map(__s -> system.toast(__s, _s)));
    }
    public static void clear() {
        byPosition.values().forEach(BlocksOld.Info::close);
        byPosition.clear();
        byUUID.clear();
    }
    public static void markDirty(Player player, Position position) {
        markDirty(player.getUniqueId(), position);
    }
    public static void markDirty(UUID playerUUID, Position position) {
        lime.once(() -> _markDirty(playerUUID, position), 0.5);
        _markDirty(playerUUID, position);
    }
    private static void _markDirty(UUID playerUUID, Position position) {
        sendToClient.compute(playerUUID, (key, value) -> {
            if (value == null) value = new HashSet<>();
            value.add(position);
            /*if (value == null) value = new ClientInfo();
            value.markDirty(position);*/
            return value;
        });
    }
    public static void sendToClient() {
        HashMap<UUID, ClientInfo> clients = new HashMap<>();
        HashMap<UUID, HashSet<Position>> sendToClient = new HashMap<>();
        BlockMap.sendToClient.entrySet().removeIf(kv -> {
            sendToClient.put(kv.getKey(), kv.getValue());
            return true;
        });
        sendToClient.forEach((uuid, list) -> clients.compute(uuid, (k, v) -> {
            if (v == null) v = new ClientInfo();
            list.forEach(v::markDirty);
            return v;
        }));
        clients.forEach((uuid, chunk) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return;
            WorldServer world = ((CraftWorld) player.getWorld()).getHandle();
            List<Packet<?>> packets = new ArrayList<>();
            chunk.send(world, packets::add);
            PacketManager.sendPackets(player, packets);
        });
    }
}
