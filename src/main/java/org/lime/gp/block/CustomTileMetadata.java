package org.lime.gp.block;

import com.google.gson.JsonPrimitive;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.LimeKey;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.entity.TileEntitySkullEventRemove;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.permissions.ServerOperator;
import org.lime.Position;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.display.BlockDisplay;
import org.lime.gp.block.component.display.CacheBlockDisplay;
import org.lime.gp.block.component.display.block.IBlock;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.lime;
import org.lime.gp.module.TimeoutData;
import org.lime.gp.module.loot.PopulateLootEvent;
import org.lime.plugin.CoreElement;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast1;
import org.lime.system.toast.Toast2;
import org.lime.system.utils.IterableUtils;
import org.lime.system.utils.RandomUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class CustomTileMetadata extends TileMetadata {
    public static Position DEBUG_BLOCK = null;
    public static int TILE_DELTA = 1;
    public static CoreElement create() {
        return CoreElement.create(CustomTileMetadata.class)
                .<JsonPrimitive>addConfig("config", v -> v
                        .withParent("tile.timeout.delta")
                        .withDefault(new JsonPrimitive(TILE_DELTA))
                        .withInvoke(_v -> TILE_DELTA = _v.getAsInt())
                )
                .addCommand("tmp.block.debug", v -> v.withCheck(ServerOperator::isOp)
                        .withUsage("/set.block [x:int,~] [y:int,~] [z:int,~]")
                        .withTab((sender, args) -> switch(args.length) {
                            case 1,2,3 -> Collections.singletonList(Optional.ofNullable(sender instanceof Player player ? player.getTargetBlockExact(5) : null)
                                    .map(p -> p.getLocation().toVector())
                                    .map(p -> p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ())
                                    .orElse("~"));
                            default -> Collections.emptyList();
                        })
                        .withExecutor((sender, args) -> {
                            if (args.length == 0) {
                                DEBUG_BLOCK = null;
                                sender.sendMessage("Disabled!");
                                return true;
                            }
                            if (args.length < 3) return false;
                            Optional<Location> player_location = Optional.ofNullable(sender instanceof Player p ? p : null).map(Entity::getLocation);
                            Integer x = ExtMethods.parseInt(args[0]).or(() -> player_location.map(Location::getBlockX)).orElse(null);
                            if (x == null) {
                                sender.sendMessage("Value '"+args[0]+"' of argument 'x' is not supported!");
                                return true;
                            }
                            Integer y = ExtMethods.parseInt(args[1]).or(() -> player_location.map(Location::getBlockY)).orElse(null);
                            if (y == null) {
                                sender.sendMessage("Value '"+args[1]+"' of argument 'y' is not supported!");
                                return true;
                            }
                            Integer z = ExtMethods.parseInt(args[2]).or(() -> player_location.map(Location::getBlockZ)).orElse(null);
                            if (z == null) {
                                sender.sendMessage("Value '"+args[2]+"' of argument 'z' is not supported!");
                                return true;
                            }
                            DEBUG_BLOCK = new Position(player_location.map(Location::getWorld).orElse(lime.MainWorld), x, y, z);
                            sender.sendMessage("Enabled block debug: " + DEBUG_BLOCK);
                            return true;
                        })
                );
    }

    public interface Element { }
    public interface Uniqueable extends Element { UUID unique(); }
    public interface Childable extends Element { Stream<? extends Element> childs(); }

    public interface Tickable extends Element { void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event); }
    public interface AsyncTickable extends Element { void onAsyncTick(CustomTileMetadata metadata, long tick); }
    public interface FirstTickable extends Element { void onFirstTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event); }
    public interface Removeable extends Element { void onRemove(CustomTileMetadata metadata, TileEntitySkullEventRemove event); }
    public interface Interactable extends Element { EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event); }
    public interface Shapeable extends Element { @Nullable VoxelShape onShape(CustomTileMetadata metadata, BlockSkullShapeInfo event); }
    public interface Lootable extends Element { void onLoot(CustomTileMetadata metadata, PopulateLootEvent event); }
    public interface Damageable extends Element { void onDamage(CustomTileMetadata metadata, BlockDamageEvent event); }
    public interface Destroyable extends Element { void onDestroy(CustomTileMetadata metadata, BlockSkullDestroyInfo event); }
    public interface Placeable extends Element { IBlockData onPlace(CustomTileMetadata metadata, BlockSkullPlaceInfo event); }

    public final LimeKey key;
    public final ConcurrentHashMap<String, BlockInstance> instances = new ConcurrentHashMap<>();
    public BlockInfo info = null;

    private long loadIndex = -1;

    public CustomTileMetadata(LimeKey key, TileEntityLimeSkull skull) {
        super(skull);
        this.key = key;

        Blocks.creator(key.type()).ifPresentOrElse(info -> {
                    if (info.getLoadIndex() != loadIndex) {
                        instances.entrySet().removeIf(kv -> {
                            kv.getValue().saveData();
                            return true;
                        });
                        list_buffer.clear();
                        loadIndex = info.getLoadIndex();
                        this.info = info;
                    }
                    info.components.forEach((_key, component) -> {
                        if (!(component instanceof ComponentDynamic<?, ?> dynamicComponent)) return;
                        instances.compute(_key, (k,v) -> {
                            if (v == null) {
                                this.list_buffer.clear();
                                v = dynamicComponent.createInstance(this).loadData();
                                v.saveData();
                            }
                            return v;
                        });
                    });
                }, () -> instances.entrySet().removeIf(kv -> {
                    kv.getValue().saveData();
                    return true;
                }));
    }

    public static Stream<Element> childsAndThis(Childable childable) {
        return Stream.concat(Stream.of(childable), childable.childs().flatMap(v -> v instanceof Childable c ? childsAndThis(c) : Stream.of(v)));
    }
    private final ConcurrentHashMap<Class<?>, List<?>> list_buffer = new ConcurrentHashMap<>();
    
    @SuppressWarnings("unchecked")
    public <T extends Element>Stream<T> list(Class<T> tClass) {
        List<?> _data = list_buffer.get(tClass);
        if (_data != null) return ((List<T>)_data).stream();
        Stream<Object> stream;
        if (info != null) {
            Stream.Builder<Object> builder = Stream.builder();
            info.components.forEach((key, value) -> {
                builder.add(value);
                BlockInstance instance = instances.get(key);
                if (instance == null) return;
                builder.add(instance);
            });
            stream = builder.build();
            //stream = Stream.concat(builder.build(), info.components.values().stream());
        } else {
            stream = Stream.empty();
        }
        Toast1<Boolean> isChildable = Toast.of(false);
        List<T> data = stream
                .flatMap(v -> {
                    if (v instanceof Childable childable) {
                        isChildable.val0 = true;
                        return childsAndThis(childable);
                    }
                    return Stream.of(v);
                })
                .filter(tClass::isInstance)
                .map(v -> (T)v)
                .toList();
        if (!isChildable.val0) list_buffer.put(tClass, data);
        return data.stream();
    }

    private boolean isFirst = true;
    public record ChunkGroup(UUID worldUUID, long chunk) implements TimeoutData.TKeyedGroup<Toast2<UUID, Long>> {
        public ChunkGroup(UUID worldUUID, BlockPosition pos) {
            this(worldUUID, ChunkCoordIntPair.asLong(pos));
        }

        @Override public Toast2<UUID, Long> groupID() { return Toast.of(worldUUID, chunk); }
    }
    public static class ChunkBlockTimeout extends TimeoutData.IGroupTimeout {
        public final UUID worldUUID;
        public final BlockPosition pos;
        public final CustomTileMetadata lastMetadata;
        private final Position position;

        public ChunkBlockTimeout(CustomTileMetadata lastMetadata, Position position) {
            this.worldUUID = position.world.getUID();
            this.pos = new BlockPosition(position.x, position.y, position.z);
            this.position = position;
            this.lastMetadata = lastMetadata;
        }

        public boolean sync(Player player) { return DisplayInstance.sendBlock(player, position); }
        public boolean markDirty() { return DisplayInstance.markDirtyBlock(position); }
    }

    private void asyncTickTimeout() {
        ChunkBlockTimeout timeout = new ChunkBlockTimeout(this, position());
        boolean firstSync = TimeoutData.put(new ChunkGroup(timeout.worldUUID, timeout.pos), key.uuid(), ChunkBlockTimeout.class, timeout);
        if (firstSync) timeout.markDirty();
    }

    private final int TIMEOUT_TICK_OFFSET = RandomUtils.rand(0, 10000);
    private boolean isInit = true;
    @Override public void onTick(TileEntitySkullTickInfo event) {
        interactLocker.entrySet().forEach(kv -> kv.setValue(kv.getValue() - 1));
        interactLocker.entrySet().removeIf(kv -> kv.getValue() < 0);
        Blocks.creator(key.type())
                .ifPresentOrElse(info -> {
                    if (info.getLoadIndex() != loadIndex) {
                        instances.entrySet().removeIf(kv -> {
                            kv.getValue().saveData();
                            return true;
                        });
                        this.loadIndex = info.getLoadIndex();
                        this.info = info;
                        this.isFirst = true;
                        this.list_buffer.clear();
                    }
                    info.components.forEach((key, component) -> {
                        if (!(component instanceof ComponentDynamic<?, ?> dynamicComponent)) return;
                        instances.compute(key, (k,v) -> {
                            if (v == null) {
                                this.list_buffer.clear();
                                v = dynamicComponent.createInstance(this).loadData();
                            }
                            return v;
                        });
                    });

                    if (isInit || (MinecraftServer.currentTick + TIMEOUT_TICK_OFFSET) % TILE_DELTA == 0) {
                        asyncTickTimeout();
                        isInit = false;
                    }
                    //if (debug) lime.logOP("Tick debug block!");
                    boolean firstTick = isFirst;
                    if (firstTick) isFirst = false;
                    list(Tickable.class).forEach(tickable -> tickable.onTick(this, event));
                    if (firstTick) list(FirstTickable.class).forEach(tickable -> tickable.onFirstTick(this, event));
                }, () -> instances.entrySet().removeIf(kv -> {
                    kv.getValue().saveData();
                    return true;
                }));
    }
    @Override public void onTickAsync(long tick) {
        asyncTickTimeout();
        list(AsyncTickable.class).forEach(v -> v.onAsyncTick(this, tick));
    }
    @Override public void onRemove(TileEntitySkullEventRemove event) {
        list(Removeable.class).forEach(v -> v.onRemove(this, event));
        TileEntityLimeSkull skull = event.getSkull();
        BlockPosition pos = skull.getBlockPos();
        CacheBlockDisplay.resetCacheBlock(skull);
        TimeoutData.remove(new ChunkGroup(skull.getLevel().getWorld().getUID(), pos), key.uuid(), ChunkBlockTimeout.class);
    }

    private final Map<Integer, Integer> interactLocker = new HashMap<>();
    @Override public @Nullable EnumInteractionResult onInteract(BlockSkullInteractInfo event) {
        EntityHuman player = event.player();
        int playerID = player.getId();
        if (event.hand() == EnumHand.MAIN_HAND) interactLocker.remove(playerID);
        else if (interactLocker.remove(playerID) != null) return EnumInteractionResult.CONSUME;
        EnumInteractionResult result = EnumInteractionResult.CONSUME;
        for (Interactable interactable : IterableUtils.iterable(list(Interactable.class)))  {
            EnumInteractionResult _result = interactable.onInteract(this, event);
            if (!_result.consumesAction()) continue;
            result = _result;
            break;
        }
        if (result.consumesAction()) player.containerMenu.sendAllDataToRemote();
        if (event.hand() == EnumHand.MAIN_HAND && result.consumesAction()) interactLocker.put(playerID, 2);
        return null;
    }
    @Override public @Nullable VoxelShape onShape(BlockSkullShapeInfo event) {
        VoxelShape last = null;
        for (Shapeable shapeable : IterableUtils.iterable(list(Shapeable.class))) {
            VoxelShape newShape = shapeable.onShape(this, event);
            last = newShape == null ? last : newShape;
        }
        return last;
    }


    @Override public @Nullable IBlockData onState(BlockSkullStateInfo e) {
        if (!(e.player().getBukkitEntity() instanceof CraftPlayer player)) return null;
        return list(BlockDisplay.Displayable.class)
                .map(_v -> _v.onDisplayAsync(player, skull.getLevel(), skull.getBlockPos(), skull.getBlockState()))
                .flatMap(Optional::stream)
                .filter(_v -> _v.data().isPresent())
                .findFirst()
                .flatMap(IBlock::data)
                .orElse(null);
    }
    @Override public void onLoot(PopulateLootEvent event) {
        event.setItems(Collections.emptyList());
        list(Lootable.class).forEach(v -> v.onLoot(this, event));
    }
    @Override public void onDamage(BlockDamageEvent event) { list(Damageable.class).forEach(v -> v.onDamage(this, event)); }
    @Override public void onDestroy(BlockSkullDestroyInfo event) { list(Destroyable.class).forEach(v -> v.onDestroy(this, event)); }
    @Override public @Nullable IBlockData onPlace(BlockSkullPlaceInfo event) {
        IBlockData last = null;
        for (Placeable placeable : IterableUtils.iterable(list(Placeable.class))) last = placeable.onPlace(this, event);
        return last;
    }

    @Override public String toString() { return key.uuid()+":"+skull.getBlockPos(); }
}















