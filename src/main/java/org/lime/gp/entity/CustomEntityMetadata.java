package org.lime.gp.entity;

import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.LimeKey;
import net.minecraft.world.entity.EntityLimeMarker;
import net.minecraft.world.entity.EntityMarkerEventDestroy;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.inventory.EquipmentSlot;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.entity.component.ComponentDynamic;
import org.lime.gp.entity.event.EntityMarkerEventInput;
import org.lime.gp.entity.event.EntityMarkerEventInteract;
import org.lime.gp.entity.event.EntityMarkerEventTick;
import org.lime.gp.module.TimeoutData;
import org.lime.gp.module.loot.PopulateLootEvent;
import org.lime.system.execute.Execute;
import org.lime.system.execute.Func1;
import org.lime.system.toast.*;
import org.lime.system.utils.IterableUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class CustomEntityMetadata extends EntityMetadata {
    public interface Element { }
    public interface Uniqueable extends Element { UUID unique(); }
    public interface Childable extends Element { Stream<Element> childs(); }

    public interface Tickable extends Element { void onTick(CustomEntityMetadata metadata, EntityMarkerEventTick event); }
    public interface AsyncTickable extends Element { void onAsyncTick(CustomEntityMetadata metadata, long tick); }
    public interface FirstTickable extends Element { void onFirstTick(CustomEntityMetadata metadata, EntityMarkerEventTick event); }
    public interface LazyTickable extends Element { void onLazyTick(CustomEntityMetadata metadata, EntityMarkerEventTick event); }
    public interface Destroyable extends Element { void onDestroy(CustomEntityMetadata metadata, EntityMarkerEventDestroy event); }
    public interface Damageable extends Element { void onDamage(CustomEntityMetadata metadata, EntityMarkerEventInteract event); }
    public interface Interactable extends Element { EnumInteractionResult onInteract(CustomEntityMetadata metadata, EntityMarkerEventInteract event); }
    public interface Inputable extends Element { void onInput(CustomEntityMetadata metadata, EntityMarkerEventInput event); }
    public interface Lootable extends Element { void onLoot(CustomEntityMetadata metadata, PopulateLootEvent event); }

    public final LimeKey key;
    public final ConcurrentHashMap<String, EntityInstance> instances = new ConcurrentHashMap<>();
    public EntityInfo info = null;

    private long loadIndex = -1;

    public CustomEntityMetadata(LimeKey key, EntityLimeMarker marker) {
        super(marker);
        this.key = key;

        Entities.creator(key.type()).ifPresentOrElse(info -> {
                    if (info.getLoadIndex() != loadIndex) {
                        instances.entrySet().removeIf(kv -> {
                            kv.getValue().saveData();
                            return true;
                        });
                        loadIndex = info.getLoadIndex();
                        this.info = info;
                    }
                    info.components.forEach((_key, component) -> {
                        if (!(component instanceof ComponentDynamic<?, ?> dynamicComponent)) return;
                        instances.compute(_key, (k,v) -> {
                            if (v == null) v = dynamicComponent.createInstance(this).loadData();
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
                EntityInstance instance = instances.get(key);
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

    public static class EntityTimeout extends TimeoutData.ITimeout {
        public final CustomEntityMetadata lastMetadata;

        public EntityTimeout(CustomEntityMetadata lastMetadata) {
            this.lastMetadata = lastMetadata;
        }
    }
    private boolean isFirst = true;
    @Override public void onTick(EntityMarkerEventTick event) {
        Entities.creator(key.type())
                .ifPresentOrElse(info -> {
                    if (info.getLoadIndex() != loadIndex) {
                        instances.entrySet().removeIf(kv -> {
                            kv.getValue().saveData();
                            return true;
                        });
                        loadIndex = info.getLoadIndex();
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
                    EntityTimeout timeout = new EntityTimeout(this);
                    TimeoutData.put(key.uuid(), EntityTimeout.class, timeout);

                    boolean firstTick = isFirst;
                    if (firstTick) isFirst = false;
                    list(Tickable.class).forEach(tickable -> tickable.onTick(this, event));
                    if (firstTick) list(FirstTickable.class).forEach(tickable -> tickable.onFirstTick(this, event));

                    Toast1<Boolean> isLazy = Toast.of(null);
                    list(LazyTickable.class)
                            .filter(v -> isLazy.val0 == null
                                    ? (isLazy.val0 = marker.level().hasNearbyAlivePlayer(marker.getX(), marker.getY(), marker.getZ(), info.distance))
                                    : isLazy.val0
                            )
                            .forEach(v -> v.onLazyTick(this, event));
                }, () -> instances.entrySet().removeIf(kv -> {
                    kv.getValue().saveData();
                    return true;
                }));
    }
    @Override public void onDestroy(EntityMarkerEventDestroy event) {
        list(Destroyable.class).forEach(v -> v.onDestroy(this, event));
        TimeoutData.remove(key.uuid(), EntityTimeout.class);
    }
    private final Map<Integer, Integer> interactLocker = new HashMap<>();
    @Override public void onInteract(EntityMarkerEventInteract event) {
        if (!(event.getPlayer() instanceof CraftPlayer cplayer)) return;
        EntityPlayer player = cplayer.getHandle();
        int playerID = player.getId();
        if (event.getHand() == EquipmentSlot.HAND) interactLocker.remove(playerID);
        else if (interactLocker.remove(playerID) != null) return;
        EnumInteractionResult result = EnumInteractionResult.CONSUME;
        for (CustomEntityMetadata.Interactable interactable : IterableUtils.iterable(list(CustomEntityMetadata.Interactable.class)))  {
            EnumInteractionResult _result = interactable.onInteract(this, event);
            if (!_result.consumesAction()) continue;
            result = _result;
            break;
        }
        if (result.consumesAction()) player.containerMenu.sendAllDataToRemote();
        if (event.getHand() == EquipmentSlot.HAND && result.consumesAction()) interactLocker.put(playerID, 2);
    }
    @Override public void onInput(EntityMarkerEventInput event) { list(Inputable.class).forEach(v -> v.onInput(this, event)); }
    @Override public void onDamage(EntityMarkerEventInteract event) { list(Damageable.class).forEach(v -> v.onDamage(this, event)); }
    @Override public void onLoot(PopulateLootEvent event) { list(Lootable.class).forEach(v -> v.onLoot(this, event)); }
    @Override public void onTickAsync(long tick) { list(AsyncTickable.class).forEach(v -> v.onAsyncTick(this, tick)); }

    public void destroyWithLoot(Func1<LootParams.a, LootParams.a> params) {
        MinecraftKey minecraftkey = new MinecraftKey("lime", key.type());
        LootParams.a lootparams = new LootParams.a((WorldServer)marker.level())
                .withParameter(LootContextParameters.THIS_ENTITY, marker)
                .withParameter(LootContextParameters.ORIGIN, marker.position())
                .withParameter(LootContextParameters.DAMAGE_SOURCE, marker.damageSources().generic());

        LootTableInfo context = new LootTableInfo.Builder(params.invoke(lootparams).create(LootContextParameterSets.ENTITY))
                .create(minecraftkey);

        PopulateLootEvent.executeLootTable(minecraftkey, context, marker::spawnAtLocation, Execute.actionEmpty());
        destroy();
    }
}













