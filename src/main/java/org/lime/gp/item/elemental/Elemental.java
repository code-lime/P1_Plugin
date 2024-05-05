package org.lime.gp.item.elemental;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.lime.ToDoException;
import org.lime.docs.IIndexDocs;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.item.settings.use.target.ILocationTarget;
import org.lime.gp.item.settings.use.target.PlayerTarget;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;
import org.lime.reflection;
import org.lime.system.execute.Execute;
import org.lime.system.toast.Toast;
import org.lime.system.utils.MathUtils;
import org.lime.unsafe;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Stream;

public class Elemental {
    public static CoreElement create() {
        return CoreElement.create(Elemental.class)
                .withInit(Elemental::init)
                .<JsonObject>addConfig("elemental", v -> v
                        .withInvoke(Elemental::config)
                        .withDefault(new JsonObject())
                );
    }


    public static final ImmutableMap<String, Class<? extends IStep<?>>> stepsClasses;
    private static Optional<Constructor<?>> constructor(Class<?> tClass, Class<?>... args) {
        try { return Optional.of(reflection.access(tClass.getDeclaredConstructor(args))); }
        catch (Exception e) { return Optional.empty(); }
    }
    static {
        try {
            String packageFilter = IStep.class.getPackage().getName() + ".";
            stepsClasses = lime._plugin.getJarClassesNames()
                    .stream()
                    .filter(v -> v.startsWith(packageFilter))
                    .map(Execute.<String, Class<?>>funcEx(Class::forName).throwable())
                    .filter(IStep.class::isAssignableFrom)
                    .flatMap(v -> Arrays.stream(v.getAnnotationsByType(Step.class)).map(_c -> Toast.of(_c, v)))
                    .collect(ImmutableMap.toImmutableMap(kv -> kv.val0.name(), kv -> (Class<? extends IStep<?>>)kv.val1));
            lime.logOP("Loaded steps: " + String.join(", ", stepsClasses.keySet()));
        } catch (Exception e) {
            lime.logStackTrace(e);
            throw e;
        }
    }

    private static final HashMap<String, IStep<?>> steps = new HashMap<>();

    private static void init() {
        AnyEvent.addEvent("elemental.execute", AnyEvent.type.owner, v -> v.createParam(_v -> _v, steps::keySet), (player, elemental) -> {
            execute(player, new DataContext(), elemental);
        });
        AnyEvent.addEvent("elemental.execute", AnyEvent.type.other, v -> v.createParam(_v -> _v, steps::keySet), (player, elemental) -> {
            execute(player, new DataContext(), elemental);
        });
    }
    private static void config(JsonObject json) {
        json = lime.combineParent(json, true, false);
        HashMap<String, IStep<?>> steps = new HashMap<>();
        json.entrySet().forEach(kv -> steps.put(kv.getKey(), IStep.parse(kv.getValue())));
        Elemental.steps.clear();
        Elemental.steps.putAll(steps);
    }
    public static void execute(Player player, DataContext context, String elemental) {
        execute(player, context, elemental, MathUtils.transformation(player.getLocation()));
    }
    public static void execute(ILocationTarget target, DataContext context, String elemental) {
        execute(target, context, elemental, MathUtils.transformation(target.getLocation()));
    }
    public static void execute(Player player, DataContext context, String elemental, Transformation location) {
        execute(new PlayerTarget(player), context, elemental, location);
    }
    public static void execute(ILocationTarget target, DataContext context, String elemental, Transformation location) {
        IStep<?> step = steps.get(elemental);
        if (step == null) {
            lime.logOP("Elemental '"+elemental+"' not funded!");
            return;
        }
        step.execute(target, context, location);
    }

    public static IIndexGroup docs(IDocsLink docs) {
        List<IIndexGroup> builders = Elemental.stepsClasses.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(kv -> docs(kv.getKey(), kv.getValue(), docs))
                .sorted(Comparator.comparing(IIndexDocs::index))
                .toList();
        return JsonEnumInfo.of("ELEMENTAL",
                Stream.<IJElement>concat(
                        Stream.of(
                                IJElement.anyList(IJElement.linkCurrent()),
                                IJElement.link(docs.elementalName())
                        ),
                        builders.stream().map(IJElement::link)
                ).collect(ImmutableList.toImmutableList())
        ).withChilds(builders);
    }
    private static IIndexGroup docs(String key, Class<? extends IStep<?>> tClass, IDocsLink docs) {
        try {
            IStep<?> setting = unsafe.createInstance(tClass);
            IIndexGroup dat = setting.docs(key.replace(".", "_").toUpperCase(), docs);
            return dat instanceof JsonGroup group && group.element() instanceof JObject json
                    ? new JsonGroup(group.title(), group.index(), json.addFirst(JProperty.require(IName.raw("type"), IJElement.raw(key))), group.comments())
                    : dat;
        } catch (ToDoException todo) {
            return IIndexGroup.raw(key, null, v -> Stream.of(IComment.warning("TODO: " + todo.getMessage()).line(v)));
        }
    }
}






