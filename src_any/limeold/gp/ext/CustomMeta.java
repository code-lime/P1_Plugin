/*
 * Made by mfnalex / JEFF Media GbR
 *
 * If you find this helpful or if you're using this project inside your paid plugins,
 * consider leaving a donation :)
 *
 * https://paypal.me/mfnalex
 *
 * If you need help or have any suggestions, just create an issue or join my discord
 * and head to the channel #programming-help
 *
 * https://discord.jeff-media.de
 */

package p1.ext;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.util.Vector;
import org.lime.Position;
import org.lime.core;
import org.lime.system;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.persistence.PersistentDataContainer;
import AnyEvent;
import p1.ItemManager;
import JManager;
import lime;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class CustomMeta implements Listener {
    private static final double UPDATE_DELTA = 0.1;
    public static core.element create() {
        return core.element.create(CustomMeta.class)
                .withInit(CustomMeta::init)
                .withInstance();
    }
    public static class MetaLoader<T extends IMeta<?, I>, I extends ILoaded<?, I>> {
        private final Class<T> tClass;
        private final Class<I> iClass;

        private final system.Func1<T, Boolean> onUpdate;
        private final system.Func1<T, Boolean> onFilter;
        private static class CheckFilter {
            public final List<Material> types;
            public final system.Func1<Block, Boolean> filter;
            public CheckFilter(List<Material> types, system.Func1<Block, Boolean> filter) {
                this.types = types;
                this.filter = filter;
            }
            public boolean check(Block block) {
                return types.contains(block.getType()) && filter.invoke(block);
            }
        }
        private final List<CheckFilter> checks;
        private MetaLoader(Class<T> tClass, Class<I> iClass, system.Func1<T, Boolean> onUpdate, system.Func1<T, Boolean> onFilter, List<CheckFilter> checks) {
            this.tClass = tClass;
            this.iClass = iClass;
            this.onUpdate = onUpdate;
            this.onFilter = onFilter;
            this.checks = checks;
        }

        public static <T extends IMeta<?, I>, I extends ILoaded<?, I>>MetaLoader<T, I> create(Class<T> tClass, Class<I> iClass) {
            return new MetaLoader<>(tClass, iClass, null, null, ImmutableList.of());
        }
        public static <T extends IBlockMeta<?>>MetaLoader<T, LoadedBlock> of(Class<T> tClass, List<Material> materials){
            return MetaLoader.create(tClass, LoadedBlock.class)
                    .withFilter(v -> materials.contains(v.getLoaded().getBlock().getType()))
                    .addPlace(materials);
        }
        public static <T extends IBlockMeta<?>>MetaLoader<T, LoadedBlock> of(Class<T> tClass, Material... materials){
            return of(tClass, Arrays.asList(materials));
        }

        public MetaLoader<T, I> withClass(Class<T> tClass) {
            return new MetaLoader<>(tClass, iClass, onUpdate, onFilter, checks);
        }
        public MetaLoader<T, I> withUpdate(system.Func1<T, Boolean> onUpdate) {
            return new MetaLoader<>(tClass, iClass, onUpdate, onFilter, checks);
        }
        public MetaLoader<T, I> withUpdate(system.Func2<T, Double, Boolean> onUpdate) {
            return withUpdate(v -> onUpdate.invoke(v, UPDATE_DELTA));
        }
        public MetaLoader<T, I> withFilter(system.Func1<T, Boolean> onFilter) {
            return new MetaLoader<>(tClass, iClass, onUpdate, onFilter, checks);
        }

        public MetaLoader<T, I> addPlace(Material type, system.Func1<Block, Boolean> filter) {
            return addPlace(Collections.singletonList(type), filter);
        }
        public MetaLoader<T, I> addPlace(List<Material> types, system.Func1<Block, Boolean> filter) {
            return new MetaLoader<>(tClass, iClass, onUpdate, onFilter, ImmutableList.<CheckFilter>builder().addAll(this.checks).add(new CheckFilter(types, filter)).build());
        }
        public MetaLoader<T, I> addPlace(Material... types) {
            return addPlace(Arrays.asList(types));
        }
        public MetaLoader<T, I> addPlace(List<Material> types) {
            return addPlace(types, v -> true);
        }

        @Override public int hashCode() {
            return tClass.hashCode();
        }
        @Override public boolean equals(Object obj) {
            return obj instanceof MetaLoader && ((MetaLoader<?,?>) obj).tClass.equals(tClass);
        }

        private boolean update(IMeta<?, ?> dat) {
            if (onUpdate == null || !tClass.isInstance(dat)) return true;
            return onUpdate.invoke((T)dat);
        }
        private boolean filter(IMeta<?, ?> dat) {
            if (onFilter == null || !tClass.isInstance(dat)) return true;
            return onFilter.invoke((T)dat);
        }
        private boolean canPlace(Block block) {
            for (CheckFilter filter : checks) {
                if (filter.check(block))
                    return true;
            }
            return false;
        }

        private T getOrNull(ILoaded<?,?> loaded) {
            if (!iClass.isInstance(loaded)) return null;
            return ((I)loaded).getOrNull(tClass);
        }
    }
    public static <T extends IMeta<?, I>, I extends ILoaded<?, I>> void loadMeta(Class<T> tClass, Class<I> iClass) {
        loadMeta(MetaLoader.create(tClass, iClass));
    }
    public static <T extends IMeta<?, I>, I extends ILoaded<?, I>> void loadMeta(Class<T> tClass, Class<I> iClass, system.Func1<MetaLoader<T, I>, MetaLoader<T, I>> loader) {
        loadMeta(loader.invoke(MetaLoader.create(tClass, iClass)));
    }
    public static <T extends IMeta<?, I>, I extends ILoaded<?, I>> void loadMeta(MetaLoader<T, I> loader) {
        metaLoaders.add(loader);
    }
    private static final ConcurrentLinkedQueue<MetaLoader<? extends IMeta<?, ?>, ? extends ILoaded<?,?>>> metaLoaders = new ConcurrentLinkedQueue<>();
    public static void init() {
        lime.timer()
                .setSync()
                .withLoop(UPDATE_DELTA)
                .withWait(UPDATE_DELTA)
                .withCallback(CustomMeta::update)
                .run();
        AnyEvent.AddEvent("chunk.up", AnyEvent.type.owner, player -> on(player.getLocation().getChunk()));
    }

    public static class SyncData<TKey, TLoad extends ILoaded<TKey, TLoad>> {
        private final ConcurrentHashMap<TKey, ConcurrentHashMap<Class<? extends IMeta<?, TLoad>>, ConcurrentLinkedQueue<system.Func1<IMeta<?, TLoad>, Boolean>>>> syncMap = new ConcurrentHashMap<>();

        public <T extends IMeta<?, TLoad>>void sync(TKey key, Class<T> tClass, system.Func1<T, Boolean> callback) {
            syncMap.compute(key, (k, v) -> {
                if (v == null) v = new ConcurrentHashMap<>();
                v.compute(tClass, (_k, _v) -> {
                    if (_v == null) _v = new ConcurrentLinkedQueue<>();
                    _v.add(_a -> callback.invoke((T)_a));
                    return _v;
                });
                return v;
            });
        }
        public <T extends IMeta<?, TLoad>>void sync(TKey key, Class<T> tClass, system.Action1<T> callback) {
            syncMap.compute(key, (k, v) -> {
                if (v == null) v = new ConcurrentHashMap<>();
                v.compute(tClass, (_k, _v) -> {
                    if (_v == null) _v = new ConcurrentLinkedQueue<>();
                    _v.add(_a -> { callback.invoke((T)_a); return false; });
                    return _v;
                });
                return v;
            });
        }

        public HashMap<TKey, HashMap<Class<? extends IMeta<?, TLoad>>, List<system.Func1<IMeta<?, TLoad>, Boolean>>>> getSync(HashMap<TKey, ?> map) {
            HashMap<TKey, HashMap<Class<? extends IMeta<?, TLoad>>, List<system.Func1<IMeta<?, TLoad>, Boolean>>>> _out = new HashMap<>();
            syncMap.entrySet().removeIf(kv -> {
                if (!map.containsKey(kv.getKey())) return true;
                HashMap<Class<? extends IMeta<?, TLoad>>, List<system.Func1<IMeta<?, TLoad>, Boolean>>> _map = new HashMap<>();
                kv.getValue().forEach((k,v) -> _map.put(k, new ArrayList<>(v)));
                _out.put(kv.getKey(), _map);
                return true;
            });
            return _out;
        }
    }

    private static abstract class ILoaded<K, T extends ILoaded<K, T>> {
        private final UUID uuid;
        private final Marker marker;
        protected abstract World getWorld();
        protected abstract Vector getVector();
        public abstract Location getLocation();
        protected Marker getMarker() { return marker; }
        private final ConcurrentHashMap<String, JsonElement> data;
        public abstract SyncData<K,T> getSync();
        protected abstract K getKey();

        public UUID getUniqueId() { return this.uuid; }
        public Location getLocation(double x, double y, double z) { return getLocation().add(x,y,z); }
        public Location getCenterLocation(double x, double y, double z) { return getLocation(x + 0.5, y + 0.5,z + 0.5); }
        public Location getCenterLocation() { return getCenterLocation(0, 0, 0); }
        public Map<String, JsonElement> getData() { return this.data; }

        public ILoaded(Marker marker, ConcurrentHashMap<String, JsonElement> data) {
            this.marker = marker;
            this.uuid = marker.getUniqueId();
            this.data = data;
        }

        public <TMeta extends IMeta<?, T>>String getJsonKey(Class<TMeta> tClass) { return tClass.getName(); }
        protected <TMeta extends IMeta<?, T>>JsonElement getJson(Class<TMeta> tClass) { return system.DeepCopy(this.data.getOrDefault(getJsonKey(tClass), null)); }

        public void kill() {
            marker.remove();
        }

        public <TMeta extends IMeta<?, T>>TMeta get(Class<TMeta> tClass, JsonElement json) {
            TMeta obj;
            try { obj = tClass.newInstance(); } catch (Exception e) { throw new IllegalArgumentException(e); }
            obj.init((T)this, json);
            return obj;
        }
        public <TMeta extends IMeta<?, T>>TMeta getOrNull(Class<TMeta> tClass) {
            JsonElement json = getJson(tClass);
            return json == null ? null : get(tClass, json);
        }
        public <TMeta extends IMeta<?, T>>TMeta getOrAdd(Class<TMeta> tClass) {
            return get(tClass, getJson(tClass));
        }
        public <TMeta extends IMeta<?, T>>void save(TMeta meta) {
            String key = getJsonKey(meta.getClass());
            this.data.put(key, meta.write());
        }
        public <TMeta extends IMeta<?, T>>void remove(TMeta meta) {
            String key = getJsonKey(meta.getClass());
            this.data.remove(key);
        }

        public <TValue extends IMeta<?, T>>void sync(Class<TValue> tClass, system.Func1<TValue, Boolean> callback) { getSync().sync(getKey(), tClass, callback); }
        public <TValue extends IMeta<?, T>>void sync(Class<TValue> tClass, system.Action1<TValue> callback) { getSync().sync(getKey(), tClass, callback); }
    }

    public static class LoadedEntity extends ILoaded<UUID, LoadedEntity> {
        private static final ConcurrentHashMap<UUID, LoadedEntity> loadedData = new ConcurrentHashMap<>();

        private static final SyncData<UUID, LoadedEntity> sync = new SyncData<>();

        public static <TValue extends IEntityMeta<?>>void ofSync(UUID uuid, Class<TValue> tClass, system.Func1<TValue, Boolean> callback) { sync.sync(uuid, tClass, callback); }
        public static <TValue extends IEntityMeta<?>>void ofSync(UUID uuid, Class<TValue> tClass, system.Action1<TValue> callback) { sync.sync(uuid, tClass, callback); }

        @Override protected World getWorld() { return getMarker().getWorld(); }
        @Override protected Vector getVector() { return getMarker().getLocation().toVector(); }
        @Override public Location getLocation() { return getMarker().getLocation(); }

        @Override public SyncData<UUID, LoadedEntity> getSync() { return sync; }

        @Override protected UUID getKey() { return getUniqueId(); }

        public LoadedEntity(Marker marker, ConcurrentHashMap<String, JsonElement> data) {
            super(marker, data);
        }

        public static <T extends IEntityMeta<?>>List<T> allReadOnly(Class<T> tClass) {
            List<T> list = new ArrayList<>();
            loadedData.values().forEach(meta -> {
                T _meta = meta.getOrNull(tClass);
                if (_meta == null) return;
                list.add(_meta);
            });
            return list;
        }
        public static List<IEntityMeta<?>> allReadOnlyAll() {
            return loadedData.values()
                    .stream()
                    .flatMap(loaded -> metaLoaders.stream()
                            .map(v -> v.getOrNull(loaded) instanceof IEntityMeta<?> bb ? bb : null)
                            .filter(Objects::nonNull)
                    )
                    .collect(Collectors.toList());
        }
    }
    public static class LoadedBlock extends ILoaded<Position, LoadedBlock> {
        private static final ConcurrentHashMap<Position, LoadedBlock> loadedData = new ConcurrentHashMap<>();

        private static final SyncData<Position, LoadedBlock> sync = new SyncData<>();
        @Override public SyncData<Position, LoadedBlock> getSync() { return sync; }

        public static <TValue extends IBlockMeta<?>>void ofSync(Position position, Class<TValue> tClass, system.Func1<TValue, Boolean> callback) { sync.sync(position, tClass, callback); }
        public static <TValue extends IBlockMeta<?>>void ofSync(Position position, Class<TValue> tClass, system.Action1<TValue> callback) { sync.sync(position, tClass, callback); }

        private static final ConcurrentHashMap<Position, ConcurrentLinkedQueue<MetaLoader<?, LoadedBlock>>> setMeta = new ConcurrentHashMap<>();
        private static void set(Position pos, List<MetaLoader<?, LoadedBlock>> meta) {
            setMeta.compute(pos, (k,v) -> {
                if (v == null) v = new ConcurrentLinkedQueue<>();
                v.addAll(meta);
                return v;
            });
        }

        @Override protected Position getKey() { return getPosition(); }

        private final Block block;
        private final Position position;

        @Override protected Vector getVector() { return position.toVector(); }
        @Override protected World getWorld() { return position.world; }
        @Override public Location getLocation() { return position.getLocation(); }

        public LoadedBlock(Marker marker, Location location, ConcurrentHashMap<String, JsonElement> data) {
            super(marker, data);
            this.block = location.getBlock();
            this.position = new Position(this.block);
        }
        public Block getBlock() { return this.block; }
        public Position getPosition() { return this.position; }

        public static LoadedBlock getReadOnly(Position position) { return loadedData.getOrDefault(position, null); }
        public static LoadedBlock getReadOnly(Location location) { return getReadOnly(new Position(location)); }
        public static LoadedBlock getReadOnly(Block block) { return getReadOnly(block.getLocation()); }
        public static LoadedBlock getReadOnly(World world, Vector position) { return getReadOnly(new Position(world, position)); }

        private static <T extends IBlockMeta<?>>T getOrNull(LoadedBlock loaded, Class<T> tClass) { return loaded == null ? null : loaded.getOrNull(tClass); }
        public static <T extends IBlockMeta<?>>T getReadOnlyOrNull(Position position, Class<T> tClass) { return getOrNull(getReadOnly(position), tClass); }
        public static <T extends IBlockMeta<?>>T getReadOnlyOrNull(Location location, Class<T> tClass) { return getOrNull(getReadOnly(location), tClass); }
        public static <T extends IBlockMeta<?>>T getReadOnlyOrNull(Block block, Class<T> tClass) { return getOrNull(getReadOnly(block), tClass); }
        public static <T extends IBlockMeta<?>>T getReadOnlyOrNull(World world, Vector position, Class<T> tClass) { return getOrNull(getReadOnly(world, position), tClass); }

        private static List<IBlockMeta<?>> getReadOnlyAll(LoadedBlock loaded) {
            return loaded == null
                    ? Collections.emptyList()
                    : metaLoaders.stream().map(v -> v.getOrNull(loaded) instanceof IBlockMeta<?> bb ? bb : null).filter(Objects::nonNull).collect(Collectors.toList());
        }
        public static List<IBlockMeta<?>> getReadOnlyAll(Position position) { return getReadOnlyAll(getReadOnly(position)); }
        public static List<IBlockMeta<?>> getReadOnlyAll(Location location) { return getReadOnlyAll(getReadOnly(location)); }
        public static List<IBlockMeta<?>> getReadOnlyAll(Block block) { return getReadOnlyAll(getReadOnly(block)); }
        public static List<IBlockMeta<?>> getReadOnlyAll(World world, Vector position) { return getReadOnlyAll(getReadOnly(world, position)); }

        public static <T extends IBlockMeta<?>>List<T> allReadOnly(Class<T> tClass) {
            List<T> list = new ArrayList<>();
            loadedData.values().forEach(meta -> {
                T _meta = meta.getOrNull(tClass);
                if (_meta == null) return;
                list.add(_meta);
            });
            return list;
        }
        public static List<IBlockMeta<?>> allReadOnlyAll() {
            return loadedData.values()
                    .stream()
                    .flatMap(loaded -> metaLoaders.stream()
                            .map(v -> v.getOrNull(loaded) instanceof IBlockMeta<?> bb ? bb : null)
                            .filter(Objects::nonNull)
                    )
                    .collect(Collectors.toList());
        }
    }

    public static void update() {
        HashMap<UUID, LoadedEntity> loads = new HashMap<>();
        HashMap<Position, LoadedBlock> loadBlocks = new HashMap<>();

        HashMap<UUID, Marker> markers = new HashMap<>();
        List<LoadedBlock> overrideBlocks = new ArrayList<>();
        Bukkit.getWorlds().forEach(world -> world.getEntitiesByClass(Marker.class).forEach(marker -> {
            JsonObject json = JManager.get(JsonObject.class, marker.getPersistentDataContainer(), "loaded_data", null);
            if (json == null || !json.has("type") || !json.has("data")) return;
            UUID uuid = marker.getUniqueId();
            ConcurrentHashMap<String, JsonElement> data = new ConcurrentHashMap<>();
            json.get("data").getAsJsonObject().entrySet().forEach(kv -> data.put(kv.getKey(), kv.getValue()));
            switch (json.get("type").getAsString()) {
                case "entity":
                    LoadedEntity loadedEntity = new LoadedEntity(marker, data);
                    if (loadedEntity.getData().size() == 0) {
                        marker.remove();
                        return;
                    }
                    markers.put(uuid, marker);
                    loads.put(uuid, loadedEntity);
                    return;
                case "block":
                    LoadedBlock loadedBlock = new LoadedBlock(marker, marker.getLocation(), data);
                    if (loadedBlock.getData().size() == 0) {
                        marker.remove();
                        return;
                    }
                    markers.put(uuid, marker);
                    LoadedBlock already = loadBlocks.put(loadedBlock.position, loadedBlock);
                    if (already != null) overrideBlocks.add(already);
                    return;
            }
        }));
        LoadedBlock.setMeta.entrySet().removeIf(kv -> {
            LoadedBlock loaded = loadBlocks.getOrDefault(kv.getKey(), null);
            if (loaded == null) {
                Location location = kv.getKey().getLocation();
                system.Toast1<Boolean> remove = system.toast(false);
                Marker _marker = location.getWorld().spawn(location, Marker.class, marker -> {
                    LoadedBlock _loaded = new LoadedBlock(marker, location, new ConcurrentHashMap<>());
                    kv.getValue().forEach(add -> {
                        IBlockMeta<?> imeta = (IBlockMeta<?>)_loaded.get(add.tClass, null);
                        if (!add.filter(imeta)) return;
                        _loaded.save(imeta);
                    });
                    JsonObject data = new JsonObject();
                    _loaded.getData().forEach(data::add);
                    if (data.size() == 0) {
                        remove.val0 = true;
                        return;
                    }
                    JManager.set(marker.getPersistentDataContainer(), "loaded_data", system.json.object()
                            .add("type", "block")
                            .add("data", data)
                            .build());
                    loadBlocks.put(_loaded.getKey(), _loaded);
                });
                if (remove.val0) {
                    _marker.remove();
                    return true;
                }
                markers.put(_marker.getUniqueId(), _marker);
                return true;
            }
            kv.getValue().forEach(add -> {
                IBlockMeta<?> imeta = add.getOrNull(loaded) instanceof IBlockMeta<?> bb ? bb : null;
                if (imeta == null || !add.filter(imeta)) return;
                loaded.save(imeta);
            });
            return true;
        });

        //List<String> lines = new ArrayList<>();
        //lines.add("Pre: " + LoadedBlock.sync.syncList.size());

        HashMap<UUID, HashMap<Class<? extends IMeta<?, LoadedEntity>>, List<system.Func1<IMeta<?, LoadedEntity>, Boolean>>>> syncEntities = LoadedEntity.sync.getSync(loads);
        HashMap<Position, HashMap<Class<? extends IMeta<?, LoadedBlock>>, List<system.Func1<IMeta<?, LoadedBlock>, Boolean>>>> syncBlocks = LoadedBlock.sync.getSync(loadBlocks);

        //lines.add("Total: " + syncBlocks.size());

        LoadedEntity.loadedData.putAll(loads);
        LoadedEntity.loadedData.entrySet().removeIf(kv -> {
            LoadedEntity loaded = loads.getOrDefault(kv.getKey(), null);
            if (loaded == null) return true;

            HashMap<Class<? extends IMeta<?, LoadedEntity>>, List<system.Func1<IMeta<?, LoadedEntity>, Boolean>>> syncMap = syncEntities.getOrDefault(loaded.getKey(), null);

            //lines.add(" - Map: " + (syncMap == null ? "?" : String.valueOf(syncMap.size())));
            system.Toast1<Boolean> isEdit = system.toast(false);
            metaLoaders.forEach(metaLoader -> {
                IEntityMeta<?> meta = metaLoader.getOrNull(loaded) instanceof IEntityMeta<?> bb ? bb : null;
                if (meta == null) return;
                if (metaLoader.filter(meta)) {
                    List<system.Func1<IMeta<?, LoadedEntity>, Boolean>> list = syncMap == null ? null : syncMap.getOrDefault(metaLoader.tClass, null);
                    if (list != null) {
                        for (system.Func1<IMeta<?, LoadedEntity>, Boolean> func : list)
                            if (func.invoke(meta))
                                isEdit.val0 = true;
                    }
                    if (metaLoader.update(meta)) isEdit.val0 = true;
                    IMeta<?,?> _meta = meta;
                    if (_meta.removed) {
                        isEdit.val0 = true;
                        meta.destroy();
                        return;
                    }
                    if (isEdit.val0) loaded.save(meta);
                }
                else {
                    loaded.remove(meta);
                    isEdit.val0 = true;
                    meta.destroy();
                }
            });
            if (isEdit.val0) {
                Marker marker = markers.get(loaded.getUniqueId());
                JsonObject data = new JsonObject();
                loaded.getData().forEach(data::add);
                if (data.size() == 0) {
                    marker.remove();
                    return true;
                }
                JManager.set(marker.getPersistentDataContainer(), "loaded_data", system.json.object()
                        .add("type", "entity")
                        .add("data", data)
                        .build());
            }
            return false;
        });
        overrideBlocks.forEach(loaded -> {
            metaLoaders.forEach(metaLoader -> {
                IBlockMeta<?> meta = metaLoader.getOrNull(loaded) instanceof IBlockMeta<?> bb ? bb : null;
                if (meta == null) return;
                loaded.remove(meta);
                meta.destroy();
            });
            markers.get(loaded.getUniqueId()).remove();
        });
        LoadedBlock.loadedData.putAll(loadBlocks);
        LoadedBlock.loadedData.entrySet().removeIf(kv -> {
            LoadedBlock loaded = loadBlocks.getOrDefault(kv.getKey(), null);
            if (loaded == null) return true;

            HashMap<Class<? extends IMeta<?, LoadedBlock>>, List<system.Func1<IMeta<?, LoadedBlock>, Boolean>>> syncMap = syncBlocks.getOrDefault(loaded.getKey(), null);

            //lines.add(" - Map: " + (syncMap == null ? "?" : String.valueOf(syncMap.size())));
            system.Toast1<Boolean> isEdit = system.toast(false);
            metaLoaders.forEach(metaLoader -> {
                IBlockMeta<?> meta = metaLoader.getOrNull(loaded) instanceof IBlockMeta<?> bb ? bb : null;
                if (meta == null) return;
                if (metaLoader.filter(meta)) {
                    List<system.Func1<IMeta<?, LoadedBlock>, Boolean>> list = syncMap == null ? null : syncMap.getOrDefault(metaLoader.tClass, null);
                    if (list != null) {
                        for (system.Func1<IMeta<?, LoadedBlock>, Boolean> func : list)
                            if (func.invoke(meta))
                                isEdit.val0 = true;
                    }
                    if (metaLoader.update(meta)) isEdit.val0 = true;
                    IMeta<?,?> _meta = meta;
                    if (_meta.removed) {
                        isEdit.val0 = true;
                        meta.destroy();
                        return;
                    }
                    if (isEdit.val0) loaded.save(meta);
                }
                else {
                    loaded.remove(meta);
                    isEdit.val0 = true;
                    meta.destroy();
                }
            });
            if (isEdit.val0) {
                Marker marker = markers.get(loaded.getUniqueId());
                JsonObject data = new JsonObject();
                loaded.getData().forEach(data::add);
                if (data.size() == 0) {
                    marker.remove();
                    return true;
                }
                JManager.set(marker.getPersistentDataContainer(), "loaded_data", system.json.object()
                    .add("type", "block")
                    .add("data", data)
                    .build());
            }
            return false;
        });

        //ScoreboardUI.SendFakeScoreboard(Bukkit.getOnlinePlayers(), "Blocks: " + loadBlocks.size(), lines);
    }

    public static void onBlockMove(BlockPistonEvent e, List<Block> blocks) {
        for (Block block : blocks) {
            if (LoadedBlock.getReadOnly(block) == null) continue;
            e.setCancelled(true);
            return;
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public static void onBlockPiston(BlockPistonExtendEvent e) {
        onBlockMove(e, e.getBlocks());
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public static void onBlockPiston(BlockPistonRetractEvent e) {
        onBlockMove(e, e.getBlocks());
    }
    @EventHandler public static void on(ChunkLoadEvent e) { on(e.getChunk()); }
    private static void on(Chunk chunk) {
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        JsonObject json = JManager.get(JsonObject.class, container, "meta_container", null);
        if (json == null || !json.has("block")) return;
        json.get("block")
                .getAsJsonObject()
                .entrySet()
                .stream()
                .map(kv -> {
                    String[] args = kv.getKey().split(" ");
                    return system.toast(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), kv.getValue().getAsJsonObject());
                })
                .forEach(kv -> {
                    if (kv.val3.size() == 0) return;
                    Location location = chunk.getBlock(kv.val0, kv.val1, kv.val2).getLocation();
                    location.getWorld().spawn(location, Marker.class, marker -> {
                        JManager.set(marker.getPersistentDataContainer(), "loaded_data", system.json.object()
                                .add("type", "block")
                                .add("data", kv.val3)
                                .build());
                    });
                });
        JManager.del(container, "meta_container");
    }
    private static void tryInit(Block block) {
        List<MetaLoader<?, LoadedBlock>> adds = new ArrayList<>();
        metaLoaders.forEach(meta -> {
            if (meta.iClass.isAssignableFrom(LoadedBlock.class) && meta.canPlace(block))
                adds.add((MetaLoader<?, LoadedBlock>)meta);
        });
        if (adds.size() == 0) return;
        LoadedBlock.set(Position.of(block), adds);
    }
    public static void setBlockMeta(Position position, IBlockMeta<?> meta) {
        setBlockMeta(position, v -> meta);
    }
    public static void setBlockMeta(Position position, system.Func1<LoadedBlock, IBlockMeta<?>> meta) {
        Location location = position.getLocation();
        position.world.spawn(location, Marker.class, marker -> {
            LoadedBlock loaded = new LoadedBlock(marker, location, new ConcurrentHashMap<>());
            loaded.save(meta.invoke(loaded));
            JsonObject data = new JsonObject();
            loaded.getData().forEach(data::add);
            JManager.set(marker.getPersistentDataContainer(), "loaded_data", system.json.object()
                    .add("type", "block")
                    .add("data", data)
                    .build());
        });
    }
    public static void spawnEntityMeta(Location location, IEntityMeta<?> meta) {
        spawnEntityMeta(location, v -> meta);
    }
    public static void spawnEntityMeta(Location location, system.Func1<LoadedEntity, IEntityMeta<?>> meta) {
        location.getWorld().spawn(location, Marker.class, marker -> {
            LoadedEntity loaded = new LoadedEntity(marker, new ConcurrentHashMap<>());
            loaded.save(meta.invoke(loaded));
            JsonObject data = new JsonObject();
            loaded.getData().forEach(data::add);
            JManager.set(marker.getPersistentDataContainer(), "loaded_data", system.json.object()
                    .add("type", "entity")
                    .add("data", data)
                    .build());
        });
    }
    @EventHandler public static void on(PopulateLootEvent e) {
        Vec3D pos = e.getOrDefault(PopulateLootEvent.Parameters.Origin, null);
        if (pos == null || !e.has(PopulateLootEvent.Parameters.BlockState)) return;
        LoadedBlock
                .getReadOnlyAll(Position.of(e.getCraftWorld(), new Vector(pos.getX(), pos.getY(), pos.getZ())))
                .forEach(meta -> meta.populate(e));
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public static void OnBlockPhysic(BlockPhysicsEvent e) {
        Block block = e.getBlock();
        if (LoadedBlock.getReadOnly(block) != null) e.setCancelled(true);
        tryInit(block);
    }
    @EventHandler public static void on(BlockPlaceEvent e) {
        if (LoadedBlock.getReadOnly(e.getBlockPlaced()) == null) return;
        e.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST) public static void on(PlayerInteractEvent e) {
        Block _block = e.getClickedBlock();
        if (_block == null) return;
        for (IBlockMeta<?> meta : LoadedBlock.getReadOnlyAll(_block)) {
            if (meta instanceof ItemManager.BlockSetting.IBlock<?> block) {
                ItemManager.MenuBlockSetting setting = block.getCreator().map(v -> v.getOrNull(ItemManager.MenuBlockSetting.class)).orElse(null);
                if (setting == null) continue;
                if (setting.tryOpen(block.owner(), e)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    public interface IIMeta<I extends ILoaded<?, I>> {
        I getLoaded();
        default World getWorld() { return getLoaded().getWorld(); }
        default Location getLocation() { return getLoaded().getLocation(); }
        default Location getLocation(double x, double y, double z) { return getLoaded().getLocation(x,y,z); }
        default Location getCenterLocation(double x, double y, double z) { return getLoaded().getCenterLocation(x,y,z); }
        default Location getCenterLocation() { return getLoaded().getCenterLocation(); }

        default String getKey() { return getLoaded().getUniqueId().toString(); }
        default String getKey(String prefix) { return prefix + ":" + getKey(); }
        default String getFullKey() { return this.getClass().getName() + "^" + getKey(); }
    }
    public static abstract class IMeta<T extends JsonElement, I extends ILoaded<?, I>> implements IIMeta<I>{
        private I loaded;
        private boolean removed = false;
        public I getLoaded() { return loaded; }

        public void init(I loaded, JsonElement json) {
            try {
                this.loaded = loaded;
                if (json == null || json.isJsonNull()) {
                    if (json != null && json.isJsonNull()) lime.logOP("NULL: " + getKey());
                    create();
                }
                else read((T)json);
            } catch (Exception e) {
                lime.logOP("ERROR META - " + system.getString(getLoaded().getLocation().toVector()) + ": "+getFullKey()+"\n" + e.getMessage());
                throw new IllegalArgumentException(e);
            }
        }
        public void remove() {
            removed = true;
            this.getLoaded().remove(this);
        }

        public abstract void create();
        public abstract void destroy();
        public abstract void read(T json);
        public abstract T write();

        public static void showParticle(Location location, Particle particle) { location.getWorld().spawnParticle(particle, location, 0, 0, 0, 0); }
        public static void showParticle(Location location) { showParticle(location, Particle.FLAME); }

        public static void showParticle(Player player, Vector position, Particle particle) { player.spawnParticle(particle, position.getX(), position.getY(), position.getZ(), 0, 0, 0, 0); }
        public static void showParticle(Player player, Vector position) { showParticle(player, position, Particle.FLAME); }
    }
    public static abstract class IEntityMeta<T extends JsonElement> extends IMeta<T, LoadedEntity> {
        public void teleport(Location location) { edit(marker -> marker.teleport(location)); }
        public void teleport(World world, double x, double y, double z) { teleport(new Location(world,x,y,z)); }
        public void teleport(double x, double y, double z) { edit(marker -> marker.teleport(new Location(marker.getWorld(), x, y, z))); }
        public void edit(system.Action1<Marker> invoke) { lime.nextTick(() -> invoke.invoke(getLoaded().getMarker())); }
    }
    public static abstract class IBlockMeta<T extends JsonElement> extends IMeta<T, LoadedBlock> {
        public void populate(PopulateLootEvent e) { }
    }
}













































