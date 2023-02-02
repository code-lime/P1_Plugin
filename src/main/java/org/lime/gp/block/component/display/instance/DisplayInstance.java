package org.lime.gp.block.component.display.instance;

import com.google.gson.JsonPrimitive;

import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BlockSkullShapeInfo;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.entity.TileEntitySkullEventRemove;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.lime.Position;
import org.lime.core;
import org.lime.display.Models;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.BlockDisplay;
import org.lime.gp.block.component.display.CacheBlockDisplay;
import org.lime.gp.block.component.display.block.IBlock;
import org.lime.gp.block.component.display.block.IModelBlock;
import org.lime.gp.block.component.display.display.BlockModelDisplay;
import org.lime.gp.block.component.display.event.ChunkCoordCache;
import org.lime.gp.block.component.display.invokable.BlockDirtyInvokable;
import org.lime.gp.block.component.display.invokable.BlockUpdateInvokable;
import org.lime.gp.block.component.display.partial.BlockPartial;
import org.lime.gp.block.component.display.partial.FramePartial;
import org.lime.gp.block.component.display.partial.ModelPartial;
import org.lime.gp.block.component.display.partial.Partial;
import org.lime.gp.block.component.list.DisplayComponent;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.extension.ProxyMap;
import org.lime.gp.lime;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.module.TimeoutData;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class DisplayInstance extends BlockInstance implements CustomTileMetadata.Shapeable, CustomTileMetadata.Tickable, CustomTileMetadata.AsyncTickable, CustomTileMetadata.FirstTickable, CustomTileMetadata.Removeable {
    private static int TIMEOUT_TICKS = 20;
    public static core.element create() {
        return core.element.create(DisplayInstance.class)
                .<JsonPrimitive>addConfig("config", v -> v
                        .withParent("display.timeout")
                        .withDefault(new JsonPrimitive(20))
                        .withInvoke(_v -> TIMEOUT_TICKS = _v.getAsInt())
                );
    }

    private final system.LockToast1<Long> variableIndex = system.toast(1L).lock();
    private final ConcurrentHashMap<String, String> variables = new ConcurrentHashMap<>();
    private final Map<String, String> unmodifiableVariables = Collections.unmodifiableMap(variables);
    private final ConcurrentHashMap<UUID, Partial> partials = new ConcurrentHashMap<>();

    @Override public DisplayComponent component() { return (DisplayComponent)super.component(); }

    private system.LockToast1<InfoComponent.Rotation.Value> rotationCache = system.<InfoComponent.Rotation.Value>toast(null).lock();

    public void variableDirty() {
        variableIndex.edit0(v -> v + 1);
        rotationCache.set0(get("rotation").flatMap(ExtMethods::parseInt).map(InfoComponent.Rotation.Value::ofAngle).orElse(null));
        saveData();
    }

    public DisplayInstance(DisplayComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
    }
    public void set(String key, String value) {
        if (Objects.equals(variables.put(key, value), value)) return;
        variableDirty();
    }
    public void set(Map<String, String> variables) {
        this.variables.putAll(variables);
        variableDirty();
    }
    public void modify(system.Func1<Map<String, String>, Boolean> modifyFunc) {
        if (!modifyFunc.invoke(variables)) return;
        variableDirty();
    }
    public Optional<String> get(String key) { return Optional.ofNullable(variables.get(key)); }
    public Map<String, String> getAll() {
        return unmodifiableVariables;
    }
    public long variableIndex() { return variableIndex.get0(); }

    public Optional<InfoComponent.Rotation.Value> getRotation() {
        return Optional.ofNullable(rotationCache.get0());
    }

    public Optional<Partial> getPartial(int distanceChunk, Map<String, String> variables) {
        return Optional.ofNullable(component().partials.get(distanceChunk)).map(v -> v.partial(variables));
    }

    public static final class DisplayMap extends TimeoutData.ITimeout {
        public final Map<UUID, ItemFrameDisplayObject> frameMap = new HashMap<>();
        public final Map<BlockModelDisplay.BlockModelKey, ModelDisplayObject> modelMap = new HashMap<>();
        public DisplayMap(Map<UUID, ItemFrameDisplayObject> itemFrame, Map<BlockModelDisplay.BlockModelKey, ModelDisplayObject> models) {
            super(TIMEOUT_TICKS);
            this.frameMap.putAll(itemFrame);
            this.modelMap.putAll(models);
        }
    }

    public record ItemFrameDisplayObject(Location location, ItemStack item, InfoComponent.Rotation.Value rotation, UUID index) {
        public static ItemFrameDisplayObject of(Location location, ItemStack item, InfoComponent.Rotation.Value rotation, UUID index) {
            return new ItemFrameDisplayObject(location, item, rotation, index);
        }
    }
    public record ModelDisplayObject(Location location, Set<UUID> viewers, Models.Model model, Map<String, Object> data) {
        public boolean hasViewer(UUID uuid) { return viewers.contains(uuid); }
        public static ModelDisplayObject of(Location location, Models.Model model, Map<String, Object> data) {
            return new ModelDisplayObject(location, ConcurrentHashMap.newKeySet(), model, data);
        }
    }

    @Override public void read(JsonObjectOptional json) {
        variables.clear();
        json.forEach((key, value) -> value.getAsString()
                .or(() -> value.getAsNumber().map(Object::toString))
                .or(() -> value.getAsBoolean().map(Object::toString))
                .ifPresent(v -> variables.put(key, v)));
        variableDirty();
    }
    @Override public system.json.builder.object write() { return system.json.object().add(variables, k -> k, v -> v); }

    private Partial orSync(CustomTileMetadata metadata, UUID playerUUID, Player player, Map<String, String> variable, Partial _new, Partial _old) {
        if (Objects.equals(_new, _old)) return _new;
        TileEntityLimeSkull skull = metadata.skull;
        WorldServer world = (WorldServer)skull.getLevel();
        UUID worldUUID = world.uuid;
        BlockPosition position = skull.getBlockPos();
        if (_new instanceof BlockPartial block) cacheDisplay.put(playerUUID, block.getDynamicDisplay(position, variable));
        else cacheDisplay.remove(playerUUID);
        lime.invokable(new BlockUpdateInvokable(player, worldUUID, position, 1));
        lime.invokable(new BlockUpdateInvokable(player, worldUUID, position, 20));
        return _new;
    }

    public static boolean sendBlock(Player player, Position position) {
        BlockPosition pos = new BlockPosition(position.x, position.y, position.z);
        UUID worldUUID = position.world.getUID();
        lime.invokable(new BlockUpdateInvokable(player, worldUUID, pos, 1));
        lime.invokable(new BlockUpdateInvokable(player, worldUUID, pos, 20));
        return true;
    }

    public static boolean markDirtyBlock(net.minecraft.world.level.World world, BlockPosition position) {
        if (!(world instanceof WorldServer worldServer)) return false;
        lime.invokable(new BlockDirtyInvokable(worldServer, position, 1));
        lime.invokable(new BlockDirtyInvokable(worldServer, position, 20));
        return true;
    }
    public static boolean markDirtyBlock(Position position) {
        return markDirtyBlock(((CraftWorld)position.world).getHandle(), new BlockPosition(position.x, position.y, position.z));
    }

    private final ConcurrentHashMap<UUID, IBlock> cacheDisplay = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Object> animationData = new ConcurrentHashMap<>();
    @Override public void onFirstTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        CacheBlockDisplay.trySetCacheBlock(metadata.skull, CacheBlockDisplay.ICacheInfo.of(cacheDisplay));
        markDirtyBlock(event.getWorld(), event.getPos());
        lime.once(() -> markDirtyBlock(event.getWorld(), event.getPos()), 0.5);
    }
    private static final int TOTAL_UPDATE_TICKS = 20 * 2 * 60;
    private int update_ticks = -1;
    private final system.LockToast1<IBlockData> last_state = system.toast(Blocks.AIR.defaultBlockState()).lock();

    private long lastUpdateDirtyIndex = -1;
    private static final ConcurrentLinkedQueue<UUID> dirtyQueue = new ConcurrentLinkedQueue<>();
    private static final List<UUID> cachedDirtyQueue = new ArrayList<>();
    public static void appendDirtyQueue(UUID uuid) {
        dirtyQueue.add(uuid);
    }

    private static long lastTickID = -1;
    private final HashMap<UUID, ItemFrameDisplayObject> frameMap = new HashMap<>();
    private final HashMap<BlockModelDisplay.BlockModelKey, ModelDisplayObject> modelMap = new HashMap<>();
    @Override public void onAsyncTick(CustomTileMetadata metadata, long tick) {
        if (lastTickID != tick) {
            lastTickID = tick;
            UUID uuid;
            cachedDirtyQueue.clear();
            while ((uuid = dirtyQueue.poll()) != null) cachedDirtyQueue.add(uuid);
        }
        TickTimeInfo tickTimeInfo = org.lime.gp.block.Blocks.deltaTime.get0();
        tickTimeInfo.resetTime();
        tickTimeInfo.calls++;

        long currUpdateDirtyIndex = variableIndex();

        Set<UUID> onlineUUIDs = EntityPosition.onlinePlayers.keySet();
        frameMap.keySet().removeIf(uuid -> !onlineUUIDs.contains(uuid));
        modelMap.values().removeIf(data -> {
            data.viewers.removeIf(uuid -> !onlineUUIDs.contains(uuid));
            return data.viewers.isEmpty();
        });
        partials.keySet().removeIf(uuid -> !onlineUUIDs.contains(uuid));
        tickTimeInfo.filter_ns += tickTimeInfo.nextTime();

        Collection<UUID> users;
        if (currUpdateDirtyIndex != lastUpdateDirtyIndex) {
            users = onlineUUIDs;
            lastUpdateDirtyIndex = currUpdateDirtyIndex;
            modelMap.clear();
            frameMap.clear();
        } else {
            users = cachedDirtyQueue;
            if (!users.isEmpty()) {
                frameMap.keySet().removeIf(uuid -> users.contains(uuid));
                modelMap.values().removeIf(data -> {
                    data.viewers.removeIf(uuid -> users.contains(uuid));
                    return data.viewers.isEmpty();
                });
            }
        }
        if (users.isEmpty()) {
            if (frameMap.size() == 0 && modelMap.size() == 0) TimeoutData.remove(unique(), DisplayMap.class);
            else TimeoutData.put(unique(), DisplayMap.class, new DisplayMap(frameMap, modelMap));
            tickTimeInfo.users_ns += tickTimeInfo.nextTime();
            return;
        }

        tickTimeInfo.users_ns += tickTimeInfo.nextTime();

        Map<String, String> variables = getAll();

        tickTimeInfo.variables1_ns += tickTimeInfo.nextTime();

        TileEntityLimeSkull skull = metadata.skull;
        net.minecraft.world.level.World handle_world = skull.getLevel();
        CraftWorld world = handle_world.getWorld();
        IBlockData state = last_state.get0();
        Location block_location = metadata.location(0.5,0.5,0.5);
        BlockPosition block_position = skull.getBlockPos();
        Vector pos = block_location.toVector();
        ChunkCoordCache.Cache blockCoord = ChunkCoordCache.Cache.of(block_position, world);

        tickTimeInfo.variables2_ns += tickTimeInfo.nextTime();

        int angle = getRotation().orElse(InfoComponent.Rotation.Value.ANGLE_0).angle;
        HashMap<Player, Integer> shows = new HashMap<>();
        boolean isControl = CacheBlockDisplay.isConcurrent(block_position, world.getUID());

        tickTimeInfo.variables3_ns += tickTimeInfo.nextTime();
        
        if (isControl) {
            users.forEach(uuid -> {
                Player player = EntityPosition.onlinePlayers.get(uuid);
                if (player == null) {
                    partials.remove(uuid);
                    return;
                }
                ChunkCoordCache.getCoord(uuid).ifPresentOrElse(cache -> {
                    int distanceChunk = cache.distance(blockCoord);
                    partials.compute(uuid, (k, v) -> {
                        Partial partial = cache.world() != world
                            ? orSync(metadata, uuid, player, variables, null, v == null ? null : v)
                            : orSync(metadata, uuid, player, variables, getPartial(distanceChunk, variables).orElse(null), v == null ? null : v);
                        if (partial == null) return null;
                        if (partial instanceof FramePartial frame && frame.show) frameMap.put(uuid, ItemFrameDisplayObject.of(pos.toLocation(world), frame.nms(variables), frame.rotation, frame.uuid));
                        if (partial instanceof ModelPartial model) model.model().ifPresent(_model -> modelMap.computeIfAbsent(
                                new BlockModelDisplay.BlockModelKey(metadata.key.uuid(), metadata.position(), _model.unique, unique()),
                                _k -> ModelDisplayObject.of(pos.toLocation(world, angle, 0), _model, animationData)
                        ).viewers.add(uuid));
                        shows.put(player, distanceChunk);
                        return partial;
                    });
                }, () -> partials.remove(uuid));
            });
            tickTimeInfo.check_ns += tickTimeInfo.nextTime();
            tickTimeInfo.partial_ns += tickTimeInfo.nextTime();
        }
        else {
            users.forEach(uuid -> {
                Player player = EntityPosition.onlinePlayers.get(uuid);
                if (player == null) return;
                ChunkCoordCache.getCoord(uuid)
                    .ifPresent(cache -> shows.put(player, cache.distance(blockCoord)));
            });
            tickTimeInfo.check_ns += tickTimeInfo.nextTime();
            tickTimeInfo.partial_ns += tickTimeInfo.nextTime();
        }

        metadata.list(BlockDisplay.Displayable.class)
                .forEach(displayable -> shows.forEach((player, distanceChunk) -> displayable.onDisplayAsync(player, handle_world, block_position, state)
                        .filter(v -> distanceChunk <= v.distanceChunk())
                        .flatMap(IModelBlock::model)
                        .ifPresent(_model -> modelMap.computeIfAbsent(
                                new BlockModelDisplay.BlockModelKey(metadata.key.uuid(), metadata.position(), _model.unique, displayable.unique()),
                                k -> ModelDisplayObject.of(pos.toLocation(world, angle, 0), _model, animationData)
                        ).viewers.add(player.getUniqueId()))
                ));
        
        tickTimeInfo.metadata_ns += tickTimeInfo.nextTime();

        if (frameMap.size() == 0 && modelMap.size() == 0) TimeoutData.remove(unique(), DisplayMap.class);
        else TimeoutData.put(unique(), DisplayMap.class, new DisplayMap(frameMap, modelMap));

        tickTimeInfo.apply_ns += tickTimeInfo.nextTime();
    }
    private final ProxyMap<String, String> proxyVariables = ProxyMap.of(this.variables);
    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        update_ticks = (update_ticks + 1) % TOTAL_UPDATE_TICKS;
        if (update_ticks == 0) markDirtyBlock(event.getWorld(), event.getPos());
        last_state.set0(event.getState());
        component().animationTick(getAll(), this.proxyVariables, animationData);
        if (proxyVariables.checkDirty(true)) variableDirty();
    }
    public void reshow() {
        partials.clear();
        variableDirty();

        TimeoutData.remove(unique(), DisplayMap.class);
        TileEntityLimeSkull skull = metadata().skull;
        UUID block_uuid = metadata().key.uuid();
        BlockDisplay.MODEL_MANAGER.getDisplays()
                .values()
                .forEach(display -> {
                    if (block_uuid.equals(display.key.block_uuid()))
                        display.hideAll();
                });
        BlockDisplay.ITEM_FRAME_MANAGER.getDisplays()
                .values()
                .forEach(display -> {
                    if (block_uuid.equals(display.block_uuid))
                        display.hideAll();
                });
        markDirtyBlock(skull.getLevel(), skull.getBlockPos());
    }
    @Override public @Nullable VoxelShape onShape(CustomTileMetadata metadata, BlockSkullShapeInfo event) {
        return getPartial(0, new UnmodifiableMergeMap<>(getAll(), Collections.singletonMap("shape", "this"))).orElse(null) instanceof CustomTileMetadata.Shapeable shapeable
            ? shapeable.onShape(metadata, event)
            : null;
    }
    @Override public void onRemove(CustomTileMetadata metadata, TileEntitySkullEventRemove event) {
        TimeoutData.remove(unique(), DisplayMap.class);
        cacheDisplay.clear();
    }
}



























