package p1;

import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.decoration.EntityItemFrame;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftEntity;
import org.lime.timings.lib.MCTiming;
import org.lime.packetwrapper.WrapperPlayServerMount;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import org.lime.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Displays implements Listener {
    public static core.element create() {
        return core.element.create(Displays.class)
                .withInstance()
                .withInit(Displays::init)
                .withUninit(Displays::uninit);
    }

    private static final Location NONE_LOCATION = new Location(null, 0, 0, 0);

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

    public static double deltaMove(int steps) {
        return deltaStepMove(steps) / (double)steps;
    }
    public static int deltaStepMove(int steps) {
        return (int)((System.currentTimeMillis() / 100) % steps);
    }

    public static void drawPoint(Vector pos, Particle particle) {
        particle.builder()
                .allPlayers()
                .force(false)
                .count(1)
                .extra(0)
                .offset(0,0,0)
                .location(pos.toLocation(lime.MainWorld))
                .spawn();
    }
    public static void drawPoint(Vector pos) {
        drawPoint(pos, true);
    }
    public static void drawPoint(Vector pos, boolean start) {
        drawPoint(pos, (start ? Particle.WAX_ON : Particle.WAX_OFF));
    }
    public static void drawPoint(Vector pos, double delta) {
        int gray = ((int)Math.round(255*delta)) % 256;
        Particle.REDSTONE.builder()
                .allPlayers()
                .force(false)
                .count(1)
                .extra(0)
                .offset(0,0,0)
                .data(new Particle.DustOptions(Color.fromRGB(gray,gray,gray), 1))
                .location(pos.toLocation(lime.MainWorld))
                .spawn();
    }

    public static final ConcurrentHashMap<Player, Location> positionMap = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, PositionInfo> positionUUIDMap = new ConcurrentHashMap<>();

    private static <TKey, TValue>void syncMap(ConcurrentHashMap<TKey, TValue> map, HashMap<TKey, TValue> data) {
        map.entrySet().removeIf(kv -> !data.containsKey(kv.getKey()));
        map.putAll(data);
    }

    public static void init() {
        lime.repeatTicks(() -> {
            HashMap<Player, Location> map_player = new HashMap<>();
            HashMap<UUID, PositionInfo> map_uuid = new HashMap<>();
            Bukkit.getOnlinePlayers().forEach(player -> {
                Location location = player.getLocation().clone();
                map_player.put(player, location);
                map_uuid.put(player.getUniqueId(), new PositionInfo(player));
            });
            syncMap(positionMap, map_player);
            syncMap(positionUUIDMap, map_uuid);
        }, 1);

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(lime._plugin, PacketType.Play.Server.MOUNT) {
            @Override public void onPacketSending(PacketEvent event) {
                WrapperPlayServerMount packet = new WrapperPlayServerMount(event.getPacket());
                IntStream stream = Arrays.stream(getPassengerIDs(packet.getEntityID()));
                stream = IntStream.concat(Arrays.stream(packet.getPassengerIds()), stream);
                try {
                    if (packet.getEntity(event) instanceof CraftEntity entity)
                        stream = IntStream.concat(entity.getHandle().passengers.stream().mapToInt(Entity::getId), stream);
                } catch (Exception ignored) { }
                packet.setPassengerIds(stream.toArray());
            }
        });

        lime.timer().setAsync().withLoopTicks(1).withCallback(Displays::updateAsync).run();
        lime.timer().setSync().withLoopTicks(1).withCallback(Displays::updateSync).run();
    }
    public static void uninit() {
        managers.removeIf(v -> {
            v.removeAll();
            return true;
        });
    }
    private static int indexSync = 0;
    public static void updateSync() {
        indexSync = (indexSync + 1) % 50;
        managers.forEach(display -> {
            if (display.isAsync()) return;
            if (!display.isFast() && indexSync != 0) return;
            try (MCTiming _1 = lime.timing("Displays.Sync." + display.ClassName).startTiming()) {
                display.update();
            }
        });
    }
    private static int indexAsync = 0;
    public static void updateAsync() {
        indexAsync = (indexAsync + 1) % 50;
        managers.forEach(display -> {
            if (!display.isAsync()) return;
            if (!display.isFast() && indexAsync != 0) return;
            try (MCTiming _1 = lime.timing("Displays.Async." + display.ClassName).startTiming()) {
                display.update();
            }
        });
    }

    /*private static final ConcurrentHashMap<Integer, List<Integer>> passengers = new ConcurrentHashMap<>();
    public static int[] getPassengerIDs(int entityID) {
        List<Integer> list = passengers.getOrDefault(entityID, null);
        return list == null ? new int[0] : list.stream().mapToInt(v -> v).toArray();
    }
    public static void addPassengerID(int entityID, int passengerID) {
        List<Integer> list = passengers.getOrDefault(entityID, new ArrayList<>());
        list.add(passengerID);
        passengers.put(entityID, list);
    }
    public static void removePassengerID(int passengerID) {
        passengers.entrySet().removeIf(kv -> kv.getValue().remove((Integer)passengerID) && kv.getValue().size() == 0);
    }
    public static Integer getVehicle(int passengerID) {
        for (Map.Entry<Integer, List<Integer>> kv : passengers.entrySet()) {
            if (kv.getValue().contains(passengerID))
                return kv.getKey();
        }
        return null;
    }
    public static boolean hasVehicle(int passengerID) {
        for (Map.Entry<Integer, List<Integer>> kv : passengers.entrySet()) {
            if (kv.getValue().contains(passengerID))
                return true;
        }
        return false;
    }*/

    private static final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Integer>> passengers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Integer> passengerParents = new ConcurrentHashMap<>();

    public static Integer getPassengerID(int entityID) {
        ConcurrentLinkedQueue<Integer> list = passengers.getOrDefault(entityID, null);
        return list == null ? null : list.stream().findFirst().orElse(null);
    }
    public static int[] getPassengerIDs(int entityID) {
        ConcurrentLinkedQueue<Integer> list = passengers.getOrDefault(entityID, null);
        return list == null ? new int[0] : list.stream().mapToInt(Integer::intValue).toArray();
    }
    public static Set<Integer> getPassengerSetID(int entityID) {
        ConcurrentLinkedQueue<Integer> list = passengers.getOrDefault(entityID, null);
        return list == null ? Collections.emptySet() : new HashSet<>(list);
    }
    public static boolean hasPassengers(int entityID) {
        ConcurrentLinkedQueue<Integer> list = passengers.getOrDefault(entityID, null);
        return list != null && list.size() > 0;
    }
    public static void addPassengerID(int entityID, int passengerID) {
        removePassengerID(passengerID);

        ConcurrentLinkedQueue<Integer> list = passengers.getOrDefault(entityID, null);
        if (list == null) list = new ConcurrentLinkedQueue<>();
        list.add(passengerID);
        passengerParents.put(passengerID, entityID);
        passengers.put(entityID, list);
    }
    public static void removePassengerID(int passengerID) {
        Integer parent = passengerParents.remove(passengerID);
        if (parent == null) return;
        passengers.compute(parent, (k,v) -> {
            if (v == null) return null;
            v.remove(passengerID);
            return v.size() == 0 ? null : v;
        });
        //passengers.entrySet().removeIf(kv -> kv.getValue().remove(passengerID) && kv.getValue().size() == 0);
    }
    public static Integer getVehicle(int passengerID) {
        return passengerParents.getOrDefault(passengerID, null);
    }
    public static boolean hasVehicle(int passengerID) {
        return passengerParents.containsKey(passengerID);
    }

    public static boolean isEqualsLocation(Location loc1, Location loc2) {
        return isEqualsLocation(loc1, loc2, 0);
    }
    public static boolean isEqualsLocation(Location loc1, Location loc2, int check) {
        if (loc1 == null) return loc2 == null;
        if (loc2 == null) return false;
        if (loc1.getWorld() != loc2.getWorld()) return false;
        if (loc1.distanceSquared(loc2) > 0.001 * Math.pow(10, check)) return false;
        double _check = Math.pow(10, check / 10.0) * 0.2;
        if (Math.abs(loc1.getPitch() - loc2.getPitch()) > _check) return false;
        if (Math.abs(loc1.getYaw() - loc2.getYaw()) > _check) return false;
        return true;
    }

    public static class Transform {
        public static LocalLocation combine(LocalLocation parent, LocalLocation local) {
            Vector right = new Vector(-1, 0, 0).rotateAroundY(Math.toRadians(-parent.getYaw()));

            Vector rotated = local.getPosition()
                    .rotateAroundY(Math.toRadians(-(parent.getYaw()+local.yaw)))
                    .rotateAroundAxis(right, Math.toRadians(-(parent.getPitch()+local.pitch)));

            return parent.add(rotated.getX(), rotated.getY(), rotated.getZ()/*, local.yaw, local.pitch*/);
        }
        public static Location toWorld(Location parent, LocalLocation local) {
            Location position = parent.clone();

            Vector right = new Vector(-1, 0, 0).rotateAroundY(Math.toRadians(-parent.getYaw()));

            Vector rotated = local.getPosition()
                    .rotateAroundY(Math.toRadians(-(parent.getYaw()+local.yaw)))
                    .rotateAroundAxis(right, Math.toRadians(-(parent.getPitch()+local.pitch)));

            /*position.setYaw(position.getYaw() + local.yaw);
            position.setPitch(position.getPitch() + local.pitch);*/
            return position.add(rotated.getX(), rotated.getY(), rotated.getZ());
        }
        public static Location[] toWorld(Location parent, LocalLocation[] locals) {
            int length = locals.length;
            Location[] positions = new Location[length];

            Vector right = new Vector(-1, 0, 0).rotateAroundY(Math.toRadians(-parent.getYaw()));

            for (int i = 0; i < length; i++) {
                Location position = parent.clone();
                LocalLocation local = locals[i];

                Vector rotated = local.getPosition()
                        .rotateAroundY(Math.toRadians(-(parent.getYaw() + local.yaw)))
                        .rotateAroundAxis(right, Math.toRadians(-(parent.getPitch() + local.pitch)));

                /*position.setYaw(position.getYaw() + local.yaw);
                position.setPitch(position.getPitch() + local.pitch);*/
                positions[i] = position.add(rotated.getX(), rotated.getY(), rotated.getZ());
            }
            return positions;
        }
        public static List<Location> toWorld(Location parent, List<LocalLocation> locals) {
            List<Location> positions = new ArrayList<>();

            Vector right = new Vector(-1, 0, 0).rotateAroundY(Math.toRadians(-parent.getYaw()));

            for (LocalLocation local : locals) {
                Vector rotated = local.getPosition()
                        .rotateAroundY(Math.toRadians(-(parent.getYaw() + local.yaw)))
                        .rotateAroundAxis(right, Math.toRadians(-(parent.getPitch() + local.pitch)));

                Location position = parent.clone();
                /*position.setYaw(position.getYaw() + local.yaw);
                position.setPitch(position.getPitch() + local.pitch);*/
                positions.add(position.add(rotated.getX(), rotated.getY(), rotated.getZ()));
            }
            return positions;
        }
    }

    public static class EditedDataWatcher extends DataWatcher {
        public static final DataWatcherObject<Optional<IChatBaseComponent>> DATA_CUSTOM_NAME = new DataWatcherObject<>(2, DataWatcherRegistry.OPTIONAL_COMPONENT);
        public static final DataWatcherObject<EntityPose> DATA_POSE = new DataWatcherObject<>(6, DataWatcherRegistry.POSE);
        public static final DataWatcherObject<ItemStack> DATA_ITEM = new DataWatcherObject<>(8, DataWatcherRegistry.ITEM_STACK);
        public static final DataWatcherObject<Integer> DATA_ROTATION = new DataWatcherObject<>(9, DataWatcherRegistry.INT);

        DataWatcher watcher;
        private final HashMap<Integer, Item<?>> customEntries = new HashMap<>();
        public EditedDataWatcher(DataWatcher watcher) {
            super(null);
            this.watcher = watcher;
        }
        public static EditedDataWatcher empty() {
            return new EditedDataWatcher(new DataWatcher(null));
        }

        @Override public <T> void register(DataWatcherObject<T> datawatcherobject, T t0) {
            watcher.register(datawatcherobject, t0);
        }
        public <T>EditedDataWatcher setCustom(DataWatcherObject<T> datawatcherobject, T t0) {
            customEntries.put(datawatcherobject.getId(), new Item<>(datawatcherobject, t0));
            return this;
        }
        public void resetCustom() {
            customEntries.clear();
        }
        public void markDirtyAll() {
            List<Item<?>> items = getAll();
            if (items == null) return;
            items.forEach(item -> item.setDirty(true));
        }
        @Override public <T> T get(DataWatcherObject<T> datawatcherobject) {
            Item<?> item = customEntries.getOrDefault(datawatcherobject.getId(), null);
            if (item == null) return watcher.get(datawatcherobject);
            return (T)item.getValue();
        }
        @Override public <T> void set(DataWatcherObject<T> datawatcherobject, T t0) {
            watcher.set(datawatcherobject, t0);
        }
        @Override public <T> void markDirty(DataWatcherObject<T> datawatcherobject) {
            watcher.markDirty(datawatcherobject);
        }
        @Override public boolean isDirty() {
            return watcher.isDirty();
        }
        private List<Item<?>> getNew(List<Item<?>> oldList) {
            if (customEntries.size() == 0) return oldList;
            if (oldList == null) return null;
            List<Item<?>> items = new ArrayList<>();
            for (Item<?> item : oldList)
                items.add(customEntries.getOrDefault(item.getAccessor().getId(), item));
            return items;
        }
        @Nullable @Override public List<Item<?>> packDirty() {
            return getNew(watcher.packDirty());
        }
        @Nullable @Override public List<Item<?>> getAll() {
            return getNew(watcher.getAll());
        }
        @Override public boolean isEmpty() {
            return watcher.isEmpty();
        }
        @Override public void clearDirty() {
            List<Item<?>> items = getAll();
            if (items == null) return;
            items.forEach(item -> item.setDirty(false));
        }
    }
    public static abstract class IObjectDisplay<TValue> {
        public static float normalRot(float val) { return Float.isFinite(val) ? val : 0; }

        public abstract void update(TValue value, double delta);

        public boolean isFilter(Player player) { return true; }
        public abstract void hide(Player player);
        protected abstract void show(Player player);

        public abstract Location lastLocation();
        public abstract void destroy();
    }
    public static class MultiObjectDisplay<TValue> extends IShowDisplay<TValue> {
        private final int count;
        private final IObjectDisplay<TValue>[] objects;
        public MultiObjectDisplay(int count, system.Func2<Integer, IObjectDisplay<TValue>, IObjectDisplay<TValue>> create) {
            this.count = count;
            this.objects = new IObjectDisplay[count];
            for (int i = 0; i < count; i++) this.objects[i] = create.invoke(i, i <= 0 ? null : this.objects[i - 1]);
        }
        @Override public void destroy() { for (int i = 0; i < count; i++) this.objects[i].destroy(); }
        @Override public void update(TValue value, double delta) { for (int i = 0; i < count; i++) this.objects[i].update(value, delta); }
        @Override public Location lastLocation() { return null; }
    }
    public static abstract class IShowDisplay<TValue> extends IObjectDisplay<TValue> {
        public double getDistance() { return 20; }

        private final List<IShowDisplay<TValue>> childDisplays = new ArrayList<>();
        protected <T extends IShowDisplay<TValue>>T preInitDisplay(T child) { childDisplays.add(child); return child; }

        private boolean destroyed = false;
        public boolean isDestroyed() { return destroyed; }
        protected void postInit() { childDisplays.forEach(IShowDisplay::postInit); }

        @Override protected void show(Player player) {
            sendData(player, false);
            this.childDisplays.forEach(v -> {
                if (!v.isFilter(player)) return;
                v.show(player);
            });
        }
        protected void sendData(Player player, boolean child) { if (child) sendDataChild(player); }
        protected final void sendData(Player player) { sendData(player, true); }
        protected void sendDataChild(Player player) {
            this.childDisplays.forEach(v -> {
                if (!v.isFilter(player)) return;
                v.sendData(player, true);
            });
        }

        @Override public void hide(Player player) { this.childDisplays.forEach(v -> v.hide(player)); }

        public void destroy() { destroyed = true; childDisplays.forEach(IShowDisplay::destroy); }
    }
    public static abstract class IShowEntityDisplay<TValue, T extends Entity> extends IShowDisplay<TValue> {
        public T entity;
        public int entityID;
        private EditedDataWatcher dataWatcher;

        protected void editDataWatcher(Player player, EditedDataWatcher dataWatcher) { }
        protected void sendDataWatcher(Player player) {
            if (entity instanceof Marker) return;
            dataWatcher.resetCustom();
            editDataWatcher(player, dataWatcher);
            dataWatcher.markDirtyAll();
            PacketManager.sendPacket(player, new PacketPlayOutEntityMetadata(entityID, dataWatcher, true));
        }

        protected T createEntity(Vector position) { return createEntity(new Location(lime.LoginWorld, position.getX(), position.getY(), position.getZ())); }
        protected abstract T createEntity(Location location);

        @Override public void hide(Player player) {
            if (!(entity instanceof Marker)) {
                PacketPlayOutEntityDestroy ppoed = new PacketPlayOutEntityDestroy(entityID);
                PacketManager.sendPackets(player, ppoed);
            }
            super.hide(player);
        }
        @Override protected void sendData(Player player, boolean child) {
            super.sendData(player, child);
            sendDataWatcher(player);
        }
        @Override protected void show(Player player) {
            if (!(entity instanceof Marker)) PacketManager.sendPackets(player, entity.getPacket());
            super.show(player);
        }
        @Override protected void postInit() {
            entities.put(entityID, this);
            this.dataWatcher = new EditedDataWatcher(entity.getDataWatcher());
            super.postInit();
        }
        @Override public void destroy() {
            entities.remove(entityID);
            removePassengerID(entityID);
            super.destroy();
        }
    }
    public static class LocalLocation {
        private final double pos_x;
        private final double pos_y;
        private final double pos_z;
        private final float yaw;
        private final float pitch;

        public Vector getPosition() { return new Vector(pos_x, pos_y, pos_z); }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }

        public LocalLocation setPosition(Vector position) { return new LocalLocation(position, yaw, pitch); }
        public LocalLocation setPosition(double x, double y, double z) { return new LocalLocation(x, y, z, yaw, pitch); }
        public LocalLocation setYaw(float yaw) { return new LocalLocation(pos_x, pos_y, pos_z, yaw, pitch); }
        public LocalLocation setPitch(float pitch) { return new LocalLocation(pos_x, pos_y, pos_z, yaw, pitch); }
        public LocalLocation setRotation(float yaw, float pitch) { return new LocalLocation(pos_x, pos_y, pos_z, yaw, pitch); }

        public LocalLocation(Vector position, float yaw, float pitch) {
            this(position.getX(), position.getY(), position.getZ(), yaw, pitch);
        }
        public LocalLocation(Vector position) { this(position, 0, 0); }
        public LocalLocation(double x, double y, double z) { this(x, y, z, 0, 0); }
        public LocalLocation(double x, double y, double z, float yaw, float pitch) {
            this.pos_x = x;
            this.pos_y = y;
            this.pos_z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public LocalLocation add(double x, double y, double z) {
            return new LocalLocation(
                    pos_x + x,
                    pos_y + y,
                    pos_z + z,
                    yaw,
                    pitch
            );
        }
        public LocalLocation add(double x, double y, double z, float yaw, float pitch) {
            return new LocalLocation(
                    pos_x + x,
                    pos_y + y,
                    pos_z + z,
                    this.yaw + yaw,
                    this.pitch + pitch
            );
        }
        public LocalLocation add(LocalLocation other) {
            return new LocalLocation(
                    pos_x + other.pos_x,
                    pos_y + other.pos_y,
                    pos_z + other.pos_z,
                    yaw + other.yaw,
                    pitch + other.pitch
            );
        }
        public LocalLocation combine(LocalLocation local) {
            return Transform.combine(this, local);
        }
        public LocalLocation clone() {
            return new LocalLocation(pos_x, pos_y, pos_z, yaw, pitch);
        }

        @Override public String toString() {
            return system.getString(new Vector(pos_x,pos_y,pos_z)) + " (" + system.getDouble(yaw) + ";" + system.getDouble(pitch) + ")";
        }
    }
    public static abstract class ChildObjectDisplay<TValue, T extends Entity> extends IShowEntityDisplay<TValue, T> {
        private final LocalLocation _localPosition;
        public LocalLocation localPosition() { return _localPosition; }
        @Override public Location lastLocation() { return Transform.toWorld(parent.lastLocation(), localPosition()); }

        public final IObjectDisplay<TValue> parent;
        public ObjectDisplay<TValue, ?> objectParent() {
            if (parent instanceof ObjectDisplay<TValue, ?> obj) return obj;
            else if (parent instanceof ChildObjectDisplay<TValue, ?> obj) return obj.objectParent();
            else return null;
        }

        public void tryShow(Player player) {

            show(player);
        }

        protected ChildObjectDisplay(IObjectDisplay<TValue> parent) { this(parent, null); }
        protected ChildObjectDisplay(IObjectDisplay<TValue> parent, LocalLocation local) {
            this.parent = parent;
            this._localPosition = local;
        }
        @Override public void update(TValue value, double delta) { }
        @Override protected void postInit() {
            this.entity = createEntity(lastLocation());
            this.entityID = entity.getId();
            super.postInit();
        }
    }
    public static abstract class ObjectDisplay<TValue, T extends Entity> extends IShowEntityDisplay<TValue, T> {
        public static final String KEY_READONLY_DISTANCE = "readonly.distance";
        private final Location _location;
        public Location location() { return _location == null ? null : _location.clone(); }

        protected ObjectDisplay() { this(null); }
        protected ObjectDisplay(Location location) { this._location = location; }

        @Override protected void postInit() {
            this.entity = createEntity(location());
            this.entityID = entity.getId();
            super.postInit();
        }

        public <TKey>Optional<TKey> keyOf(Player player, String key) {
            return Optional.ofNullable(shows.getOrDefault(player, null))
                    .map(v -> (TKey) v.getOrDefault(key, null));
        }
        public <TKey>void keyOf(Player player, String key, TKey value) {
            ConcurrentHashMap<String, Object> map = shows.getOrDefault(player, null);
            if (map == null) return;
            map.put(key, value);
        }

        private final ConcurrentHashMap<Player, ConcurrentHashMap<String, Object>> shows = new ConcurrentHashMap<>();
        public Map<Player, Location> getShows() { return shows.keySet().stream().collect(Collectors.toMap(p -> p, p -> positionMap.getOrDefault(p, NONE_LOCATION))); }
        public List<Player> getShowPlayers() { return new ArrayList<>(shows.keySet()); }
        public boolean hasShow(UUID uuid) {
            system.Toast1<Boolean> has = system.toast(false);
            shows.forEach((p,v) -> { if (p.getUniqueId().equals(uuid)) has.val0 = true; });
            return has.val0;
        }
        protected int getShowCount() {
            return shows.size();
        }
        protected Location last_location;

        public void invokeAll(system.Action1<Player> invoke) { shows.keySet().forEach(invoke); }
        public void invokeDistanceAll(system.Action2<Player, Double> invoke) {
            shows.forEach((player, map) -> {
                Double distance = (Double)map.getOrDefault(KEY_READONLY_DISTANCE, null);
                if (distance == null) return;
                invoke.invoke(player, distance);
            });
        }
        protected Player getNearShow() { return system.GetNearPlayer(getShows(), location()); }
        protected Player getNearShow(double minDistance, system.Func1<Player, Boolean> filter) { return system.GetNearPlayer(getShows(), location(), minDistance, filter); }

        public Optional<Double> getShowDistance(Player player) {
            return Optional.ofNullable(shows.getOrDefault(player, null))
                    .map(v -> (Double) v.getOrDefault(KEY_READONLY_DISTANCE, null));
        }

        @Override public void update(TValue value, double delta) {
            HashMap<Player, ConcurrentHashMap<String, Object>> nowSee = new HashMap<>();
            HashMap<Player, Double> distanceMap = new HashMap<>();
            Location newLocation = this.location();
            if (newLocation != null) {
                positionMap.forEach((player, location) -> {
                    if (location.getWorld() != newLocation.getWorld()) return;
                    double distance = location.distance(newLocation);
                    if (distance > Math.min(getDistance(), (player.getSendViewDistance()-1) * 16)) return;
                    if (!isFilter(player)) return;
                    distanceMap.put(player, distance);
                    nowSee.put(player, new ConcurrentHashMap<>());
                });
            }
            shows.keySet().removeIf(v -> {
                if (!v.isOnline()) return true;
                if (nowSee.containsKey(v)) return false;
                hide(v);
                return true;
            });
            nowSee.keySet().removeIf(shows::containsKey);
            shows.putAll(nowSee);
            nowSee.forEach((player, map) -> show(player));
            shows.forEach((player, map) -> {
                Double distance = distanceMap.getOrDefault(player, null);
                if (distance != null) map.put(KEY_READONLY_DISTANCE, distance);
            });
            last_location = newLocation;
        }
        public void hideAll() {
            shows.keySet().forEach(this::hide);
            shows.clear();
        }
        @Override public void destroy() {
            hideAll();
            super.destroy();
        }

        @Override public Location lastLocation() { return last_location == null ? location() : last_location; }
    }
    public static abstract class CombineObjectDisplay<TValue> extends IObjectDisplay<TValue> {
        public int getDistance() { return 20; }
    }
    public static abstract class DisplayManager<TKey, TValue, TObject extends IObjectDisplay<TValue>> {
        public boolean isFast() { return false; }
        public boolean isAsync() { return false; }

        private final system.LockToast1<Boolean> isDestroy = system.toast(false).lock();

        public final String ClassName;
        public DisplayManager() {
            ClassName = this.getClass().getName();
        }
        private final ConcurrentHashMap<TKey, TObject> displays = new ConcurrentHashMap<>();
        public void remove(TKey key) {
            TObject obj = displays.remove(key);
            if (obj == null) return;
            obj.destroy();
        }
        public ConcurrentHashMap<TKey, TObject> getDisplays() {
            return displays;
        }
        public abstract Map<TKey, TValue> getData();
        public abstract TObject create(TKey key, TValue value);

        private long last_update = -1;

        public void updateOne(TKey key, TValue value, double delta) {
            if (isDestroy.get0()) return;
            TObject display = displays.getOrDefault(key, null);
            if (display == null) {
                display = create(key, value);
                if (display == null) return;
                displays.put(key, display);
            }
            display.update(value, delta);
        }
        public void update() {
            if (isDestroy.get0()) return;
            Map<TKey, TValue> nowExists = getData();
            displays.entrySet().removeIf(kv -> {
                TKey key = kv.getKey();
                TValue row = nowExists.getOrDefault(key, null);
                if (row == null) {
                    kv.getValue().destroy();
                    return true;
                }
                return false;
            });
            long now = System.currentTimeMillis();
            double delta = (last_update == -1 ? 0 : (now - last_update)) / 1000.0;
            nowExists.forEach((k,v) -> updateOne(k, v, delta));
            last_update = now;
        }
        public void removeAll() {
            isDestroy.set0(true);
            displays.forEach((k,v) -> v.destroy());
            displays.clear();
        }
    }
    public static abstract class MultiDisplayManager<TKey, TValue, TObject extends IObjectDisplay<TValue>> extends DisplayManager<TKey, TValue, MultiObjectDisplay<TValue>> {
        public final int count;
        protected MultiDisplayManager(int count) {
            this.count = count;
        }
        @Override public MultiObjectDisplay<TValue> create(TKey key, TValue value) {
            return new <TObject>MultiObjectDisplay<TValue>(count, (id, last) -> createOne(id, (TObject) last, key, value));
        }
        public abstract TObject createOne(int id, TObject last, TKey key, TValue value);
    }
    public static <TValue, TObject extends IObjectDisplay<TValue>>DisplayManager<Integer, TValue, TObject> single(system.Func0<TValue> single, system.Func0<TObject> object, boolean fast, boolean async) {
        return new DisplayManager<>() {
            @Override public boolean isFast() { return fast; }
            @Override public boolean isAsync() { return async; }

            @Override public Map<Integer, TValue> getData() {
                TValue value = single.invoke();
                return value == null ? Collections.emptyMap() : Collections.singletonMap(0, single.invoke());
            }
            @Override public TObject create(Integer integer, TValue value) { return object.invoke(); }
        };
    }
    public static <TValue, TObject extends IObjectDisplay<TValue>>DisplayManager<Integer, TValue, TObject> single(system.Func0<TObject> object, boolean fast, boolean async, TValue value) {
        return new DisplayManager<>() {
            @Override public boolean isFast() { return fast; }
            @Override public boolean isAsync() { return async; }

            @Override public Map<Integer, TValue> getData() { return Collections.singletonMap(0, value); }
            @Override public TObject create(Integer integer, TValue value) { return object.invoke(); }
        };
    }

    private static final ConcurrentLinkedQueue<DisplayManager<?,?,?>> managers = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<Integer, IShowEntityDisplay<?, ?>> entities = new ConcurrentHashMap<>();
    public static IShowEntityDisplay<?, ?> byID(int id) { return entities.getOrDefault(id, null); }
    public static <TDisplay extends IShowEntityDisplay<?, ?>>TDisplay byID(Class<TDisplay> tClass, int id) {
        IShowEntityDisplay<?, ?> display = byID(id);
        if (tClass.isInstance(display)) return (TDisplay)display;
        return null;
    }

    public static void initDisplay(DisplayManager<?,?,?>... managers) {
        for (DisplayManager<?,?,?> manager : managers) {
            manager.isDestroy.set0(false);
            Displays.managers.add(manager);
        }
    }
    public static void uninitDisplay(DisplayManager<?,?,?>... managers) {
        for (DisplayManager<?,?,?> manager : managers) {
            if (Displays.managers.remove(manager)) manager.removeAll();
        }
    }
}





















