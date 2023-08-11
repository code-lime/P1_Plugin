package org.lime.gp.player.module.needs.food;

import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TypedFoodLevel implements IFoodLevel {
    private final Map<FoodType, Float> values = Stream.of(FoodType.Dessert, FoodType.Fruit, FoodType.Meat, FoodType.Cereals)
            .collect(Collectors.toMap(v -> v, v -> (float)v.maxCount));

    private final system.Action0 onChange;
    public TypedFoodLevel(system.Action0 onChange) {
        this.onChange = onChange;
    }

    public float getValue(FoodType type) {
        return values.getOrDefault(type, 0f);
    }

    @Override public boolean addLevel(FoodType type, float level) {
        system.Toast2<Boolean, Boolean> isChanged = system.toast(false, false);
        values.computeIfPresent(type, (_type, value) -> {
            float _value = IFoodLevel.limit(_type, value + level);
            if (_value != value) isChanged.val0 = true;
            isChanged.val1 = true;
            return _value;
        });
        if (isChanged.val0) onChange.invoke();
        return isChanged.val1;
    }
    @Override public float totalLevel() {
        float total = 0;
        for (float value : values.values()) total += value;
        return total;
    }
    @Override public boolean needsFood() {
        for (Map.Entry<FoodType, Float> kv : values.entrySet())
            if (kv.getValue() < kv.getKey().maxCount)
                return true;
        return false;
    }
    @Override public void moveTo(float food) {
        float total = 0;
        float totalWeight = 0;
        for (Map.Entry<FoodType, Float> kv : values.entrySet()) {
            total += kv.getValue();

            FoodType type = kv.getKey();
            totalWeight += type.weight();
        }
        if (totalWeight == 0) return;
        float delta = (food - total) / totalWeight;

        boolean isChanged = false;
        for (Map.Entry<FoodType, Float> kv : values.entrySet()) {
            float value = kv.getValue();
            float _value = IFoodLevel.limit(kv.getKey(), value + delta * kv.getKey().weight());
            if (_value != value) isChanged = true;
            kv.setValue(_value);
        }
        if (isChanged) onChange.invoke();
    }
    @Override public boolean moveDelta(float deltaFood) {
        float totalWeight = 0;
        for (FoodType type : values.keySet()) totalWeight += type.weight();
        if (totalWeight == 0) return false;
        float delta = deltaFood / totalWeight;

        boolean isChanged = false;
        for (Map.Entry<FoodType, Float> kv : values.entrySet()) {
            float value = kv.getValue();
            float _value = IFoodLevel.limit(kv.getKey(), value + delta * kv.getKey().weight());
            if (_value != value) isChanged = true;
            kv.setValue(_value);
        }
        if (isChanged) onChange.invoke();
        return isChanged;
    }

    @Override public String type() { return "typed"; }
    @Override public void load(JsonObjectOptional json) {
        values.entrySet().forEach(kv -> kv.setValue(json.getAsFloat(kv.getKey().name().toLowerCase()).orElseGet(kv::getValue)));
    }
    @Override public system.json.builder.object save() {
        return system.json.object().add(values, kv -> kv.name().toLowerCase(), kv -> kv);
    }

    @Override public boolean isVanilla() { return false; }
}
