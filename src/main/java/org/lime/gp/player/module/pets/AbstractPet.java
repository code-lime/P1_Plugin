package org.lime.gp.player.module.pets;

import com.google.gson.JsonObject;
import org.lime.display.models.ChildDisplay;
import org.lime.display.models.Model;

import java.util.Map;

public abstract class AbstractPet {
    public final String key;
    public final double speed;
    public final int steps;

    public abstract Model model();

    public abstract void tick(ChildDisplay<?> model, Map<String, Object> data);

    protected AbstractPet(String key, JsonObject json) {
        this.key = key;
        this.speed = json.has("speed") ? json.get("speed").getAsDouble() : 0.05;
        this.steps = json.has("steps") ? json.get("steps").getAsInt() : 500;
    }

    public static AbstractPet parse(String key, JsonObject json) {
        if (json.has("model")) return new ModelPet(key, json);
        return new VariablePet(key, json);
    }
}
