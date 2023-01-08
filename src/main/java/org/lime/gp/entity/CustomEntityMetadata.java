package org.lime.gp.entity;

import net.minecraft.world.LimeKey;
import net.minecraft.world.entity.EntityLimeMarker;
import net.minecraft.world.entity.EntityMarkerEventDestroy;
import org.lime.gp.entity.component.ComponentDynamic;
import org.lime.gp.entity.event.EntityMarkerEventInteract;
import org.lime.gp.entity.event.EntityMarkerEventTick;
import org.lime.system;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class CustomEntityMetadata extends EntityMetadata {
    public interface Element { }
    public interface Uniqueable extends Element { UUID unique(); }
    public interface Childable extends Element { Stream<Element> childs(); }

    public interface Tickable extends Element { void onTick(CustomEntityMetadata metadata, EntityMarkerEventTick event); }
    public interface LazyTickable extends Element { void onLazyTick(CustomEntityMetadata metadata, EntityMarkerEventTick event); }
    public interface Destroyable extends Element { void onDestroy(CustomEntityMetadata metadata, EntityMarkerEventDestroy event); }
    public interface Damageable extends Element { void onDamage(CustomEntityMetadata metadata, EntityMarkerEventInteract event); }
    public interface Interactable extends Element { void onInteract(CustomEntityMetadata metadata, EntityMarkerEventInteract event); }

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

    private static Stream<Element> childsAndThis(Childable childable) {
        return Stream.concat(Stream.of(childable), childable.childs().flatMap(v -> v instanceof Childable c ? childsAndThis(c) : Stream.of(v)));
    }
    @SuppressWarnings("unchecked")
    public <T extends Element>Stream<T> list(Class<T> tClass) {
        Stream<Object> stream = instances.values().stream().map(v -> v);
        if (info != null) stream = Stream.concat(stream, info.components.values().stream());
        return stream
                .flatMap(v -> v instanceof Childable childable ? childsAndThis(childable) : Stream.of(v))
                .filter(tClass::isInstance)
                .map(v -> (T)v);
    }

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
                    }
                    info.components.forEach((key, component) -> {
                        if (component instanceof Tickable tickable) tickable.onTick(this, event);
                        if (!(component instanceof ComponentDynamic<?, ?> dynamicComponent)) return;
                        instances.compute(key, (k,v) -> {
                            if (v == null) v = dynamicComponent.createInstance(this).loadData();
                            if (v instanceof Tickable tickable) tickable.onTick(this, event);
                            return v;
                        });
                    });
                    system.Toast1<Boolean> isLazy = system.toast(null);
                    list(LazyTickable.class)
                            .filter(v -> isLazy.val0 == null
                                    ? (isLazy.val0 = marker.level.hasNearbyAlivePlayer(marker.getX(), marker.getY(), marker.getZ(), info.distance))
                                    : isLazy.val0
                            )
                            .forEach(v -> v.onLazyTick(this, event));
                }, () -> instances.entrySet().removeIf(kv -> {
                    kv.getValue().saveData();
                    return true;
                }));
    }
    @Override public void onDestroy(EntityMarkerEventDestroy event) { list(Destroyable.class).forEach(v -> v.onDestroy(this, event)); }
    @Override public void onInteract(EntityMarkerEventInteract event) { list(Interactable.class).forEach(v -> v.onInteract(this, event)); }
    @Override public void onDamage(EntityMarkerEventInteract event) { list(Damageable.class).forEach(v -> v.onDamage(this, event)); }
}













