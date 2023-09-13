package org.lime.gp.item.elemental;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.display.transform.LocalLocation;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.lime;

import java.util.HashMap;

public class Elemental {
    public static CoreElement create() {
        return CoreElement.create(Elemental.class)
                .withInit(Elemental::init)
                .<JsonObject>addConfig("elemental", v -> v
                        .withInvoke(Elemental::config)
                        .withDefault(new JsonObject())
                );
    }

    private static final HashMap<String, IStep> steps = new HashMap<>();

    private static void init() {
        AnyEvent.addEvent("elemental.execute", AnyEvent.type.owner, v -> v.createParam(_v -> _v, steps::keySet), (player, elemental) -> {
            execute(player, elemental);
        });
    }
    private static void config(JsonObject json) {
        json = lime.combineParent(json, true, false);
        HashMap<String, IStep> steps = new HashMap<>();
        json.entrySet().forEach(kv -> steps.put(kv.getKey(), IStep.parse(kv.getValue())));
        Elemental.steps.clear();
        Elemental.steps.putAll(steps);
    }
    public static void execute(Player player, String elemental) {
        execute(player, elemental, new LocalLocation(player.getLocation()));
    }
    public static void execute(Player player, String elemental, LocalLocation location) {
        IStep step = steps.get(elemental);
        if (step == null) {
            lime.logOP("Elemental '"+elemental+"' not funded!");
            return;
        }
        step.execute(player, location);
    }
}






