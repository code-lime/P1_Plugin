package org.lime.gp.entity;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.protocol.game.PacketPlayInUseEntity;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.LimeKey;
import net.minecraft.world.entity.EntityLimeMarker;
import net.minecraft.world.entity.EntityMarkerEventDestroy;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftMarker;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.persistence.PersistentDataContainer;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.entity.event.EntityMarkerEventInteract;
import org.lime.gp.entity.event.EntityMarkerEventTick;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.extension.LimePersistentDataType;
import org.lime.gp.extension.Modify;
import org.lime.gp.lime;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

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
                );
    }
    private static final MinecraftServer server = MinecraftServer.getServer();
    public static Stream<EntityLimeMarker> all() {
        return Streams.stream(server.getAllLevels().iterator())
                .flatMap(world -> Streams.stream(world.getEntities().getAll()))
                .map(v -> v instanceof EntityLimeMarker marker ? marker : null)
                .filter(Objects::nonNull);
    }
    public static void init() {
        lime.timer().setSync().withLoopTicks(1).withCallbackTicks(ms -> {
            double delta = ms / 1000.0;
            server.getAllLevels().forEach(world -> world.getEntities().getAll().forEach(entity -> {
                if (entity instanceof EntityLimeMarker marker)
                    EntityMarkerEventTick.execute(world, marker, delta);
            }));
        }).run();
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(lime._plugin, PacketType.Play.Client.USE_ENTITY) {
            @Override public void onPacketReceiving(PacketEvent event) {
                PacketPlayInUseEntity use = (PacketPlayInUseEntity)event.getPacket().getHandle();
                lime.invokeSync(() -> EntityMarkerEventInteract.execute(event.getPlayer(), use));
            }
        });
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

    private static final HashMap<String, EntityInfo> creators = new HashMap<>();
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
        })).getHandle());
    }

    @EventHandler public static void tick(EntityMarkerEventTick e) { of(e.getMarker()).ifPresent(v -> v.onTick(e)); }
    @EventHandler public static void destroy(EntityMarkerEventDestroy e) { of(e.getMarker()).ifPresent(v -> v.onDestroy(e)); }
    @EventHandler public static void interact(EntityMarkerEventInteract e) {
        of(e.getMarker())
                .map(v -> Execute.action(e.isAttack() ? v::onDamage : v::onInteract))
                .ifPresent(v -> v.invoke(e));
    }
}



























