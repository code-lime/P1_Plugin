package org.lime.gp.block.component.display;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.LimeKey;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockCampfire;
import net.minecraft.world.level.block.BlockCoralDead;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.DataPaletteBlock;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.lime.Position;
import org.lime.core;
import org.lime.display.Displays;
import org.lime.display.Models;
import org.lime.display.invokable.PacketInvokable;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.event.BlockMarkerEventInteract;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.lime;
import org.lime.gp.module.TimeoutData;
import org.lime.system;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BlockDisplay implements Listener {
    public static core.element create() {
        return core.element.create(BlockDisplay.class)
                .withInit(BlockDisplay::init)
                .withInstance();
    };
    public static final BlockModelDisplay.EntityModelManager MODEL_MANAGER = BlockModelDisplay.manager();
    public static final BlockItemFrameDisplay.BlockItemFrameManager ITEM_FRAME_MANAGER = BlockItemFrameDisplay.manager();
    public static void init() {
        Displays.initDisplay(MODEL_MANAGER);
        Displays.initDisplay(ITEM_FRAME_MANAGER);
        PacketManager.adapter()
                .add(PacketPlayOutBlockChange.class, PacketListener::onPacket)
                .add(PacketPlayOutMultiBlockChange.class, PacketListener::onPacket)
                .add(ClientboundLevelChunkWithLightPacket.class, PacketListener::onPacket)
                .add(PacketPlayOutTileEntityData.class, PacketListener::onPacket)
                .add(PacketPlayInUseEntity.class, PacketListener::onPacket)
                .listen();
        AnyEvent.addEvent("resync.chunk", AnyEvent.type.owner, v -> v.createParam("[chunk_x]", "~").createParam("[chunk_z]", "~"), (p,cx,cz) -> {
            Chunk chunk = p.getChunk();
            int chunk_x = Objects.equals(cx, "~") ? chunk.getX() : Integer.parseInt(cx);
            int chunk_z = Objects.equals(cz, "~") ? chunk.getZ() : Integer.parseInt(cz);
            chunk.getWorld().refreshChunk(chunk_x, chunk_z);
        });
        AnyEvent.addEvent("block.display.variable", AnyEvent.type.other, v -> v
                .createParam(UUID::fromString, "[block_uuid:uuid]")
                .createParam(Integer::parseInt, "[x:int]")
                .createParam(Integer::parseInt, "[y:int]")
                .createParam(Integer::parseInt, "[z:int]")
                .createParam("[key:text]")
                .createParam("[value:text]"),
                (p, block_uuid, x, y, z, key, value) -> org.lime.gp.block.Blocks.of(p.getWorld().getBlockAt(x,y,z))
                        .flatMap(org.lime.gp.block.Blocks::customOf)
                        .filter(v -> v.key.uuid().equals(block_uuid))
                        .flatMap(v -> v.list(DisplayInstance.class).findAny())
                        .ifPresent(instance -> instance.set(key, value))
        );
    }

    public interface IBlock {
        Optional<IBlockData> data();

        default IModelBlock withModel(Models.Model model) { return IModelBlock.of(data().orElse(null), model); }
        default IModelBlock withModel(Models.Model model, double distance) { return IModelBlock.of(data().orElse(null), model, distance); }

        static IBlock of(IBlockData data) { return () -> Optional.ofNullable(data); }
    }
    public interface ITileBlock extends IBlock {
        Optional<PacketPlayOutTileEntityData> packet();

        @Override default ITileModelBlock withModel(Models.Model model) { return ITileModelBlock.of(data().orElse(null), packet().orElse(null), model); }
        @Override default ITileModelBlock withModel(Models.Model model, double distance) { return ITileModelBlock.of(data().orElse(null), packet().orElse(null), model, distance); }

        static ITileBlock of(IBlockData data, PacketPlayOutTileEntityData packet) {
            return new ITileBlock() {
                @Override public Optional<IBlockData> data() { return Optional.ofNullable(data); }
                @Override public Optional<PacketPlayOutTileEntityData> packet() { return Optional.ofNullable(packet); }
            };
        }
    }
    public interface IModelBlock extends IBlock {
        double distance();
        default double distanceSquared() {
            double distance = distance();
            return distance * distance;
        }
        Optional<Models.Model> model();

        @Override default IModelBlock withModel(Models.Model model) { return this; }
        @Override default IModelBlock withModel(Models.Model model, double distance) { return this; }

        static IModelBlock of(IBlockData data, Models.Model model) {
            return new IModelBlock() {
                @Override public double distance() { return Double.POSITIVE_INFINITY; }
                @Override public Optional<IBlockData> data() { return Optional.ofNullable(data); }
                @Override public Optional<Models.Model> model() { return Optional.ofNullable(model); }
            };
        }
        static IModelBlock of(IBlockData data, Models.Model model, double distance) {
            return new IModelBlock() {
                @Override public double distance() { return distance; }
                @Override public Optional<IBlockData> data() { return Optional.ofNullable(data); }
                @Override public Optional<Models.Model> model() { return Optional.ofNullable(model); }
            };
        }
    }
    public interface ITileModelBlock extends ITileBlock, IModelBlock {
        @Override default ITileModelBlock withModel(Models.Model model) { return this; }
        @Override default ITileModelBlock withModel(Models.Model model, double distance) { return this; }

        static ITileModelBlock of(IBlockData data, PacketPlayOutTileEntityData packet, Models.Model model) {
            return new ITileModelBlock() {
                @Override public Optional<PacketPlayOutTileEntityData> packet() { return Optional.ofNullable(packet); }
                @Override public double distance() { return Double.POSITIVE_INFINITY; }
                @Override public Optional<IBlockData> data() { return Optional.ofNullable(data); }
                @Override public Optional<Models.Model> model() { return Optional.ofNullable(model); }
            };
        }
        static ITileModelBlock of(IBlockData data, PacketPlayOutTileEntityData packet, Models.Model model, double distance) {
            return new ITileModelBlock() {
                @Override public Optional<PacketPlayOutTileEntityData> packet() { return Optional.ofNullable(packet); }
                @Override public double distance() { return distance; }
                @Override public Optional<IBlockData> data() { return Optional.ofNullable(data); }
                @Override public Optional<Models.Model> model() { return Optional.ofNullable(model); }
            };
        }
    }

    public interface Displayable extends CustomTileMetadata.Uniqueable {
        Optional<IModelBlock> onDisplayAsync(Player player, World world, BlockPosition position, IBlockData data);
    }
    public interface Interactable extends CustomTileMetadata.Element { void onInteract(CustomTileMetadata metadata, BlockMarkerEventInteract event); }
    /*public interface DisplayVariable extends CustomTileMetadata.Element {
        Map<String, String> onDisplayVariable();
    }*/

///*
    public static boolean inDebugZone(BlockPosition position) {
        if (position.getY() <= 62) return false;
        if (-517 > position.getX() || position.getX() > -513) return false;
        if (-146 > position.getZ() || position.getZ() > -142) return false;
        return true;
    }
    public static boolean inDebugZone(Position position) {
        if (position.y <= 62) return false;
        if (-517 > position.x || position.x > -513) return false;
        if (-146 > position.z || position.z > -142) return false;
        return true;
    }
//*/

    private static class PacketListener {
        private static BlockPosition toPosition(int val, int chunkX, int chunkY, int chunkZ) {
            int shift = 4;
            int radix = 1 << shift;
            int mask = radix - 1;
            int x = val & mask;
            val >>>= shift;
            int z = val & mask;
            val >>>= shift;
            int y = val & mask;
            return new BlockPosition(x + chunkX * 16, y + chunkY * 16, z + chunkZ * 16);
        }
        private static synchronized <T extends IBlockData>void set(DataPaletteBlock<T> states, int index, T object) {
            states.set(index, 0, 0, object);
        }
        private static ByteBuf getWriteBuffer(byte[] buffer) {
            ByteBuf byteBuf = Unpooled.wrappedBuffer(buffer);
            byteBuf.writerIndex(0);
            return byteBuf;
        }
        private static Optional<BlockPosition> getBlockPosition(NBTTagCompound compound) {
            try { return Optional.of(new BlockPosition(compound.getInt("x"),compound.getInt("y"),compound.getInt("z"))); }
            catch (Exception e) { return Optional.empty(); }
        }

        private static Optional<IBlock> tryReplace(Player player, WorldServer world, BlockPosition position, IBlockData data) {
            return tryReplace(player, world, position, data, false);
        }
        private static Optional<IBlock> tryReplace(Player player, WorldServer world, BlockPosition position, IBlockData data, boolean debug) {
            return CacheBlockDisplay.getCacheBlock(position, world.uuid).map(v -> v.cache(player.getUniqueId()));
            /*Material bukkit;
            if (data.is(Blocks.SOUL_CAMPFIRE)) bukkit = Material.SOUL_CAMPFIRE;
            else if (data.is(Blocks.SKELETON_SKULL)) bukkit = Material.SKELETON_SKULL;
            else if (data.is(Blocks.DEAD_HORN_CORAL)) bukkit = Material.DEAD_HORN_CORAL;
            else return Optional.empty();
            return world.getBlockEntity(position, TileEntityTypes.SKULL)
                    .flatMap(org.lime.gp.block.Blocks::customOf)
                    .flatMap(v -> v.list(Displayable.class)
                            .map(_v -> _v.onDisplayAsync(player, world, position, data))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .filter(_v -> _v.data().isPresent())
                            .findAny()
                    )
                    .or(() -> switch (bukkit) {
                            case SOUL_CAMPFIRE ->
                                data.getOptionalValue(BlockCampfire.LIT)
                                        .filter(v -> !v)
                                        .map(v -> Blocks.CAMPFIRE.defaultBlockState()
                                                .setValue(BlockCampfire.LIT, false)
                                                .setValue(BlockCampfire.SIGNAL_FIRE, data.getValue(BlockCampfire.SIGNAL_FIRE))
                                                .setValue(BlockCampfire.WATERLOGGED, data.getValue(BlockCampfire.WATERLOGGED))
                                                .setValue(BlockCampfire.FACING, data.getValue(BlockCampfire.FACING))
                                        )
                                        .map(IBlock::of);
                            case DEAD_HORN_CORAL -> Optional.of(IBlock.of(Blocks.DEAD_FIRE_CORAL.defaultBlockState()
                                    .setValue(BlockCoralDead.WATERLOGGED, data.getValue(BlockCoralDead.WATERLOGGED))));
                            default -> Optional.empty();
                    });*/
        }

        private static class BlockEntity {
            private final Object handle;

            public BlockEntity(Object handle) { this.handle = handle; }
            public BlockEntity(int xz, int y, TileEntityTypes<?> type, @Nullable NBTTagCompound nbt) {
                this(ReflectionAccess.init_a_ClientboundLevelChunkPacketData_Func.invoke(xz, y, type, nbt));
            }
            public BlockEntity(BlockPosition position, TileEntityTypes<?> type, @Nullable NBTTagCompound nbt) {
                this(SectionPosition.sectionRelative(position.getX()) << 4 | SectionPosition.sectionRelative(position.getZ()), position.getY(), type, nbt);
            }
            public NBTTagCompound tag() {
                return ReflectionAccess.tag_a_ClientboundLevelChunkPacketData.get(handle);
            }
            public void tag(NBTTagCompound tag) {
                ReflectionAccess.tag_a_ClientboundLevelChunkPacketData.set(handle, tag);
            }
        }

        public static void onPacket(PacketPlayOutBlockChange packet, PacketEvent event) {
            Player player = event.getPlayer();
            WorldServer world = ((CraftWorld)player.getWorld()).getHandle();
            UUID worldUUID = world.uuid;
            tryReplace(player, world, packet.getPos(), packet.blockState)
                    .ifPresent(block -> {
                        block.data().ifPresent(data -> event.setPacket(new PacketContainer(event.getPacketType(), new PacketPlayOutBlockChange(packet.getPos(), data))));
                        if (block instanceof ITileBlock tileBlock) tileBlock.packet()
                                .ifPresent(data -> lime.invokable(new PacketInvokable<>(player, worldUUID, data, 1)));
                    });
        }
        /*public static void onPacket(PacketPlayOutMultiBlockChange packet, PacketEvent event) {
            system.Toast1<SectionPosition> section = system.toast(null);
            Short2ObjectMap<IBlockData> shorts = new Short2ObjectArrayMap<>();
            Player player = event.getPlayer();
            WorldServer world = ((CraftWorld)player.getWorld()).getHandle();
            Map<BlockPosition, CustomTileMetadata.ChunkBlockTimeout> blockMap = TimeoutData.stream(CustomTileMetadata.ChunkBlockTimeout.class)
                    .collect(Collectors.toMap(kv -> kv.val1.pos, kv -> kv.val1));
            List<BlockPosition> blocks = new ArrayList<>();
            packet.runUpdates((pos, state) -> {
                if (section.val0 == null) {
                    section.val0 = SectionPosition.of(pos);
                }
                CustomTileMetadata.ChunkBlockTimeout block = blockMap.get(pos);
                if (block == null) {
                    shorts.put(SectionPosition.sectionRelativePos(pos), state);
                } else {
                    blocks.add(pos);
                }

                shorts.put(SectionPosition.sectionRelativePos(pos), tryReplace(player, world, pos, state)
                        .flatMap(block -> {
                            if (block instanceof ITileBlock tileBlock) tileBlock.packet().ifPresent(data -> PacketManager.sendPacket(player, data));
                            return block.data();
                        })
                        .orElse(state)
                );
            });
            if (section.val0 == null) return;
            event.setPacket(new PacketContainer(event.getPacketType(), new PacketPlayOutMultiBlockChange(section.val0, shorts, packet.shouldSuppressLightUpdates())));
        }*/
        public static void onPacket(PacketPlayOutMultiBlockChange packet, PacketEvent event) {
            system.Toast1<SectionPosition> section = system.toast(null);
            Short2ObjectMap<IBlockData> shorts = new Short2ObjectArrayMap<>();
            packet.runUpdates((pos, state) -> {
                if (section.val0 == null) section.val0 = SectionPosition.of(pos);
                shorts.put(SectionPosition.sectionRelativePos(pos), state);
            });
            if (section.val0 == null || shorts.isEmpty()) return;
            long chunkID = section.val0.chunk().longKey;
            Player player = event.getPlayer();
            TimeoutData.values(CustomTileMetadata.ChunkBlockTimeout.class)
                    .forEach(timeout -> {
                        if (timeout.chunk != chunkID) return;
                        short posID = SectionPosition.sectionRelativePos(timeout.pos);
                        if (shorts.remove(posID) != null) return;
                        timeout.sync(player);
                    });
            event.setPacket(new PacketContainer(event.getPacketType(), new PacketPlayOutMultiBlockChange(section.val0, shorts, packet.shouldSuppressLightUpdates())));
        }
        /*public static void onPacket(ClientboundLevelChunkWithLightPacket packet, PacketEvent event) {
            ClientboundLevelChunkPacketData change = packet.getChunkData();
            PacketDataSerializer buffer = change.getReadBuffer();
            HashMap<system.Toast2<DataPaletteBlock<IBlockData>, Integer>, IBlockData> replaces = new HashMap<>();
            List<ChunkSection> sections = new ArrayList<>();
            HashMap<BlockPosition, BlockEntity> blockEntitiesTags = new HashMap<>();
            Player player = event.getPlayer();
            WorldServer world = ((CraftWorld)player.getWorld()).getHandle();
            int chunkX = packet.getX();
            int chunkZ = packet.getZ();
            int minSectionID = world.getMinSection();
            int maxSectionID = world.getMaxSection();
            for (int i = minSectionID; i <= maxSectionID; i++) {
                if (buffer.isReadable()) {
                    int chunkY = i;
                    ChunkSection section = new ChunkSection(chunkY, MinecraftServer.getServer().registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY));
                    section.read(buffer);
                    sections.add(section);
                    DataPaletteBlock<IBlockData> states = section.states;
                    states.forEachLocation((state, index) -> {
                        BlockPosition pos = toPosition(index, chunkX, chunkY, chunkZ);
                        tryReplace(player, world, pos, state)
                                .ifPresent(block -> {
                                    block.data().ifPresent(data -> replaces.put(system.toast(states, index), data));
                                    if (block instanceof ITileBlock tileBlock)
                                        tileBlock.packet().ifPresent(data -> blockEntitiesTags.put(pos, new BlockEntity(data.getPos(), data.getType(), data.getTag())));
                                });
                    });
                }
            }
            if (blockEntitiesTags.size() != 0) {
                List<Object> _blockEntitiesTags = ReflectionAccess.blockEntitiesData_PacketPlayOutMapChunk.get(change);
                _blockEntitiesTags.removeIf(v -> getBlockPosition(new BlockEntity(v).tag()).map(blockEntitiesTags::containsKey).orElse(false));
                _blockEntitiesTags.addAll(blockEntitiesTags.values().stream().map(v -> v.handle).toList());
            }
            if (replaces.size() == 0) return;
            replaces.forEach((key, state) -> set(key.val0, key.val1, state));
            try {
                byte[] bytes = new byte[sections.stream().mapToInt(ChunkSection::getSerializedSize).sum()];
                PacketDataSerializer serializer = new PacketDataSerializer(getWriteBuffer(bytes));
                for (ChunkSection section : sections) section.write(serializer);
                ReflectionAccess.buffer_PacketPlayOutMapChunk.set(change, bytes);
            }
            catch (Exception e) { throw new IllegalArgumentException(e); }
        }
        /*public static void onPacket(ClientboundLevelChunkWithLightPacket packet, PacketEvent event) {
            ClientboundLevelChunkPacketData change = packet.getChunkData();
            PacketDataSerializer buffer = change.getReadBuffer();
            HashMap<system.Toast2<DataPaletteBlock<IBlockData>, Integer>, IBlockData> replaces = new HashMap<>();
            List<ChunkSectionData> sections = new ArrayList<>();
            HashMap<BlockPosition, BlockEntity> blockEntitiesTags = new HashMap<>();
            Player player = event.getPlayer();
            WorldServer world = ((CraftWorld)player.getWorld()).getHandle();
            int chunkX = packet.getX();
            int chunkZ = packet.getZ();
            int minSectionID = world.getMinSection();
            int maxSectionID = world.getMaxSection();
            for (int i = minSectionID; i <= maxSectionID; i++) {
                if (buffer.isReadable()) {
                    int chunkY = i;
                    ChunkSectionData section = new ChunkSectionData(chunkY, buffer);
                    sections.add(section);
                    DataPaletteBlock<IBlockData> states = section.states;
                    states.forEachLocation((state, index) -> {
                        BlockPosition pos = toPosition(index, chunkX, chunkY, chunkZ);
                        tryReplace(player, world, pos, state)
                                .ifPresent(block -> {
                                    block.data().ifPresent(data -> replaces.put(system.toast(states, index), data));
                                    if (block instanceof ITileBlock tileBlock)
                                        tileBlock.packet().ifPresent(data -> blockEntitiesTags.put(pos, new BlockEntity(data.getPos(), data.getType(), data.getTag())));
                                });
                    });
                }
            }
            if (blockEntitiesTags.size() != 0) {
                List<Object> _blockEntitiesTags = ReflectionAccess.blockEntitiesData_PacketPlayOutMapChunk.get(change);
                _blockEntitiesTags.removeIf(v -> getBlockPosition(new BlockEntity(v).tag()).map(blockEntitiesTags::containsKey).orElse(false));
                _blockEntitiesTags.addAll(blockEntitiesTags.values().stream().map(v -> v.handle).toList());
            }
            if (replaces.size() == 0) return;
            replaces.forEach((key, state) -> set(key.val0, key.val1, state));
            try {
                ChunkSectionData.combine(sections)
                        .invoke((size, apply) -> {
                            byte[] bytes = new byte[size];
                            PacketDataSerializer serializer = new PacketDataSerializer(getWriteBuffer(bytes));
                            apply.invoke(serializer);
                            ReflectionAccess.buffer_PacketPlayOutMapChunk.set(change, bytes);
                        });
            }
            catch (Exception e) { throw new IllegalArgumentException(e); }
        }
        /*public static void onPacket(ClientboundLevelChunkWithLightPacket packet, PacketEvent event) {
            ClientboundLevelChunkPacketData change = packet.getChunkData();
            PacketDataSerializer buffer = change.getReadBuffer();
            List<ChunkSectionData> sections = new ArrayList<>();
            HashMap<BlockPosition, BlockEntity> blockEntitiesTags = new HashMap<>();
            Player player = event.getPlayer();
            WorldServer world = ((CraftWorld)player.getWorld()).getHandle();
            int chunkX = packet.getX();
            int chunkZ = packet.getZ();
            int minSectionID = world.getMinSection();
            int maxSectionID = world.getMaxSection();
            system.Toast1<Boolean> edited = system.toast(false);
            for (int i = minSectionID; i <= maxSectionID; i++) {
                if (buffer.isReadable()) {
                    int chunkY = i;
                    ChunkSectionData section = new ChunkSectionData(chunkY, buffer, (x,y,z,state) -> {
                        BlockPosition pos = new BlockPosition(x + chunkX * 16, y + chunkY * 16, z + chunkZ * 16);
                        return tryReplace(player, world, pos, state)
                                .flatMap(block -> {
                                    if (block instanceof ITileBlock tileBlock) tileBlock.packet().ifPresent(data -> blockEntitiesTags.put(pos, new BlockEntity(data.getPos(), data.getType(), data.getTag())));
                                    Optional<IBlockData> data = block.data();
                                    if (data.isPresent()) edited.val0 = true;
                                    return data;
                                })
                                .orElse(state);
                    });
                    sections.add(section);
                }
            }
            if (blockEntitiesTags.size() != 0) {
                List<Object> _blockEntitiesTags = ReflectionAccess.blockEntitiesData_PacketPlayOutMapChunk.get(change);
                _blockEntitiesTags.removeIf(v -> getBlockPosition(new BlockEntity(v).tag()).map(blockEntitiesTags::containsKey).orElse(false));
                _blockEntitiesTags.addAll(blockEntitiesTags.values().stream().map(v -> v.handle).toList());
            }
            if (edited.val0) return;

            try {
                ChunkSectionData.combine(sections)
                        .invoke((size, apply) -> {
                            byte[] bytes = new byte[size];
                            PacketDataSerializer serializer = new PacketDataSerializer(getWriteBuffer(bytes));
                            apply.invoke(serializer);
                            ReflectionAccess.buffer_PacketPlayOutMapChunk.set(change, bytes);
                        });
            }
            catch (Exception e) { throw new IllegalArgumentException(e); }
        }*/
        public static void onPacket(ClientboundLevelChunkWithLightPacket packet, PacketEvent event) {
            long chunk = ChunkCoordIntPair.asLong(packet.getX(), packet.getZ());
            Player player = event.getPlayer();
            UUID worldUUID = player.getWorld().getUID();
            TimeoutData.values(CustomTileMetadata.ChunkBlockTimeout.class)
                    .filter(v -> v.chunk == chunk && worldUUID.equals(v.worldUUID))
                    .forEach(block -> block.sync(player));
        }
        public static void onPacket(PacketPlayOutTileEntityData packet, PacketEvent event) {
            if (Optional.ofNullable(packet.getTag()).filter(v -> v.contains("lime:sended") && v.getBoolean("lime:sended")).isEmpty()) return;
            Player player = event.getPlayer();
            WorldServer world = ((CraftWorld)player.getWorld()).getHandle();
            BlockPosition pos = packet.getPos();
            packet.getType()
                    .validBlocks
                    .stream()
                    .findAny()
                    .flatMap(state -> tryReplace(player, world, pos, state.defaultBlockState()))
                    .map(v -> v instanceof ITileBlock b ? b : null)
                    .flatMap(ITileBlock::packet)
                    .ifPresent(v -> event.setPacket(new PacketContainer(event.getPacketType(), v)));
        }

        public static void onPacket(PacketPlayInUseEntity packet, PacketEvent event) {
            lime.invokeSync(() -> BlockMarkerEventInteract.execute(event.getPlayer(), packet));
        }
    }
    @EventHandler public static void interact(BlockMarkerEventInteract e) {
        org.lime.gp.block.Blocks.customOf(e.getSkull())
                .ifPresent(v -> v.list(Interactable.class).forEach(_v -> _v.onInteract(v, e)));
    }
}


































