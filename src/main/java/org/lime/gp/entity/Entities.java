package org.lime.gp.entity;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketPlayInSteerVehicle;
import net.minecraft.network.protocol.game.PacketPlayInUseEntity;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.LimeKey;
import net.minecraft.world.entity.EntityLimeMarker;
import net.minecraft.world.entity.EntityMarkerEventDestroy;
import net.minecraft.world.item.EggSpawnEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftMarker;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftLocation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.persistence.PersistentDataContainer;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.component.display.instance.TickTimeInfo;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.entity.component.display.instance.DisplayInstance;
import org.lime.gp.entity.event.EntityMarkerEventInput;
import org.lime.gp.entity.event.EntityMarkerEventInteract;
import org.lime.gp.entity.event.EntityMarkerEventTick;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.extension.LimePersistentDataType;
import org.lime.gp.extension.Modify;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.EntitySetting;
import org.lime.gp.lime;
import org.lime.gp.module.EntityOwner;
import org.lime.gp.module.ThreadPool;
import org.lime.gp.module.TimeoutData;
import org.lime.gp.module.loot.Parameters;
import org.lime.gp.module.loot.PopulateLootEvent;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Execute;
import org.lime.system.map;
import org.lime.system.toast.LockToast1;
import org.lime.system.toast.LockToast2;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast1;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class Entities implements Listener {
    public static CoreElement create() {
        return CoreElement.create(Entities.class)
                .withInit(Entities::init)
                .withInstance()
                .<JsonObject>addConfig("entities", v -> v.withDefault(new JsonObject()).withInvoke(Entities::config))
                .addCommand("spawn.entity", v -> v.withCheck(ServerOperator::isOp)
                        .withUsage("/spawn.entity [x:double,~] [y:double,~] [z:double,~] [entity:key]")
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
                            Double x = ExtMethods.parseDouble(args[0]).or(() -> player_location.map(Location::getX)).orElse(null);
                            if (x == null) {
                                sender.sendMessage("Value '"+args[0]+"' of argument 'x' is not supported!");
                                return true;
                            }
                            Double y = ExtMethods.parseDouble(args[1]).or(() -> player_location.map(Location::getY)).orElse(null);
                            if (y == null) {
                                sender.sendMessage("Value '"+args[1]+"' of argument 'y' is not supported!");
                                return true;
                            }
                            Double z = ExtMethods.parseDouble(args[2]).or(() -> player_location.map(Location::getZ)).orElse(null);
                            if (z == null) {
                                sender.sendMessage("Value '"+args[2]+"' of argument 'z' is not supported!");
                                return true;
                            }
                            creator(args[3]).ifPresentOrElse(info -> {
                                World world = player_location.map(Location::getWorld).orElse(lime.MainWorld);
                                spawn(new Location(world, x, y, z), info);
                                sender.sendMessage("Entity '"+info.getKey()+"' spawned in "+x+" "+y+" "+z);
                            }, () -> sender.sendMessage("Entity '"+args[3]+"' not founded!"));
                            return true;
                        })
                )
                .addCommand("variable.entity", v -> v.withCheck(ServerOperator::isOp)
                        .withUsage("/variable.entity [uuid]")
                        .withTab((sender, args) -> switch (args.length) {
                            case 1 -> sender instanceof Player player
                                    ? Stream.concat(Stream.of("[uuid]"),
                                            player.getLocation()
                                                    .getNearbyEntitiesByType(Marker.class, 10)
                                                    .stream()
                                                    .map(Entity::getUniqueId)
                                                    .map(UUID::toString)).toList()
                                    : Collections.singletonList("[uuid]");
                            default -> Collections.emptyList();
                        })
                        .withExecutor((sender, args) -> {
                            if (args.length != 1) return false;
                            String uuid = args[0];
                            ExtMethods.parseUUID(uuid)
                                    .map(Bukkit::getEntity)
                                    .map(e -> e instanceof Marker m ? m : null)
                                    .flatMap(Entities::of)
                                    .flatMap(Entities::customOf)
                                    .flatMap(_v -> _v.list(DisplayInstance.class).findFirst())
                                    .map(DisplayInstance::getAll)
                                    .ifPresentOrElse(variable ->
                                                    sender.sendMessage(Component.text("Variables of entity "+uuid+":\n{\n")
                                                            .append(Component.join(JoinConfiguration.separator(Component.text(",\n")), variable.entrySet()
                                                                    .stream()
                                                                    .sorted(Comparator.comparing(Map.Entry::getKey))
                                                                    .map(kv -> Component.empty()
                                                                            .append(Component.text("  "))
                                                                            .append(Component.text("\""))
                                                                            .append(Component.text(kv.getKey()).color(NamedTextColor.AQUA))
                                                                            .append(Component.text("\":\""))
                                                                            .append(Component.text(kv.getValue()).color(NamedTextColor.GREEN))
                                                                            .append(Component.text("\""))
                                                                    )
                                                                    .toList()
                                                            ))
                                                            .append(Component.text("\n}"))),
                                            () -> sender.sendMessage("Entity "+uuid+" not have display variables"));
                            return true;
                        })
                );
    }
    private static final MinecraftServer server = MinecraftServer.getServer();
    public static Stream<EntityLimeMarker> all() {
        return Streams.stream(server.getAllLevels().iterator())
                .flatMap(world -> Streams.stream(world.getEntities().getAll()))
                .map(v -> v instanceof EntityLimeMarker marker ? marker : null)
                .filter(Objects::nonNull);
    }

    public static final LockToast1<TickTimeInfo> deltaTime = Toast.lock(new TickTimeInfo());
    public static final LockToast2<Long, Long> nextAsyncTimes = Toast.lock(0L, 0L);

    public static void init() {
        AnyEvent.addEvent("entity.kill", AnyEvent.type.owner_console, v -> v
                        .createParam(UUID::fromString, "[entity_uuid:uuid]")
                        .createParam("loot", "force"),
                (p, entity_uuid, action) -> Optional.ofNullable(Bukkit.getEntity(entity_uuid))
                        .map(v -> v instanceof Marker marker ? marker : null)
                        .flatMap(Entities::of)
                        .flatMap(Entities::customOf)
                        .ifPresent(metadata -> {
                            switch (action) {
                                case "loot" -> metadata.destroyWithLoot(v -> v);
                                case "force" -> metadata.destroy();
                            }
                        })
        );
        lime.timer().setSync().withLoopTicks(1).withCallbackTicks(ms -> {
            double delta = ms / 1000.0;
            server.getAllLevels().forEach(world -> world.getEntities().getAll().forEach(entity -> {
                if (entity instanceof EntityLimeMarker marker)
                    EntityMarkerEventTick.execute(world, marker, delta);
            }));
        }).run();

        Toast1<Long> tick = Toast.of(0L);
        ThreadPool.Type.Async.executeRepeat(() -> {
            long _tick = tick.val0;
            tick.val0 = _tick + 1;
            TimeoutData.allValues(CustomEntityMetadata.EntityTimeout.class).forEach(info -> info.lastMetadata.onTickAsync(_tick));
        }, nextAsyncTimes);

        PacketManager.adapter()
                .add(PacketPlayInUseEntity.class, (packet, event) ->
                        lime.invokeSync(() -> EntityMarkerEventInteract.execute(event.getPlayer(), packet)))
                .add(PacketPlayInSteerVehicle.class, (packet, event) ->
                        lime.invokeSync(() -> EntityMarkerEventInput.execute(event.getPlayer(), packet)))
                .listen();
    }
    public static void config(JsonObject json) {
        HashMap<String, EntityInfo> creators = new HashMap<>(defaultBlocks);
        JsonElement modify_json = json.remove("MODIFY_LIST");
        json = lime.combineParent(json, true, false);
        if (modify_json != null) {
            HashMap<String, Map<String, JsonObject>> modify_map = map.<String, Map<String, JsonObject>>of()
                    .add(modify_json.getAsJsonObject().entrySet(), Map.Entry::getKey, kv ->
                            map.<String, JsonObject>of()
                                    .add(lime.combineParent(kv.getValue().getAsJsonObject(), false, false).entrySet(), Map.Entry::getKey, _kv -> _kv.getValue().getAsJsonObject())
                                    .build()
                    )
                    .build();
            Modify.modify(json, modify_map);
        }

        overrides.clear();
        json.entrySet().forEach((kv) -> {
            overrides.put(kv.getKey(), kv.getValue().getAsJsonObject());
            creators.put(kv.getKey(), new EntityInfo(kv.getKey(), kv.getValue().getAsJsonObject()));
        });
        Entities.creators.clear();
        Entities.creators.putAll(creators);
    }

    public static final HashMap<String, EntityInfo> creators = new HashMap<>();
    private static final HashMap<String, EntityInfo> defaultBlocks = new HashMap<>();
    private static final HashMap<String, JsonObject> overrides = new HashMap<>();

    public static Optional<EntityInfo> creator(String key) {
        return Optional.ofNullable(creators.get(key));
    }
    public static void addDefaultEntities(EntityInfo creator) { defaultBlocks.put(creator.getKey(), creator); }

    private static final MinecraftKey TICKER = new MinecraftKey("lime", "ticker");

    private static Optional<EntityMetadata> ticker(EntityLimeMarker marker) {
        return Optional.ofNullable(marker)
                .map(v -> v.getMetadata(TICKER)
                        .map(_v -> _v instanceof EntityMetadata metadata ? metadata : null)
                        .orElseGet(() -> {
                            EntityMetadata metadata = marker.customKey()
                                    .map(_v -> (EntityMetadata) new CustomEntityMetadata(_v, marker))
                                    .orElseGet(() -> EntityMetadata.empty(marker));
                            v.setMetadata(TICKER, metadata);
                            return metadata;
                        })
                );
    }

    public static Optional<EntityLimeMarker> of(Marker marker) {
        return marker instanceof CraftMarker _block
                ? Optional.ofNullable(_block.getHandle() instanceof EntityLimeMarker __marker ? __marker : null)
                : Optional.empty();
    }

    public static Optional<EntityMetadata> of(EntityLimeMarker marker) {
        return Optional.ofNullable(marker)
                .flatMap(Entities::ticker);
    }
    public static Optional<EntityMetadata> of(net.minecraft.world.entity.Marker marker) {
        return Optional.ofNullable(marker)
                .map(v -> v instanceof EntityLimeMarker _v ? _v : null)
                .flatMap(Entities::ticker);
    }

    public static Optional<CustomEntityMetadata> customOf(EntityLimeMarker marker) {
        return Optional.ofNullable(marker)
                .flatMap(Entities::ticker)
                .map(v -> v instanceof CustomEntityMetadata _v ? _v : null);
    }
    public static Optional<CustomEntityMetadata> customOf(net.minecraft.world.entity.Marker marker) {
        return Optional.ofNullable(marker)
                .map(v -> v instanceof EntityLimeMarker _v ? _v : null)
                .flatMap(Entities::ticker)
                .map(v -> v instanceof CustomEntityMetadata _v ? _v : null);
    }

    private static final ConcurrentHashMap<String, NamespacedKey> keys = new ConcurrentHashMap<>();
    public static NamespacedKey ofKey(String key) {
        return keys.compute(key, (k,v) -> v == null ? new NamespacedKey(lime._plugin, k) : v);
    }

    public static EntityLimeMarker spawn(Location location, EntityInfo type) {
        return spawn(location, type, Collections.emptyMap());
    }
    public static EntityLimeMarker spawn(Location location, EntityInfo type, Map<String, JsonObject> data) {
        return (EntityLimeMarker)(((CraftMarker)location.getWorld().spawn(location, Marker.class, _marker -> {
            PersistentDataContainer container = _marker.getPersistentDataContainer();
            new LimeKey(_marker.getUniqueId(), type.getKey()).setKey(container, LimeKey.KeyType.CUSTOM_ENTITY);
            data.forEach((key, value) -> container.set(ofKey(key), LimePersistentDataType.JSON_OBJECT, value));
            if (_marker instanceof CraftMarker marker)
                marker.getHandle().setYRot(location.getYaw());
        })).getHandle());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public static void spawn(EggSpawnEvent e) {
        ItemStack item = e.getItem();
        if (item != null && Items.hasIDByItem(item)) {
            if (!(e.getPlayer() instanceof EntityPlayer player) || !Items.getOptional(EntitySetting.class, item)
                    .flatMap(setting -> Entities.creator(setting.entity)
                            .flatMap(info -> UserRow.getBy(player.getUUID())
                                    .map(row -> {
                                        BlockPosition pos = e.getPos();
                                        Location location = CraftLocation.toBukkit(Vec3D.atBottomCenterOf(pos), e.getWorld().getWorld(), player.getBukkitYaw(), player.getXRot());
                                        var data = setting.entityArgs(item.asBukkitMirror());
                                        EntityLimeMarker marker = spawn(location, info, data);
                                        e.setOverride(marker);
                                        EntityOwner.setOwner(marker.getBukkitEntity(), row);
                                        return true;
                                    })))
                    .orElse(false))
                e.setCancelled(true);
        }
        /*
        e.setCancelled(true);
        if (e.)
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
        */
    }

    @EventHandler public static void tick(EntityMarkerEventTick e) { of(e.getMarker()).ifPresent(v -> v.onTick(e)); }
    @EventHandler public static void destroy(EntityMarkerEventDestroy e) { of(e.getMarker()).ifPresent(v -> v.onDestroy(e)); }
    @EventHandler public static void interact(EntityMarkerEventInteract e) {
        of(e.getMarker())
                .map(v -> Execute.action(e.isAttack() ? v::onDamage : v::onInteract))
                .ifPresent(v -> v.invoke(e));
    }
    @EventHandler public static void input(EntityMarkerEventInput e) { of(e.getMarker()).ifPresent(v -> v.onInput(e)); }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH) public static void loot(PopulateLootEvent e) {
        e.getOptional(Parameters.ThisEntity)
                .map(v -> v instanceof EntityLimeMarker marker ? marker : null)
                .flatMap(Entities::customOf)
                .ifPresent(metadata -> metadata.onLoot(e));
    }
}



























