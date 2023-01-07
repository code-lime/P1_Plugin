package org.lime.gp.block.display;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.decoration.EntityItemFrame;
import net.minecraft.world.level.block.BlockCampfire;
import net.minecraft.world.level.block.BlockCoralDead;
import net.minecraft.world.level.block.BlockSkull;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.chunk.DataPaletteBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.lime.Position;
import org.lime.core;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.block.BlockMap;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.display.DisplayInstance;
import org.lime.gp.display.DisplayManager;
import org.lime.gp.display.Displays;
import org.lime.gp.display.EditedDataWatcher;
import org.lime.gp.display.ObjectDisplay;
import org.lime.gp.lime;
import org.lime.gp.extension.PacketManager;
import org.lime.system;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class BlockDisplay implements Listener {
    public static core.element create() {
        return core.element.create(BlockDisplay.class)
                .withInit(BlockDisplay::init);
    }
    public static void init() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketListener(lime._plugin, ListenerPriority.NORMAL));
    }
    public static void reload() {
        Displays.uninitDisplay(BLOCK_MANAGER, LOD_MANAGER);
        Displays.initDisplay(BLOCK_MANAGER, LOD_MANAGER);
    }

    public static class BlockLodDisplay extends ObjectDisplay<BlockSyncDisplay, EntityItemFrame> {
        @Override public double getDistance() { return block.getDistance(); }
        @Override public Location location() { return getPosition().map(v -> v.getLocation(0.5,0.5,0.5)).orElse(null); }

        public final UUID uuid;
        public final BlockSyncDisplay block;

        @Override public boolean isFilter(Player player) {
            return getLod(player).map(v -> v.type() == LOD.TypeLOD.ItemFrame).orElse(false);
        }

        public Optional<BlocksOld.Info> getInfo() { return block.getInfo(); }
        public Optional<org.lime.Position> getPosition() { return block.getPosition(); }
        public Optional<UUID> getLodUUID(Player player) { return block.getLodUUID(player); }
        public Optional<LOD.ILOD> getLod(Player player) { return block.getLod(player); }

        protected BlockLodDisplay(UUID uuid, BlockSyncDisplay block) {
            this.uuid = uuid;
            this.block = block;
            postInit();
        }

        @Override public void update(BlockSyncDisplay block, double delta) {
            super.update(block, delta);
            invokeAll(player -> {
                UUID old_uuid = this.<UUID>keyOf(player, BlockSyncDisplay.KEY_LOD_UUID).orElse(null);
                UUID new_uuid = getLodUUID(player).orElse(null);
                if (Objects.equals(old_uuid, new_uuid)) return;
                keyOf(player, BlockSyncDisplay.KEY_LOD_UUID, new_uuid);
                sendData(player);
            });
        }

        @Override protected void editDataWatcher(Player player, EditedDataWatcher dataWatcher) {
            super.editDataWatcher(player, dataWatcher);
            getLod(player)
                    .map(v -> v instanceof LOD.ItemFrameLOD lod ? lod : null)
                    .ifPresent(v -> {
                        dataWatcher.setCustom(EditedDataWatcher.DATA_ITEM, v.nms_item);
                        dataWatcher.setCustom(EditedDataWatcher.DATA_ROTATION, v.rotation.ordinal());
                    });
        }
        @Override protected EntityItemFrame createEntity(Location location) {
            EntityItemFrame itemFrame = new EntityItemFrame(
                    ((CraftWorld)location.getWorld()).getHandle(),
                    new BlockPosition(location.getX(), location.getY(), location.getZ()),
                    EnumDirection.UP);
            itemFrame.setInvisible(true);
            itemFrame.setInvulnerable(true);
            return itemFrame;
        }
    }
    private static class BlockLodManager extends DisplayManager<UUID, BlockSyncDisplay, BlockLodDisplay> {
        @Override public boolean isAsync() { return true; }
        @Override public boolean isFast() { return true; }

        @Override public Map<UUID, BlockSyncDisplay> getData() {
            return new HashMap<>(BLOCK_MANAGER.getDisplays());
        }
        @Override public BlockLodDisplay create(UUID uuid, BlockSyncDisplay display) {
            return new BlockLodDisplay(uuid, display);
        }
    }
    public static class BlockSyncDisplay extends ObjectDisplay<VariableComponent, Marker> {
        public static final String KEY_LOD_UUID = "lod.uuid";

        @Override public double getDistance() { return LOD.maxDistance(); }
        @Override public Location location() { return getPosition().map(v -> v.getLocation(0.5,0.5,0.5)).orElse(null); }

        public final UUID uuid;
        public final VariableComponent LOD;

        public Optional<BlocksOld.Info> getInfo() {
            return BlockMap.byUUID(uuid);
        }
        public Optional<org.lime.Position> getPosition() {
            return BlockMap.positionByUUID(uuid);
        }
        public Optional<UUID> getLodUUID(Player player) {
            return keyOf(player, KEY_LOD_UUID);
        }
        public Optional<LOD.ILOD> getLod(Player player) {
            return getLodUUID(player).map(v -> LOD.getLodMap().getOrDefault(v, null));
        }

        protected BlockSyncDisplay(UUID uuid, VariableComponent lod) {
            this.uuid = uuid;
            this.LOD = lod;
            postInit();
        }

        @Override protected void sendData(Player player, boolean child) {
            getPosition().ifPresent(pos -> BlockMap.markDirty(player, pos));
            super.sendData(player, child);
        }
        @Override public void update(VariableComponent lod, double delta) {
            super.update(lod, delta);
            boolean log = lod.info().position().map(v -> v.y == 64).orElse(false);
            invokeDistanceAll((player, distance) -> {
                lod.getLOD(distance).ifPresentOrElse(element -> {
                    UUID lod_uuid = this.<UUID>keyOf(player, KEY_LOD_UUID).orElse(null);
                    if (element.uuid.equals(lod_uuid)) return;
                    keyOf(player, KEY_LOD_UUID, element.uuid);
                    sendData(player);
                }, () -> hide(player));
            });
        }

        @Override public void hide(Player player) {
            super.hide(player);
            getPosition().ifPresent(pos -> BlockMap.markDirty(player, pos));
        }

        @Override protected void editDataWatcher(Player player, EditedDataWatcher dataWatcher) {
            super.editDataWatcher(player, dataWatcher);
            getLod(player)
                    .map(v -> v instanceof LOD.ItemFrameLOD lod ? lod : null)
                    .ifPresent(v -> dataWatcher.setCustom(EditedDataWatcher.DATA_ITEM, v.nms_item));
        }
        @Override protected Marker createEntity(Location location) {
            return new Marker(EntityTypes.MARKER, ((CraftWorld)location.getWorld()).getHandle());
        }
    }
    public static class BlockSyncManager extends DisplayManager<UUID, VariableComponent, BlockSyncDisplay> {
        @Override public boolean isAsync() { return true; }
        @Override public boolean isFast() { return true; }

        @Override public Map<UUID, VariableComponent> getData() {
            return BlockMap.blocks()
                    .map(v -> system.toast(v.uuid, v.instance(VariableComponent.class)))
                    .filter(v -> v.val1.isPresent())
                    .collect(Collectors.toMap(v -> v.val0, v -> v.val1.get()));
        }
        @Override public BlockSyncDisplay create(UUID uuid, VariableComponent lod) {
            return new BlockSyncDisplay(uuid, lod);
        }
    }

    public static final BlockSyncManager BLOCK_MANAGER = new BlockSyncManager();
    private static final BlockLodManager LOD_MANAGER = new BlockLodManager();

    private static class PacketListener extends PacketAdapter {
        private static final IBlockData DEFAULT_SKULL = CraftMagicNumbers
                .getBlock(Material.SKELETON_SKULL)
                .defaultBlockState()
                .setValue(BlockSkull.ROTATION, 0);
        public static BlockPosition toPosition(int val, int chunkX, int chunkY, int chunkZ) {
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
        public static Optional<InfoComponent.IReplace.Result> tryReplace(InfoComponent.IReplace.Input input) {
            switch (input.data.getBukkitMaterial()) {
                case SOUL_CAMPFIRE: {
                    if (!input.data.hasProperty(BlockCampfire.LIT)) return Optional.empty();
                    if (input.data.getValue(BlockCampfire.LIT)) return Optional.empty();
                    return Optional.of(new InfoComponent.IReplace.Result(CraftMagicNumbers
                            .getBlock(Material.CAMPFIRE)
                            .defaultBlockState()
                            .setValue(BlockCampfire.LIT, false)
                            .setValue(BlockCampfire.SIGNAL_FIRE, input.data.getValue(BlockCampfire.SIGNAL_FIRE))
                            .setValue(BlockCampfire.WATERLOGGED, input.data.getValue(BlockCampfire.WATERLOGGED))
                            .setValue(BlockCampfire.FACING, input.data.getValue(BlockCampfire.FACING))
                    ));
                }
                case SKELETON_SKULL: {
                    BlockPosition position = input.position();
                    return Optional.of(
                            BlockMap.byPosition(new Position(input.world(), position.getX(), position.getY(), position.getZ()))
                                    .map(v -> v.replace(input))
                                    .orElse(new InfoComponent.IReplace.Result(CraftMagicNumbers
                                            .getBlock(Material.SKELETON_SKULL)
                                            .defaultBlockState()
                                            .setValue(BlockSkull.ROTATION, input.data.getValue(BlockSkull.ROTATION))
                                    ))
                    );
                }
                case DEAD_HORN_CORAL: return Optional.of(new InfoComponent.IReplace.Result(CraftMagicNumbers
                        .getBlock(Material.DEAD_FIRE_CORAL)
                        .defaultBlockState()
                        .setValue(BlockCoralDead.WATERLOGGED, input.data.getValue(BlockCoralDead.WATERLOGGED))));
                default: return Optional.empty();
            }
        }
        public static synchronized <T extends IBlockData>void set(DataPaletteBlock<T> states, int index, T object) {
            states.set(index, 0, 0, object);
        }
        private static ByteBuf getWriteBuffer(byte[] buffer) {
            ByteBuf byteBuf = Unpooled.wrappedBuffer(buffer);
            byteBuf.writerIndex(0);
            return byteBuf;
        }
        private static Optional<BlockPosition> getBlockPosition(NBTTagCompound compound) {
            try {
                return Optional.of(new BlockPosition(compound.getInt("x"),compound.getInt("y"),compound.getInt("z")));
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        private static class BlockEntity {
            private final Object handle;

            public BlockEntity(Object handle) {
                this.handle = handle;
            }
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

        public static HashMap<PacketType, system.Action2<PacketListener, PacketEvent>> events = system.map.<PacketType, system.Action2<PacketListener, PacketEvent>>of()
                .add(PacketType.Play.Server.BLOCK_CHANGE, (listener, event) -> {
                    PacketPlayOutBlockChange change = (PacketPlayOutBlockChange)event.getPacket().getHandle();
                    Player player = event.getPlayer();
                    tryReplace(new InfoComponent.IReplace.Input(player, change.getPos(), change.blockState))
                            .ifPresent(result -> {
                                if (result.data == null) return;
                                event.setPacket(new PacketContainer(event.getPacketType(), new PacketPlayOutBlockChange(change.getPos(), result.data)));
                                lime.nextTick(() -> result.packet(change.getPos()).ifPresent(v -> PacketManager.sendPacket(player, v)));
                            });
                })
                .add(PacketType.Play.Server.MULTI_BLOCK_CHANGE, (listener, event) -> {
                    PacketPlayOutMultiBlockChange change = (PacketPlayOutMultiBlockChange)event.getPacket().getHandle();
                    SectionPosition sectionPos = ReflectionAccess.sectionPos_PacketPlayOutMultiBlockChange.get(change);
                    short[] positions = ReflectionAccess.positions_PacketPlayOutMultiBlockChange.get(change);
                    IBlockData[] states = ReflectionAccess.states_PacketPlayOutMultiBlockChange.get(change);
                    Player player = event.getPlayer();
                    for(int i = 0; i < states.length; ++i) {
                        short position = positions[i];
                        BlockPosition pos = new BlockPosition(sectionPos.relativeToBlockX(position), sectionPos.relativeToBlockY(position), sectionPos.relativeToBlockZ(position));
                        Optional<InfoComponent.IReplace.Result> _result = tryReplace(new InfoComponent.IReplace.Input(player, pos, states[i]));
                        if (_result.isEmpty()) continue;
                        InfoComponent.IReplace.Result result = _result.get();
                        if (result.data == null) continue;
                        states[i] = result.data;
                        lime.nextTick(() -> result.packet(pos).ifPresent(v -> PacketManager.sendPacket(player, v)));
                    }
                })
                .add(PacketType.Play.Server.MAP_CHUNK, (listener, event) -> {
                    ClientboundLevelChunkWithLightPacket packet = (ClientboundLevelChunkWithLightPacket)event.getPacket().getHandle();
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
                        int chunkY = i;
                        ChunkSection section = new ChunkSection(chunkY, MinecraftServer.getServer().registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY));
                        section.read(buffer);
                        sections.add(section);
                        DataPaletteBlock<IBlockData> states = section.states;
                        states.forEachLocation((state, index) -> {
                            BlockPosition pos = toPosition(index, chunkX, chunkY, chunkZ);
                            InfoComponent.IReplace.Input input = new InfoComponent.IReplace.Input(player, pos, state);
                            tryReplace(input).ifPresent(result -> {
                                if (result.data == null) return;
                                replaces.put(system.toast(states, index), result.data);
                                result.nbt(input.position()).ifPresent(v -> blockEntitiesTags.put(pos, new BlockEntity(pos, TileEntityTypes.CAMPFIRE, v)));
                            });
                        });
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
                })
                .add(PacketType.Play.Server.TILE_ENTITY_DATA, (listener, event) -> {
                    PacketPlayOutTileEntityData data = (PacketPlayOutTileEntityData)event.getPacket().getHandle();
                    Player player = event.getPlayer();
                    BlockPosition pos = data.getPos();
                    tryReplace(new InfoComponent.IReplace.Input(player, pos, DEFAULT_SKULL)).map(v -> v.compound).ifPresent(v -> {
                        event.setPacket(new PacketContainer(
                                PacketType.Play.Server.TILE_ENTITY_DATA,
                                ReflectionAccess.init_PacketPlayOutTileEntityData.newInstance(pos, data.getType(), v)
                        ));
                    });
                })
                .build();
        public PacketListener(Plugin plugin, ListenerPriority listenerPriority) {
            super(plugin, listenerPriority, events.keySet().toArray(new PacketType[0]));
        }

        @Override public void onPacketSending(PacketEvent event) {
            system.Action2<PacketListener, PacketEvent> callback = events.getOrDefault(event.getPacketType(), null);
            if (callback == null) return;
            callback.invoke(this, event);
        }
    }
}




