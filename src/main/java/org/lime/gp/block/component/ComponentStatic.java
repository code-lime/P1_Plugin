package org.lime.gp.block.component;

import com.google.gson.*;
import org.lime.gp.block.BlockInfo;
import org.lime.system.Func2;
import org.lime.gp.lime;
import org.lime.system;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class ComponentStatic<T extends JsonElement> {
    private final BlockInfo _info;
    private final String _name;

    public BlockInfo info() { return this._info; }
    public String name() { return _name; }

    private static final Map<Class<?>, String> componentKeys;
    private static final Map<String, system.Func2<BlockInfo, JsonElement, ComponentStatic<?>>> components;

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
                    .map(system.<String, Class<?>>funcEx(Class::forName).throwable())
            //Stream.of(Components.class.getDeclaredClasses())
                    .filter(ComponentStatic.class::isAssignableFrom)
                    .map(v -> constructor(v, BlockInfo.class, JsonElement.class)
                            .or(() -> constructor(v, BlockInfo.class, JsonArray.class))
                            .or(() -> constructor(v, BlockInfo.class, JsonObject.class))
                            .or(() -> constructor(v, BlockInfo.class, JsonPrimitive.class))
                            .or(() -> constructor(v, BlockInfo.class, JsonNull.class))
                            .or(() -> constructor(v, BlockInfo.class))
                            .map(c -> system.toast(v.getAnnotation(InfoComponent.Component.class), c))
                            .orElse(null))
                    .filter(Objects::nonNull)
                    .filter(kv -> kv.val0 != null)
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

    public ComponentStatic(BlockInfo info) {
        this._info = info;
        this._name = componentKeys.getOrDefault(this.getClass(), null);
    }
    public ComponentStatic(BlockInfo creator, T json) {
        this(creator);
    }

    public static ComponentStatic<?> parse(String key, BlockInfo creator, JsonElement json) {
        Func2<BlockInfo, JsonElement, ComponentStatic<?>> func = components.get(key);
        if (func == null) throw new IllegalArgumentException("Block component '"+key+"' not founded!");
        return func.invoke(creator, json);
    }
}
