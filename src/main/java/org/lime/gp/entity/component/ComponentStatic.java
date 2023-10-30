package org.lime.gp.entity.component;

import com.google.gson.*;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityInfo;
import org.lime.gp.lime;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class ComponentStatic<T extends JsonElement> implements CustomEntityMetadata.Element {
    private final EntityInfo _info;
    private final String _name;

    public EntityInfo info() { return this._info; }
    public String name() { return _name; }

    private static final Map<Class<?>, String> componentKeys;
    private static final Map<String, Func2<EntityInfo, JsonElement, ComponentStatic<?>>> components;

    private static Optional<Constructor<?>> constructor(Class<?> tClass, Class<?>... args) {
        try { return Optional.of(org.lime.reflection.access(tClass.getDeclaredConstructor(args))); }
        catch (Exception e) { return Optional.empty(); }
    }

    static {
        try {
            componentKeys = new HashMap<>();
            components = new HashMap<>();
            String packageFilter = ComponentStatic.class.getPackage().getName() + ".list.";
            lime._plugin.getJarClassesNames()
                    .stream()
                    .filter(v -> v.startsWith(packageFilter))
                    .map(Execute.<String, Class<?>>funcEx(Class::forName).throwable())
                    .filter(ComponentStatic.class::isAssignableFrom)
                    .flatMap(v -> constructor(v, EntityInfo.class, JsonElement.class)
                            .or(() -> constructor(v, EntityInfo.class, JsonArray.class))
                            .or(() -> constructor(v, EntityInfo.class, JsonObject.class))
                            .or(() -> constructor(v, EntityInfo.class, JsonPrimitive.class))
                            .or(() -> constructor(v, EntityInfo.class, JsonNull.class))
                            .or(() -> constructor(v, EntityInfo.class))
                            .stream()
                            .flatMap(c -> Arrays.stream(v.getAnnotationsByType(InfoComponent.Component.class)).map(_c -> Toast.of(_c, c)))
                    )
                    .forEach(kv -> {
                        components.put(kv.val0.name(), (creator, json) -> {
                            try {
                                return (ComponentStatic<?>) (kv.val1.getParameterCount() == 2 ? kv.val1.newInstance(creator, json) : kv.val1.newInstance(creator));
                            } catch (InvocationTargetException e) {
                                lime.logStackTrace(e.getCause());
                                throw new IllegalArgumentException(e.getCause());
                            } catch (Exception e) {
                                lime.logStackTrace(e);
                                throw new IllegalArgumentException(e);
                            }
                        });
                        componentKeys.put(kv.val1.getDeclaringClass(), kv.val0.name());
                    });
        } catch (Exception e) {
            lime.logStackTrace(e);
            throw e;
        }
        components.keySet().forEach(k -> lime.logOP("Component: " + k));
    }

    public ComponentStatic(EntityInfo info) {
        this._info = info;
        this._name = componentKeys.getOrDefault(this.getClass(), null);
    }
    public ComponentStatic(EntityInfo creator, T json) {
        this(creator);
    }

    public static ComponentStatic<?> parse(String key, EntityInfo creator, JsonElement json) {
        return components.get(key).invoke(creator, json);
    }
}
