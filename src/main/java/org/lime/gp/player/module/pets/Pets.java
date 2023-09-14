package org.lime.gp.player.module.pets;

import com.google.gson.JsonObject;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.display.Displays;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.lime;
import org.lime.gp.module.JavaScript;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.HashMap;

public class Pets {
    public static CoreElement create() {
        return CoreElement.create(Pets.class)
                .<JsonObject>addConfig("pets", v -> v
                        .withDefault(new JsonObject())
                        .withInvoke(Pets::config)
                        .orText("pets.js", _v -> _v
                                .withInvoke(t -> Pets.config(json.parse(JavaScript.getJsString(t).orElseThrow()).getAsJsonObject()))
                                .withDefault("{}")
                        )
                );
    }
    public static final PetDisplayManager MANAGER = new PetDisplayManager();
    public static final HashMap<String, AbstractPet> pets = new HashMap<>();

    private static void config(JsonObject json) {
        HashMap<String, AbstractPet> pets = new HashMap<>();
        json.entrySet().forEach(kv -> pets.put(kv.getKey(), AbstractPet.parse(kv.getKey(), kv.getValue().getAsJsonObject())));

        Displays.uninitDisplay(MANAGER);
        Pets.pets.clear();
        Pets.pets.putAll(pets);
        Displays.initDisplay(MANAGER);
    }
}
