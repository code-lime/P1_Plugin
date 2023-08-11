package org.lime.gp.player.module.needs.food;

import org.lime.gp.lime;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

public class VanillaFoodLevel implements IFoodLevel {
    private float value = FoodType.Vanilla.maxCount;

    private final system.Action0 onChange;
    public VanillaFoodLevel(system.Action0 onChange) {
        this.onChange = onChange;
    }

    @Override public boolean addLevel(FoodType type, float level) {
        if (type != FoodType.Vanilla) return false;
        float oldValue = value;
        value = IFoodLevel.limit(type, value + level);
        if (value != oldValue) onChange.invoke();
        return true;
    }
    @Override public float totalLevel() { return value; }
    @Override public boolean needsFood() { return value < FoodType.Vanilla.maxCount; }
    @Override public void moveTo(float food) {
        float oldValue = value;
        value = IFoodLevel.limit(FoodType.Vanilla, food);
        if (value != oldValue) onChange.invoke();
    }

    @Override public boolean moveDelta(float deltaFood) {
        float oldValue = value;
        value = IFoodLevel.limit(FoodType.Vanilla, value + deltaFood);
        if (value == oldValue) return false;
        onChange.invoke();
        return true;
    }

    @Override public String type() { return "vanilla"; }
    @Override public void load(JsonObjectOptional json) { json.getAsFloat("value").ifPresent(v -> value = v); }
    @Override public system.json.builder.object save() { return system.json.object().add("value", value); }
    @Override public boolean isVanilla() { return true; }
}
