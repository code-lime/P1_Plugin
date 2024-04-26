package org.lime.gp.entity.component.display.instance;

import net.minecraft.world.entity.EntityLimeMarker;
import net.minecraft.world.entity.EntityMarkerEventDestroy;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.gp.block.component.display.event.ChunkCoordCache;
import org.lime.gp.block.component.display.instance.TickTimeInfo;
import org.lime.gp.block.component.display.instance.UnmodifiableMergeMap;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.Entities;
import org.lime.gp.entity.EntityInstance;
import org.lime.gp.entity.component.display.EntityDisplay;
import org.lime.gp.entity.component.display.display.EntityModelDisplay;
import org.lime.gp.entity.component.display.partial.Partial;
import org.lime.gp.entity.component.display.partial.list.ModelPartial;
import org.lime.gp.entity.component.list.DisplayComponent;
import org.lime.gp.entity.event.EntityMarkerEventTick;
import org.lime.gp.extension.ProxyMap;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.module.TimeoutData;
import org.lime.json.JsonObjectOptional;
import org.lime.system.execute.Func1;
import org.lime.system.json;
import org.lime.system.toast.LockToast1;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast3;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public final class DisplayInstance extends EntityInstance implements
        CustomEntityMetadata.Tickable,
        CustomEntityMetadata.AsyncTickable,
        CustomEntityMetadata.Destroyable
{
    private final LockToast1<Long> variableIndex = Toast.lock(1L);
    private final ConcurrentHashMap<String, String> variables = new ConcurrentHashMap<>();
    private final Map<String, String> unmodifiableVariables = Collections.unmodifiableMap(variables);
    private final ConcurrentHashMap<UUID, Partial> partials = new ConcurrentHashMap<>();

    @Override public DisplayComponent component() { return (DisplayComponent)super.component(); }

    public DisplayInstance(DisplayComponent component, CustomEntityMetadata metadata) {
        super(component, metadata);
    }
    public void variableDirty() {
        variables.put(".index", String.valueOf(variableIndex.edit0(v -> v + 1)));
        saveData();
    }
    public void set(String key, String value) {
        if (Objects.equals(variables.put(key, value), value)) return;
        variableDirty();
    }
    public void set(Map<String, String> variables) {
        this.variables.putAll(variables);
        variableDirty();
    }
    public void modify(Func1<Map<String, String>, Boolean> modifyFunc) {
        if (!modifyFunc.invoke(variables)) return;
        variableDirty();
    }
    public Optional<String> get(String key) { return Optional.ofNullable(variables.get(key)); }
    public Map<String, String> getAll() { return unmodifiableVariables; }
    public long variableIndex() { return variableIndex.get0(); }

    public Optional<Partial> getPartial(int distanceChunk, Map<String, String> variables) {
        return Optional.ofNullable(component().partials.get(distanceChunk)).map(v -> v.partial(variables));
    }

    @Override public void read(JsonObjectOptional json) {
        variables.clear();
        json.forEach((key, value) -> value.getAsString()
                .or(() -> value.getAsNumber().map(Object::toString))
                .or(() -> value.getAsBoolean().map(Object::toString))
                .ifPresent(v -> variables.put(key, v)));
        variableDirty();
    }
    @Override public json.builder.object write() { return json.object().add(variables, k -> k, v -> v); }

    private final ConcurrentHashMap<String, Object> animationData = new ConcurrentHashMap<>();

    private long lastUpdateDirtyIndex = -1;
    private static final ConcurrentLinkedQueue<UUID> dirtyQueue = new ConcurrentLinkedQueue<>();
    private static final List<UUID> cachedDirtyQueue = new ArrayList<>();
    public static void appendDirtyQueue(UUID uuid) {
        dirtyQueue.add(uuid);
    }

    private static long lastTickID = -1;
    private final HashMap<EntityModelDisplay.EntityModelKey, DisplayObject> modelMap = new HashMap<>();

    private @Nullable Toast3<Vector, Float, Float> lastPosition = null;
    @Override public void onAsyncTick(CustomEntityMetadata metadata, long tick) {
        if (lastTickID != tick) {
            lastTickID = tick;
            UUID uuid;
            cachedDirtyQueue.clear();
            while ((uuid = dirtyQueue.poll()) != null) cachedDirtyQueue.add(uuid);
        }
        TickTimeInfo tickTimeInfo = Entities.deltaTime.get0();
        tickTimeInfo.resetTime();
        tickTimeInfo.calls++;

        long currUpdateDirtyIndex = variableIndex();

        Set<UUID> onlineUUIDs = EntityPosition.onlinePlayers.keySet();
        modelMap.values().removeIf(data -> {
            data.removeViewersIf(uuid -> !onlineUUIDs.contains(uuid));
            return data.isViewersEmpty();
        });
        partials.keySet().removeIf(uuid -> !onlineUUIDs.contains(uuid));
        tickTimeInfo.filter_ns += tickTimeInfo.nextTime();

        Collection<UUID> users;
        EntityLimeMarker marker = metadata.marker;
        Vector pos = new Vector(marker.getX(), marker.getY(), marker.getZ());
        float yaw = marker.getBukkitYaw();
        float pitch = marker.getXRot();

        Toast3<Vector, Float, Float> raw = Toast.of(pos, yaw, pitch);

        if (currUpdateDirtyIndex != lastUpdateDirtyIndex || !raw.equals(lastPosition)) {
            users = onlineUUIDs;
            lastUpdateDirtyIndex = currUpdateDirtyIndex;
            modelMap.clear();
        } else {
            users = cachedDirtyQueue;
            if (!users.isEmpty()) {
                modelMap.values().removeIf(data -> {
                    data.removeViewersIf(users::contains);
                    return data.isViewersEmpty();
                });
            }
        }
        lastPosition = raw;

        if (users.isEmpty()) {
            if (modelMap.size() == 0) TimeoutData.remove(unique(), DisplayMap.class);
            else TimeoutData.put(unique(), DisplayMap.class, new DisplayMap(modelMap));
            tickTimeInfo.users_ns += tickTimeInfo.nextTime();
            return;
        }

        tickTimeInfo.users_ns += tickTimeInfo.nextTime();

        Map<String, String> variables = getAll();

        tickTimeInfo.variables1_ns += tickTimeInfo.nextTime();

        World world = metadata.marker.level().getWorld();
        ChunkCoordCache.Cache entityCoord = ChunkCoordCache.Cache.of(pos.toLocation(world), 0);

        tickTimeInfo.variables2_ns += tickTimeInfo.nextTime();

        HashMap<Player, Integer> shows = new HashMap<>();
        tickTimeInfo.variables3_ns += tickTimeInfo.nextTime();

        users.forEach(uuid -> {
            Player player = EntityPosition.onlinePlayers.get(uuid);
            if (player == null) {
                partials.remove(uuid);
                return;
            }
            Map<String, String> playerVariables = UnmodifiableMergeMap.of(variables, player.getScoreboardTags().stream().collect(Collectors.toMap(v -> "tag." + v, v -> "true")));
            ChunkCoordCache.getCoord(uuid).ifPresentOrElse(cache -> {
                int distanceChunk = cache.distance(entityCoord);
                partials.compute(uuid, (k, v) -> {
                    Partial partial = getPartial(distanceChunk, playerVariables).orElse(null);
                    if (partial == null) return null;
                    if (partial instanceof ModelPartial model) model.model().ifPresent(_model -> modelMap.computeIfAbsent(
                            new EntityModelDisplay.EntityModelKey(marker.getUUID(), _model.unique(), unique()),
                            _k -> DisplayObject.of(pos.toLocation(world, yaw, pitch), _model, animationData)
                    ).addViewer(uuid));
                    shows.put(player, distanceChunk);
                    return partial;
                });
            }, () -> partials.remove(uuid));
        });
        tickTimeInfo.check_ns += tickTimeInfo.nextTime();
        tickTimeInfo.partial_ns += tickTimeInfo.nextTime();

        metadata.list(EntityDisplay.Displayable.class)
                .forEach(displayable -> shows.forEach((player, distanceChunk) -> displayable.onDisplayAsync(player, marker)
                        .filter(v -> distanceChunk <= v.distanceChunk())
                        .flatMap(EntityDisplay.IEntity::model)
                        .ifPresent(_model -> modelMap.computeIfAbsent(
                                new EntityModelDisplay.EntityModelKey(marker.getUUID(), _model.unique(), displayable.unique()),
                                k -> DisplayObject.of(pos.toLocation(world, yaw, pitch), _model, animationData)
                        ).addViewer(player.getUniqueId()))
                ));

        tickTimeInfo.metadata_ns += tickTimeInfo.nextTime();

        if (modelMap.size() == 0) TimeoutData.remove(unique(), DisplayMap.class);
        else TimeoutData.put(unique(), DisplayMap.class, new DisplayMap(modelMap));

        tickTimeInfo.apply_ns += tickTimeInfo.nextTime();
    }
    private final ProxyMap<String, String> proxyVariables = ProxyMap.of(this.variables);
    @Override public void onTick(CustomEntityMetadata metadata, EntityMarkerEventTick event) {
        component().animationTick(getAll(), this.proxyVariables, animationData);
        if (proxyVariables.checkDirty(true)) variableDirty();
    }

    @Override public void onDestroy(CustomEntityMetadata metadata, EntityMarkerEventDestroy event) {
        TimeoutData.remove(unique(), DisplayMap.class);
    }
}



























