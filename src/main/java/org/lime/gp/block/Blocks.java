package org.lime.gp.block;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.coreprotect.event.AsyncBlockInfoEvent;
import net.coreprotect.event.IsContainerEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.commands.arguments.blocks.ArgumentTileLocation;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.LimeKey;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_19_R3.persistence.CraftPersistentDataContainer;
import org.bukkit.craftbukkit.v1_19_R3.persistence.CraftPersistentDataTypeRegistry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.ServerOperator;
import org.lime.Position;
import org.lime.core;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.OtherGenericInstance;
import org.lime.gp.block.component.display.CacheBlockDisplay;
import org.lime.gp.block.component.display.block.IBlock;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.display.instance.TickTimeInfo;
import org.lime.gp.block.component.list.MultiBlockComponent;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.extension.LimePersistentDataType;
import org.lime.gp.extension.Modify;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.BlockSetting;
import org.lime.gp.lime;
import org.lime.gp.module.PopulateLootEvent;
import org.lime.gp.module.ThreadPool;
import org.lime.gp.module.TimeoutData;
import org.lime.system;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class Blocks implements Listener {
    public static core.element create() {
        return core.element.create(Blocks.class)
                .withInstance()
                .withInit(Blocks::init)
                .<JsonObject>addConfig("blocks", v -> v.withDefault(new JsonObject()).withInvoke(Blocks::config))
                .addCommand("set.block", v -> v.withCheck(ServerOperator::isOp)
                        .withUsage("/set.block [x:int,~] [y:int,~] [z:int,~] [block:key]")
                        .withTab((sender, args) -> switch(args.length) {
                            case 1,2,3 -> Collections.singletonList(Optional.ofNullable(sender instanceof Player player ? player.getTargetBlockExact(5) : null)
                                    .map(p -> p.getLocation().toVector())
                                    .map(p -> p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ())
                                    .orElse("~"));
                            case 4 -> creators.keySet();
                            default -> Collections.emptyList();
                        })
                        .withExecutor((sender, args) -> {
                            if (args.length < 4) return false;
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
                            creator(args[3]).ifPresentOrElse(info -> {
                                World world = player_location.map(Location::getWorld).orElse(lime.MainWorld);
                                setBlock(new Position(world, x, y, z), info);
                                sender.sendMessage("Block '"+info.getKey()+"' set in "+x+" "+y+" "+z);
                            }, () -> sender.sendMessage("Block '"+args[3]+"' not founded!"));
                            return true;
                        })
                )
                .addCommand("variable.block", v -> v.withCheck(ServerOperator::isOp)
                        .withUsage("/set.block [x:int,~] [y:int,~] [z:int,~] [block:key]")
                        .withTab((sender, args) -> switch(args.length) {
                            case 1,2,3 -> Collections.singletonList(Optional.ofNullable(sender instanceof Player player ? player.getTargetBlockExact(5) : null)
                                    .map(p -> p.getLocation().toVector())
                                    .map(p -> p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ())
                                    .orElse("~"));
                            default -> Collections.emptyList();
                        })
                        .withExecutor((sender, args) -> {
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
                            Blocks.of(player_location.map(Location::getWorld).orElse(lime.MainWorld).getBlockAt(x,y,z))
                                    .flatMap(Blocks::customOf)
                                    .flatMap(_v -> _v.list(DisplayInstance.class).findFirst())
                                    .map(DisplayInstance::getAll)
                                    .ifPresentOrElse(variable ->
                                            sender.sendMessage(Component.text("Variables of block in "+x+" "+y+" "+z+":\n{")
                                                    .append(Component.join(JoinConfiguration.separator(Component.text(",")), variable.entrySet()
                                                            .stream()
                                                            .map(kv -> Component.empty()
                                                                    .append(Component.text("\""))
                                                                    .append(Component.text(kv.getKey()).color(NamedTextColor.AQUA))
                                                                    .append(Component.text("\":\""))
                                                                    .append(Component.text(kv.getValue()).color(NamedTextColor.GREEN))
                                                                    .append(Component.text("\""))
                                                            )
                                                            .toList()
                                                    ))
                                                    .append(Component.text("}"))),
                                            () -> sender.sendMessage("Block in "+x+" "+y+" "+z+" not have display variables")
                                    );
                            return true;
                        })
                );
    }

    public static final HashMap<String, BlockInfo> creators = new HashMap<>();
    private static final HashMap<String, BlockInfo> defaultBlocks = new HashMap<>();
    private static final HashMap<String, JsonObject> overrides = new HashMap<>();
    private static final HashMap<Material, List<BlockInfo.Replacer<?>>> replaceBlocks = new HashMap<>();

    public static Optional<BlockInfo> creator(String key) {
        return Optional.ofNullable(creators.get(key));
    }
    public static void addDefaultBlocks(BlockInfo creator) { defaultBlocks.put(creator.getKey(), creator); }

    public static String getBlockKey(Block block) {
        return of(block)
                .flatMap(Blocks::customOf)
                .map(v -> v.list(OtherGenericInstance.class)
                        .map(OtherGenericInstance::owner)
                        .flatMap(Optional::stream)
                        .map(Blocks::customOf)
                        .flatMap(Optional::stream)
                        .findFirst()
                        .orElse(v))
                .map(v -> v.key.type())
                .orElseGet(() -> block.getType().name());
    }

    public static void config(JsonObject json) {
        HashMap<String, BlockInfo> creators = new HashMap<>(defaultBlocks);
        JsonElement modify_json = json.remove("MODIFY_LIST");
        json = lime.combineParent(json, true, false);
        if (modify_json != null) {
            HashMap<String, Map<String, JsonObject>> modify_map = system.map.<String, Map<String, JsonObject>>of()
                    .add(modify_json.getAsJsonObject().entrySet(), Map.Entry::getKey, kv ->
                            system.map.<String, JsonObject>of()
                                    .add(lime.combineParent(kv.getValue().getAsJsonObject(), false, false).entrySet(), Map.Entry::getKey, _kv -> _kv.getValue().getAsJsonObject())
                                    .build()
                    )
                    .build();
            Modify.modify(json, modify_map);
        }

        overrides.clear();
        json.entrySet().forEach((kv) -> {
            overrides.put(kv.getKey(), kv.getValue().getAsJsonObject());
            creators.put(kv.getKey(), new BlockInfo(kv.getKey(), kv.getValue().getAsJsonObject()));
        });
        replaceBlocks.clear();
        Blocks.creators.clear();
        Blocks.creators.putAll(creators);
        Blocks.creators.values().forEach(creator -> replaceBlocks.putAll(creator.replaces));
        CacheBlockDisplay.reset();
    }
    public static final system.LockToast2<Long, Long> nextAsyncTimes = system.toast(0L, 0L).lock();
    public static final system.LockToast1<TickTimeInfo> deltaTime = system.toast(new TickTimeInfo()).lock();
    private static final system.LockToast2<TickTimeInfo,TickTimeInfo> lastDeltaTime = system.toast(new TickTimeInfo(),new TickTimeInfo()).lock();
    
    public static void init() {
        AnyEvent.addEvent("blocks.json", AnyEvent.type.owner_console, b -> b.createParam(v -> v, overrides::keySet), (p, key) -> lime.logOP("Block '" + key + "':\n" + overrides.getOrDefault(key, null)));
        setBlockData(0.0f);
        AnyEvent.addEvent("delta.show.blocks", AnyEvent.type.owner_console, p -> lime.logOP(Component.text("[Blocks] DeltaShow:\n" + nextAsyncTimes.call(v -> String.join("\n",
                " - Last call: " + system.formatCalendar(system.getMoscowTime(v.val0), true),
                " - Delta call: " + v.val1 + "ms")))
                .append(Component.text("\n - Tick info:\n    ").append(lastDeltaTime.get0().toComponent()))
                .append(Component.text("\n - Average info:\n    ").append(lastDeltaTime.get1().toComponent()))
        ));
        system.Toast1<Long> tick = system.toast(0L);
        ThreadPool.Type.Async.executeRepeat(() -> {
            long _tick = tick.val0;
            tick.val0 = _tick + 1;
            deltaTime.edit0(time -> {
                lastDeltaTime.set0(time);
                lastDeltaTime.edit1(v -> {
                    if (v.count > 500) v = new TickTimeInfo();
                    v.append(time);
                    return v;
                });
                return new TickTimeInfo();
            });
            TimeoutData.allValues(CustomTileMetadata.ChunkBlockTimeout.class).forEach(info -> info.last_metadata.onTickAsync(_tick));
        }, nextAsyncTimes);
    }
    private static void setBlockData(float speed) {
        net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY.forEach(data -> {
            if (data.getBukkitMaterial() != Material.SKELETON_SKULL) return;
            ReflectionAccess.destroySpeed_BlockData.set(data, speed);
        });
    }
    /*private static void setBlockData(float speed, boolean uncache) {
        net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY.forEach(data -> {
            if (data.getBukkitMaterial() != Material.SKELETON_SKULL) return;
            ReflectionAccess.destroySpeed_BlockData.set(data, speed);
            if (uncache) {
                ReflectionAccess.cache_BlockData.set(data, null);
                ReflectionAccess.shapeExceedsCube_BlockData.set(data, true);
                ReflectionAccess.opacityIfCached_BlockData.set(data, -1);
            }
        });
        ReflectionAccess.destroyTime_BlockData.set(ReflectionAccess.properties_BlockBase.get(net.minecraft.world.level.block.Blocks.SKELETON_SKULL), speed);
    }*/

    @EventHandler public static void onAsync(AsyncBlockInfoEvent e) {
        LimeKey.getKey(e.getContainer(), LimeKey.KeyType.CUSTOM_BLOCK)
                .map(LimeKey::type)
                .map(type -> OtherGenericInstance.tryGetOwnerType(e.getContainer()).orElse(type))
                .map(String::toLowerCase)
                .ifPresent(e::setDisplayName);
    }
    @EventHandler public static void onAsync(IsContainerEvent e) {
        if (e.isContainerBlock()) return;
        e.setContainerBlock(of(e.getBlock()).map(Blocks::customOf).isPresent());
    }

    //private static final MinecraftKey TICKER = new MinecraftKey("lime", "ticker");

    private static Optional<TileMetadata> ticker(TileEntityLimeSkull skull) {
        if (skull == null) return Optional.empty();
        if (skull.getMetadata() instanceof TileMetadata metadata) return Optional.of(metadata);
        TileMetadata metadata = skull.customKey()
                .map(_v -> (TileMetadata) new CustomTileMetadata(_v, skull))
                .orElseGet(() -> TileMetadata.empty(skull));
        skull.setMetadata(metadata);
        return Optional.of(metadata);
    }

    public static Optional<TileEntityLimeSkull> of(Block block) {
        return block instanceof CraftBlock _block
                ? Optional.ofNullable(_block.getHandle().getBlockEntity(_block.getPosition()) instanceof TileEntityLimeSkull skull ? skull : null)
                : Optional.empty();
    }

    public static Optional<TileMetadata> of(TileEntityLimeSkull skull) {
        return Optional.ofNullable(skull)
                .flatMap(Blocks::ticker);
    }
    public static Optional<TileMetadata> of(TileEntitySkull skull) {
        return Optional.ofNullable(skull)
                .map(v -> v instanceof TileEntityLimeSkull _v ? _v : null)
                .flatMap(Blocks::ticker);
    }

    public static Optional<CustomTileMetadata> customOf(TileEntityLimeSkull skull) {
        return Optional.ofNullable(skull)
                .flatMap(Blocks::ticker)
                .map(v -> v instanceof CustomTileMetadata _v ? _v : null);
    }
    public static Optional<CustomTileMetadata> customOf(TileEntitySkull skull) {
        return Optional.ofNullable(skull)
                .map(v -> v instanceof TileEntityLimeSkull _v ? _v : null)
                .flatMap(Blocks::ticker)
                .map(v -> v instanceof CustomTileMetadata _v ? _v : null);
    }

    private static final ConcurrentHashMap<String, NamespacedKey> keys = new ConcurrentHashMap<>();
    public static NamespacedKey ofKey(String key) {
        return keys.compute(key, (k,v) -> v == null ? new NamespacedKey(lime._plugin, k) : v);
    }

    public static TileEntityLimeSkull setBlock(Position position, BlockInfo type) {
        return setBlock(position, type, Collections.emptyMap());
    }
    public static TileEntityLimeSkull setBlock(Position position, BlockInfo type, Map<String, JsonObject> data) {
        CraftPersistentDataContainer container = new CraftPersistentDataContainer(new CraftPersistentDataTypeRegistry());
        LimeKey.of(type.getKey()).setKey(container, LimeKey.KeyType.CUSTOM_BLOCK);
        data.forEach((key, value) -> container.set(ofKey(key), LimePersistentDataType.JSON_OBJECT, value));

        NBTTagCompound nbt = new NBTTagCompound();
        nbt.put("PublicBukkitValues", container.toTagCompound());

        WorldServer world = ((CraftWorld)position.world).getHandle();
        BlockPosition blockPosition = new BlockPosition(position.x, position.y, position.z);

        world.removeBlock(blockPosition, false);
        new ArgumentTileLocation(
                net.minecraft.world.level.block.Blocks.SKELETON_SKULL.defaultBlockState(),
                Set.of(),
                nbt
        ).place(world, blockPosition, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);

        return world.getBlockEntity(blockPosition, TileEntityTypes.SKULL)
                .map(v -> v instanceof TileEntityLimeSkull s ? s : null)
                .filter(v -> {
                    v.removeMetadata();
                    return true;
                })
                .orElse(null);
    }

    @EventHandler public static void preTick(TileEntitySkullPreTickEvent e) {
        of(e.getSkull()).ifPresent(v -> v.onTick(e.info()));
    }
    @EventHandler public static void remove(TileEntitySkullEventRemove e) {
        of(e.getSkull()).ifPresent(v -> v.onRemove(e));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH) public static void loot(PopulateLootEvent e) {
        Vec3D pos = e.getOrDefault(PopulateLootEvent.Parameters.Origin, null);
        if (pos == null || !e.has(PopulateLootEvent.Parameters.BlockState)) return;
        if (!(e.getOrDefault(PopulateLootEvent.Parameters.BlockEntity, null) instanceof TileEntityLimeSkull skull)) return;
        of(skull).ifPresent(v -> v.onLoot(e));
    }
    @EventHandler public static void damage(BlockDamageEvent e) {
        if (!(e.getBlock() instanceof CraftBlock block)) return;
        Optional.ofNullable(block.getHandle().getBlockEntity(block.getPosition()) instanceof TileEntityLimeSkull skull ? skull : null)
                .flatMap(Blocks::of)
                .ifPresent(v -> v.onDamage(e));
    }

    private static void piston(BlockPistonEvent e, Stream<Position> blocks) {
        if (blocks
                .map(v -> ((CraftWorld)v.world).getHandle().getBlockEntity(new BlockPosition(v.x, v.y, v.z)) instanceof TileEntityLimeSkull skull ? skull : null)
                .filter(Objects::nonNull)
                .anyMatch(v -> v.customKey().isPresent()))
            e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public static void piston(BlockPistonExtendEvent e) {
        piston(e, e.getBlocks().stream().map(Position::new));
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public static void piston(BlockPistonRetractEvent e) {
        piston(e, e.getBlocks().stream().map(Position::new));
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public static void place(BlockPlaceEvent e) {
        if (!e.canBuild()) return;
        Block block = e.getBlock();
        ItemStack item = e.getItemInHand();
        Items.getOptional(BlockSetting.class, item)
                .ifPresent(setting -> {
                    InfoComponent.Rotation.Value rotation = InfoComponent.Rotation.of(e.getPlayer().getLocation().getDirection(), setting.rotation.keySet());
                    Optional.ofNullable(setting.rotation.get(rotation))
                            .flatMap(Blocks::creator)
                            .filter(creator -> creator.component(MultiBlockComponent.class).map(v -> v.isCan(block, rotation)).orElse(true))
                            .map(creator -> creator.setMultiBlock(e.getPlayer(), new Position(block), setting.blockArgs(item), rotation))
                            .ifPresentOrElse(list -> {}, () -> e.setBuild(false));
                });
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public static void physics(BlockPhysicsEvent e) {
        if (!replaceBlocks.containsKey(e.getChangedType())) return;
        Location location = e.getBlock().getLocation();
        lime.nextTick(() -> {
            Block block = location.getBlock();
            Optional.ofNullable(replaceBlocks.get(block.getType()))
                    .ifPresent(replaces -> {
                        for (BlockInfo.Replacer<?> replace : replaces)
                            if (replace.replace(block))
                                return;
                    });
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public static void on(PlayerInteractEvent e) {
        Optional.ofNullable(e.getClickedBlock())
                .flatMap(Blocks::of)
                .map(skull -> CacheBlockDisplay.getCacheBlock(skull.getBlockPos(), skull.getLevel().getWorld().getUID())
                    .map(v -> v.cache(e.getPlayer().getUniqueId()))
                    .flatMap(IBlock::data)
                    .map(display -> display.getDestroySpeed(skull.getLevel(), skull.getBlockPos()))
                    .orElse(1.0F)
                )
                .ifPresent(Blocks::setBlockData);
    }
}











