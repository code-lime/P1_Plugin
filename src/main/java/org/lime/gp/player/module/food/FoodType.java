package org.lime.gp.player.module.food;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.lime.core;
import org.lime.gp.item.settings.list.FoodSetting;
import org.lime.system;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum FoodType {
    Vanilla(20, 1),
    Dessert(2, 1, 0xE7A0, 9, 0, 0),
    Fruit(6, 3, 0xE7A4, 7, 1, 1),
    Meat(6, 3, 0xE7A8, 9, -1, 0),
    Cereals(6, 3, 0xE7AC, 8, 0, 0);

    public final int maxCount;
    public final int imageIndex;
    public final int imageWidth;
    public final int imageOffset;
    public final int imageEndOffset;
    private float weight;

    public float weight() { return weight; }
    public int imageByValue(float delta) {
        return imageIndex + (3 - Math.round(delta * 3));
    }

    FoodType(int maxCount, float weight) {
        this(maxCount, weight, 0, 0, 0, 0);
    }
    FoodType(int maxCount, float weight, int imageIndex, int imageWidth, int imageOffset, int imageEndOffset) {
        this.maxCount = maxCount;
        this.weight = weight;
        this.imageIndex = imageIndex;
        this.imageWidth = imageWidth;
        this.imageOffset = imageOffset;
        this.imageEndOffset = imageEndOffset;
    }

    public static boolean IsVanilla = true;

    public static float SaturationStep = 0.5f;
    public static float FoodStep = 0.5f;

    public static final Map<Material, Map<FoodType, FoodSetting.Info>> MaterialFood = new HashMap<>();

    public static FoodType parse(String name) {
        for (FoodType type : FoodType.values()) {
            if (type.name().equalsIgnoreCase(name))
                return type;
        }
        throw new IllegalArgumentException("No enum constant " + FoodType.class.getCanonicalName() + "." + name);
    }

    public static core.element create() {
        return core.element.create(FoodType.class)
                .<JsonObject>addConfig("food", v -> v
                        .withDefault(system.json.object()
                                .add("vanilla", IsVanilla)
                                .addObject("step", _v -> _v
                                        .add("food", FoodStep)
                                        .add("saturation", SaturationStep)
                                )
                                .addObject("types", _v -> _v
                                        .add(List.of(FoodType.values()),
                                                type -> type.name().toLowerCase(),
                                                type -> system.json.object()
                                                        .add("weight", type.weight)
                                        )
                                )
                                .addObject("default", _v -> _v)
                                .build())
                        .withInvoke(json -> {
                            IsVanilla = json.get("vanilla").getAsBoolean();

                            JsonObject step = json.get("step").getAsJsonObject();
                            FoodStep = step.get("food").getAsFloat();
                            SaturationStep = step.get("saturation").getAsFloat();

                            JsonObject types = json.get("types").getAsJsonObject();
                            for (FoodType type : FoodType.values()) {
                                JsonObject data = types.getAsJsonObject(type.name().toLowerCase());
                                type.weight = data.get("weight").getAsFloat();
                            }

                            if (json.has("default")) {
                                Map<Material, Map<FoodType, FoodSetting.Info>> materialFood = new HashMap<>();
                                json.getAsJsonObject("default")
                                        .entrySet()
                                        .forEach(kv -> materialFood.put(Material.valueOf(kv.getKey()), kv.getValue()
                                                .getAsJsonObject()
                                                .entrySet()
                                                .stream()
                                                .collect(Collectors.toMap(_v -> FoodType.parse(_v.getKey()), _v -> FoodSetting.Info.of(_v.getValue().getAsJsonObject())))
                                        ));
                                MaterialFood.clear();
                                MaterialFood.putAll(materialFood);
                            }
                        })
                );
    }
}
