package org.lime.gp.block.component;

import com.google.gson.*;
import org.lime.docs.IGroup;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.component.display.IDisplayVariable;
import org.lime.gp.block.component.list.DisplayComponent;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.lime;
import org.lime.system.execute.Execute;
import org.lime.system.toast.Toast;
import org.lime.unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Stream;

public abstract class ComponentStatic<T extends JsonElement> {
    private final BlockInfo _info;
    private final String _name;

    public BlockInfo info() { return this._info; }
    public String name() { return _name; }

    private interface ComponentLink {
        ComponentStatic<?> create(BlockInfo blockInfo, JsonElement json);
        Class<?> tClass();

        String key();
        default IIndexGroup docs(IDocsLink docs) {
            ComponentStatic<?> component = (ComponentStatic<?>)unsafe.createInstance(tClass());
            IIndexGroup group = component.docs(key(), docs);
            if (component instanceof ComponentDynamic<?,?> dynamic
                    && unsafe.createInstance(dynamic.classInstance()) instanceof IDisplayVariable instance)
                group = group.withChild(JsonGroup.of(
                        "Устанавливаемые параметры оторбражения блока",
                        "display_variable",
                        instance.docsDisplayVariable(),
                        "Используется в " + docs.componentsLink(DisplayComponent.class)
                ));
            //"Используется вместе с " + docs.componentsLink(DisplayComponent.class) + ". Параметры блока: "
            return group;
        }
    }

    private static final Map<Class<?>, String> componentKeys;
    private static final Map<String, ComponentLink> components;

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
                    .flatMap(v -> constructor(v, BlockInfo.class, JsonElement.class)
                            .or(() -> constructor(v, BlockInfo.class, JsonArray.class))
                            .or(() -> constructor(v, BlockInfo.class, JsonObject.class))
                            .or(() -> constructor(v, BlockInfo.class, JsonPrimitive.class))
                            .or(() -> constructor(v, BlockInfo.class, JsonNull.class))
                            .or(() -> constructor(v, BlockInfo.class))
                            .stream()
                            .flatMap(c -> Arrays.stream(v.getAnnotationsByType(InfoComponent.Component.class)).map(_c -> Toast.of(_c, c, v)))
                    )
                    .forEach(kv -> {
                        components.put(kv.val0.name(), new ComponentLink() {
                            private final Constructor<?> constructor = kv.val1;
                            private final Class<?> tClass = kv.val2;
                            private final String key = kv.val0.name();
                            @Override public ComponentStatic<?> create(BlockInfo blockInfo, JsonElement json) {
                                try {
                                    return (ComponentStatic<?>) (constructor.getParameterCount() == 2 ? constructor.newInstance(blockInfo, json) : constructor.newInstance(blockInfo));
                                } catch (InvocationTargetException e) {
                                    lime.logStackTrace(e.getCause());
                                    throw new IllegalArgumentException(e.getCause());
                                } catch (Exception e) {
                                    lime.logStackTrace(e);
                                    throw new IllegalArgumentException(e);
                                }
                            }
                            @Override public Class<?> tClass() { return tClass; }
                            @Override public String key() { return key; }
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

    public abstract IIndexGroup docs(String index, IDocsLink docs);

    public static ComponentStatic<?> parse(String key, BlockInfo creator, JsonElement json) {
        ComponentLink link = components.get(key);
        if (link == null) throw new IllegalArgumentException("Block component '"+key+"' not founded!");
        return link.create(creator, json);
    }

    public static String getName(Class<? extends ComponentStatic<?>> tClass) {
        return Arrays.stream(tClass.getAnnotationsByType(InfoComponent.Component.class)).map(InfoComponent.Component::name).findFirst().orElseThrow();
    }
    public static Stream<IGroup> allDocs(IDocsLink docs) {
        return components.values().stream().sorted(Comparator.comparing(ComponentLink::key)).map(link -> link.docs(docs));
    }
}
