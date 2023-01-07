package org.lime.gp.module;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityLiving;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.lime.core;
import org.lime.gp.lime;
import org.lime.system;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EntityPosition {
    public static core.element create() {
        return core.element.create(EntityPosition.class)
                .withInit(EntityPosition::init);
    }
    public static void init() {
        lime.repeatTicks(EntityPosition::update, 5);
    }
    public static void update() {
        updatePlayers();
        updateEntities();
    }
    public static void updatePlayers() {
        HashMap<UUID, Player> online = new HashMap<>();
        HashMap<UUID, PositionInfo> info = new HashMap<>();
        HashMap<Player, Location> location = new HashMap<>();

        Bukkit.getOnlinePlayers().forEach(p -> {
            online.put(p.getUniqueId(), p);
            info.put(p.getUniqueId(), new PositionInfo(p));
            location.put(p, p.getLocation().clone());
        });

        onlinePlayers.putAll(online);
        onlinePlayers.entrySet().removeIf(kv -> !online.containsKey(kv.getKey()));

        playerInfo.putAll(info);
        playerInfo.entrySet().removeIf(kv -> !info.containsKey(kv.getKey()));

        playerLocations.putAll(location);
        playerLocations.entrySet().removeIf(kv -> !location.containsKey(kv.getKey()));
    }
    /*public static void updateEntities() {
        HashMap<UUID, Location> locations = new HashMap<>();
        HashSet<UUID> locationRemove = new HashSet<>(entityLocations.keySet());

        HashMap<UUID, LivingEntity> list = new HashMap<>();
        HashSet<UUID> listRemove = new HashSet<>(entityList.keySet());

        Bukkit.getWorlds().forEach(w -> w.getEntities().forEach(e -> {
            UUID uuid = e.getUniqueId();
            locations.put(uuid, e.getLocation());
            locationRemove.remove(uuid);
            if (e instanceof LivingEntity le) {
                list.put(uuid, le);
                listRemove.remove(uuid);
            }
        }));

        entityLocations.putAll(locations);
        locationRemove.forEach(entityLocations::remove);

        entityList.putAll(list);
        listRemove.forEach(entityList::remove);
    }*/
    public static final MinecraftServer SERVER = MinecraftServer.getServer();
    public static void updateEntities() {
        HashMap<UUID, Location> locations = new HashMap<>();
        HashSet<UUID> locationsRemove = new HashSet<>(entityLocations.keySet());

        SERVER.getAllLevels().forEach(world -> {
            CraftWorld bukkitWorld = world.getWorld();
            world.getEntities().getAll().forEach(nms -> {
                if (nms instanceof EntityLiving living) {
                    UUID uuid = living.getUUID();
                    locations.put(uuid, new Location(bukkitWorld, nms.getX(), nms.getY(), nms.getZ()));
                    locationsRemove.remove(uuid);
                }
            });
        });

        entityLocations.putAll(locations);
        locationsRemove.forEach(entityLocations::remove);
    }

    public static final Location EMPTY_LOCATION = new Location(null, 0, 0, 0);

    public static class PositionInfo {
        public final Location location;
        public final Location eye;
        public final boolean onGround;

        public PositionInfo(Player player) {
            this.location = player.getLocation().clone();
            this.eye = player.getEyeLocation().clone();
            this.onGround = player.isOnGround();
        }
    }

    public static final ConcurrentHashMap<UUID, PositionInfo> playerInfo = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Player> onlinePlayers = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Player, Location> playerLocations = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Location> entityLocations = new ConcurrentHashMap<>();
    //public static final ConcurrentHashMap<UUID, LivingEntity> entityList = new ConcurrentHashMap<>();

    private static List<Tameable> getEntities() {
        List<Tameable> list = new ArrayList<>();
        Bukkit.getWorlds().forEach(world -> list.addAll(world.getEntitiesByClass(Tameable.class)));
        return list;
    }
    private static List<Tameable>getEntities(system.Func1<Tameable, Boolean> removeIf) {
        List<Tameable> list = getEntities();
        list.removeIf(removeIf::invoke);
        return list;
    }
    public static Map<UUID, Tameable> getEntitiyRows() {
        return getEntities()
                .stream()
                .filter(v -> v.getOwnerUniqueId() != null)
                .collect(Collectors.toMap(Entity::getUniqueId, v -> v));
    }
}






















