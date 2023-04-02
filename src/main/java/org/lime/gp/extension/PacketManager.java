package org.lime.gp.extension;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.PlayerConnection;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.lime.gp.lime;
import org.lime.system;
import java.util.*;

public class PacketManager {
    public static void sendPacket(Player player, Packet<?> packet) {
        ((CraftPlayer)player).getHandle().connection.send(packet);
    }
    public static void sendPackets(Player player, List<Packet<?>> packets) {
        PlayerConnection playerConnection = ((CraftPlayer)player).getHandle().connection;
        packets.forEach(playerConnection::send);
    }
    public static void sendPackets(Player player, Packet<?>... packets) {
        sendPackets(player, Arrays.asList(packets));
    }
    public static net.minecraft.world.entity.Entity getEntityHandle(Entity entity) {
        return ((CraftEntity)entity).getHandle();
    }

    public static class Adapter extends PacketAdapter {
        public static class Builder {
            private final HashMap<PacketType, List<system.Action2<PacketType, PacketEvent>>> receiving = new HashMap<>();
            private final HashMap<PacketType, List<system.Action2<PacketType, PacketEvent>>> sending = new HashMap<>();

            public Builder add(PacketType type, system.Action2<PacketType, PacketEvent> func) {
                return add(Collections.singletonList(type), func);
            }
            public Builder add(PacketType type, system.Action1<PacketEvent> func) {
                return add(Collections.singletonList(type), func);
            }

            @SuppressWarnings("unchecked")
            public <T extends Packet<?>>Builder add(Class<T> packetClass, system.Action2<T, PacketEvent> func) {
                return add(PacketType.fromClass(packetClass), e -> func.invoke((T)e.getPacket().getHandle(), e));
            }

            public Builder add(PacketType[] types, system.Action2<PacketType, PacketEvent> func) {
                return add(Arrays.asList(types), func);
            }
            public Builder add(PacketType[] types, system.Action1<PacketEvent> func) {
                return add(Arrays.asList(types), func);
            }
            public Builder add(List<PacketType> types, system.Action2<PacketType, PacketEvent> func) {
                types.forEach(type -> (type.isServer() ? sending : receiving).compute(type, (k, v) -> {
                    if (v == null) v = new ArrayList<>();
                    v.add(func);
                    return v;
                }));
                return this;
            }
            public Builder add(List<PacketType> types, system.Action1<PacketEvent> func) {
                add(types,  (a,b) -> func.invoke(b));
                return this;
            }

            public Adapter build() {
                return new Adapter(this);
            }
            public void listen() { if (!receiving.isEmpty() || !sending.isEmpty()) ProtocolLibrary.getProtocolManager().addPacketListener(build()); }
        }
        private final HashMap<PacketType, List<system.Action2<PacketType, PacketEvent>>> receiving = new HashMap<>();
        private final HashMap<PacketType, List<system.Action2<PacketType, PacketEvent>>> sending = new HashMap<>();
        private Adapter(Builder builder) {
            super(lime._plugin, system.list.<PacketType>of().add(builder.receiving.keySet()).add(builder.sending.keySet()).build());
            this.receiving.putAll(builder.receiving);
            this.sending.putAll(builder.sending);
        }
        private void invoke(PacketEvent event, HashMap<PacketType, List<system.Action2<PacketType, PacketEvent>>> map) {
            PacketType type = event.getPacketType();
            List<system.Action2<PacketType, PacketEvent>> list = map.getOrDefault(type, null);
            if (list == null) return;
            list.forEach(func -> func.invoke(type, event));
        }
        @Override public void onPacketReceiving(PacketEvent event) {
            invoke(event, receiving);
        }
        @Override public void onPacketSending(PacketEvent event) {
            invoke(event, sending);
        }
    }

    public static Adapter.Builder adapter() {
        return new Adapter.Builder();
    }

    /*public static class ProxyPacketPlayOutMapChunk {
        public static final int TWO_MEGABYTES = 0x200000;

        public int x = 0;
        public int z = 0;
        public BitSet availableSections = new BitSet();
        public NBTTagCompound heightmaps = new NBTTagCompound();
        public int[] biomes = null;
        public byte[] buffer = null;
        public List<NBTTagCompound> blockEntitiesTags = new ArrayList<>();
        public List<Packet<?>> extraPackets = new ArrayList<>();

        private static final reflection.field<Integer> _x = reflection.field.<Integer>ofMojang(PacketPlayOutMapChunk.class, "x").nonFinal();
        private static final reflection.field<Integer> _z = reflection.field.<Integer>ofMojang(PacketPlayOutMapChunk.class, "z").nonFinal();
        private static final reflection.field<BitSet> _availableSections = reflection.field.<BitSet>ofMojang(PacketPlayOutMapChunk.class, "availableSections").nonFinal();
        private static final reflection.field<NBTTagCompound> _heightmaps = reflection.field.<NBTTagCompound>ofMojang(PacketPlayOutMapChunk.class, "heightmaps").nonFinal();
        private static final reflection.field<int[]> _biomes = reflection.field.<int[]>ofMojang(PacketPlayOutMapChunk.class, "biomes").nonFinal();
        private static final reflection.field<byte[]> _buffer = reflection.field.<byte[]>ofMojang(PacketPlayOutMapChunk.class, "buffer").nonFinal();
        private static final reflection.field<List<NBTTagCompound>> _blockEntitiesTags = reflection.field.<List<NBTTagCompound>>ofMojang(PacketPlayOutMapChunk.class, "blockEntitiesTags").nonFinal();
        private static final reflection.field<List<Packet<?>>> _extraPackets = reflection.field.<List<Packet<?>>>ofMojang(PacketPlayOutMapChunk.class, "extraPackets").nonFinal();

        private static final int TE_LIMIT = Integer.getInteger("Paper.excessiveTELimit", 750);

        public ProxyPacketPlayOutMapChunk() { }
        public ProxyPacketPlayOutMapChunk(Chunk chunk) {
            ChunkCoordIntPair chunkPos = chunk.getPos();
            this.x = chunkPos.x;
            this.z = chunkPos.z;
            this.heightmaps = new NBTTagCompound();
            for (Map.Entry<HeightMap.Type, HeightMap> entry : chunk.getHeightmaps()) {
                if (!entry.getKey().sendToClient()) continue;
                this.heightmaps.set(entry.getKey().getSerializationKey(), new NBTTagLongArray(entry.getValue().getRawData()));
            }
            this.biomes = chunk.getBiomeIndex().writeBiomes();
            this.buffer = new byte[this.calculateChunkSize(chunk)];
            this.availableSections = this.extractChunkData(new PacketDataSerializer(this.getWriteBuffer()), chunk);
            this.blockEntitiesTags = Lists.newArrayList();
            int totalTileEntities = 0;
            for (Map.Entry<BlockPosition, TileEntity> entry2 : chunk.getTileEntities().entrySet()) {
                PacketPlayOutTileEntityData updatePacket;
                TileEntity blockEntity = entry2.getValue();
                if (++totalTileEntities > TE_LIMIT && (updatePacket = blockEntity.getUpdatePacket()) != null) {
                    this.extraPackets.add(updatePacket);
                    continue;
                }
                NBTTagCompound compoundTag = blockEntity.getUpdateTag();
                if (blockEntity instanceof TileEntitySkull) TileEntitySkull.sanitizeTileEntityUUID(compoundTag);
                this.blockEntitiesTags.add(compoundTag);
            }
        }
        public void write(PacketDataSerializer buf) {
            buf.writeInt(this.x);
            buf.writeInt(this.z);
            buf.writeBitSet(this.availableSections);
            buf.writeNbt(this.heightmaps);
            buf.writeVarIntArray(this.biomes);
            buf.writeVarInt(this.buffer.length);
            buf.writeBytes(this.buffer);
            buf.writeCollection(this.blockEntitiesTags, PacketDataSerializer::writeNbt);
        }

        public PacketDataSerializer getReadBuffer() {
            return new PacketDataSerializer(Unpooled.wrappedBuffer(this.buffer));
        }
        private ByteBuf getWriteBuffer() {
            ByteBuf byteBuf = Unpooled.wrappedBuffer(this.buffer);
            byteBuf.writerIndex(0);
            return byteBuf;
        }

        public boolean chunkSectionFilter(ChunkSection chunkSection) {
            return true;
        }
        public BitSet extractChunkData(PacketDataSerializer buf, Chunk chunk) {
            return this.extractChunkData(buf, chunk, null);
        }
        public BitSet extractChunkData(PacketDataSerializer buf, Chunk chunk, ChunkPacketInfo<IBlockData> chunkPacketInfo) {
            BitSet bitSet = new BitSet();
            ChunkSection[] levelChunkSections = chunk.getSections();
            int j2 = levelChunkSections.length;
            for (int i2 = 0; i2 < j2; ++i2) {
                ChunkSection levelChunkSection = levelChunkSections[i2];
                if (levelChunkSection == Chunk.EMPTY_SECTION || levelChunkSection.isEmpty()) continue;
                if (!chunkSectionFilter(levelChunkSection)) continue;
                bitSet.set(i2);
                levelChunkSection.write(buf, chunkPacketInfo);
            }
            return bitSet;
        }
        private int calculateChunkSize(Chunk chunk) {
            int i2 = 0;
            for (ChunkSection levelChunkSection : chunk.getSections()) {
                if (levelChunkSection == Chunk.EMPTY_SECTION || levelChunkSection.isEmpty()) continue;
                if (!chunkSectionFilter(levelChunkSection)) continue;
                i2 += levelChunkSection.getSerializedSize();
            }
            return i2;
        }

        public List<Packet<?>> getExtraPackets() { return this.extraPackets; }
        public int getX() { return this.x; }
        public int getZ() { return this.z; }
        public BitSet getAvailableSections() { return this.availableSections; }
        public NBTTagCompound getHeightmaps() { return this.heightmaps; }
        public List<NBTTagCompound> getBlockEntitiesTags() { return this.blockEntitiesTags; }
        public int[] getBiomes() { return this.biomes; }

        public PacketPlayOutMapChunk packet() {
            PacketPlayOutMapChunk packet = (PacketPlayOutMapChunk)StructureCache.newPacket(PacketType.Play.Server.MAP_CHUNK);
            _x.set(packet, x);
            _z.set(packet, z);
            _availableSections.set(packet, availableSections);
            _heightmaps.set(packet, heightmaps);
            _biomes.set(packet, biomes);
            _buffer.set(packet, buffer);
            _blockEntitiesTags.set(packet, blockEntitiesTags);
            _extraPackets.set(packet, extraPackets);
            packet.setReady(true);
            return packet;
        }
    }*/
}






















