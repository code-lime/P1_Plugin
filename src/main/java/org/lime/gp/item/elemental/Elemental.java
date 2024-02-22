package org.lime.gp.item.elemental;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;
import org.lime.reflection;
import org.lime.system.execute.Execute;
import org.lime.system.toast.Toast;
import org.lime.system.utils.MathUtils;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

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
    public static void execute(Player player, DataContext context, String elemental, Transformation location) {
        IStep<?> step = steps.get(elemental);
        if (step == null) {
            lime.logOP("Elemental '"+elemental+"' not funded!");
            return;
        }
        step.execute(player, context, location);
    }
}






