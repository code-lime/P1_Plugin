package org.lime.gp.player.module.needs.food;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.lime.core;
import org.lime.gp.item.data.Checker;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;
import org.lime.gp.item.settings.list.FoodSetting;
import org.lime.gp.player.module.needs.INeedEffect;
import org.lime.system.Regex;
import org.lime.system.json;
import org.lime.system.range.IRange;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.*;
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
    public static final Map<IRange, List<INeedEffect<?>>> Needs = new HashMap<>();

    public static FoodType parse(String name) {
        for (FoodType type : FoodType.values()) {
            if (type.name().equalsIgnoreCase(name))
                return type;
        }
        throw new IllegalArgumentException("No enum constant " + FoodType.class.getCanonicalName() + "." + name);
    }

    public static CoreElement create() {
        return CoreElement.create(FoodType.class)
                .<JsonObject>addConfig("food", v -> v
                        .withDefault(json.object()
                                .add("vanilla", IsVanilla)
                                .addObject("step", _v -> _v
                                        .add("food", FoodStep)
                                        .add("saturation", SaturationStep)
                                )
                                .addObject("types", _v -> _v
                                        .add(List.of(FoodType.values()),
                                                type -> type.name().toLowerCase(),
                                                type -> json.object()
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

                            Map<Material, Map<FoodType, FoodSetting.Info>> materialFood = new HashMap<>();
                            if (json.has("default")) json.getAsJsonObject("default")
                                    .entrySet()
                                    .forEach(kv -> {
                                        Map<FoodType, FoodSetting.Info> info = kv.getValue()
                                                .getAsJsonObject()
                                                .entrySet()
                                                .stream()
                                                .collect(Collectors.toMap(_v -> FoodType.parse(_v.getKey()), _v -> FoodSetting.Info.of(_v.getValue().getAsJsonObject())));
                                        Toast1<Boolean> isEmpty = Toast.of(true);
                                        Arrays.stream(Material.values())
                                                .filter(Regex.<Material>filterRegex(Enum::name, kv.getKey())::invoke)
                                                .forEach(material -> {
                                                    isEmpty.val0 = false;
                                                    materialFood.put(material, info);
                                                });
                                        if (isEmpty.val0) lime.logOP("Materials in '"+kv.getKey()+"' is EMPTY! Maybe error...");
                                    });

                            Map<IRange, List<INeedEffect<?>>> needs = new HashMap<>();
                            if (json.has("needs")) json.get("needs").getAsJsonObject().entrySet().forEach(kv -> {
                                List<INeedEffect<?>> values = new ArrayList<>();
                                kv.getValue().getAsJsonArray().forEach(item -> values.add(INeedEffect.parse(item.getAsJsonObject())));
                                needs.put(IRange.parse(kv.getKey()), values);
                            });

                            MaterialFood.clear();
                            MaterialFood.putAll(materialFood);

                            Needs.clear();
                            Needs.putAll(needs);
                        })
                );
    }
}
















