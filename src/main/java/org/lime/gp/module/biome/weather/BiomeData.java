package org.lime.gp.module.biome.weather;

import net.kyori.adventure.text.format.TextColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.biome.BiomeFog;
import org.lime.gp.extension.JsonNBT;
import org.lime.gp.module.biome.time.SeasonKey;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;

import java.util.Optional;

public record BiomeData(
        Optional<Integer> fog,
        Optional<Integer> water,

        Optional<Integer> waterFog,
        Optional<Integer> sky,

        Optional<Integer> foliage,
        Optional<Integer> grass,

        Optional<BiomeFog.GrassColor> grassModifier,

        Optional<Boolean> snow
) {
    private BiomeData() {
        this(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static BiomeFog.GrassColor getGrassColor(String name) {
        for (BiomeFog.GrassColor color : BiomeFog.GrassColor.values())
            if (color.getName().equals(name))
                return color;
        throw new IllegalArgumentException("No grass modifier type " + name);
    }

    public static BiomeData parseElement(NBTTagCompound element) {
        if (!element.contains("effects")) return new BiomeData();
        JsonObjectOptional json = JsonObjectOptional.of(JsonNBT.toJson(element.get("effects")).getAsJsonObject());
        return new BiomeData(
                json.getAsInt("fog_color"),
                json.getAsInt("water_color"),

                json.getAsInt("water_fog_color"),
                json.getAsInt("sky_color"),

                json.getAsInt("foliage_color"),
                json.getAsInt("grass_color"),

                json.getAsString("grass_color_modifier").map(BiomeData::getGrassColor),

                Optional.empty()
        );
    }

    public static BiomeData parseJson(JsonObjectOptional json) {
        return new BiomeData(
                json.getAsString("fog_color").map(TextColor::fromHexString).map(TextColor::value),
                json.getAsString("water_color").map(TextColor::fromHexString).map(TextColor::value),

                json.getAsString("water_fog_color").map(TextColor::fromHexString).map(TextColor::value),
                json.getAsString("sky_color").map(TextColor::fromHexString).map(TextColor::value),

                json.getAsString("foliage_color").map(TextColor::fromHexString).map(TextColor::value),
                json.getAsString("grass_color").map(TextColor::fromHexString).map(TextColor::value),

                json.getAsString("grass_color_modifier").map(BiomeData::getGrassColor),

                json.getAsBoolean("snow")
        );
    }

    public void modify(NBTTagCompound element) {
        NBTTagCompound effects;
        if (element.contains("effects")) effects = element.getCompound("effects");
        else element.put("effects", effects = new NBTTagCompound());

        fog.ifPresent(value -> effects.putInt("fog_color", value));
        water.ifPresent(value -> effects.putInt("water_color", value));
        waterFog.ifPresent(value -> effects.putInt("water_fog_color", value));
        sky.ifPresent(value -> effects.putInt("sky_color", value));
        foliage.ifPresent(value -> effects.putInt("foliage_color", value));
        grass.ifPresent(value -> effects.putInt("grass_color", value));
        grassModifier.ifPresent(value -> effects.putString("grass_color_modifier", value.getName()));
        snow.ifPresent(value -> SeasonKey.setTemperature(element, value ? -0.5f : 0.8f));
    }

    public json.builder.object saveJson() {
        json.builder.object raw = json.object();

        fog.map(TextColor::color).map(TextColor::asHexString).ifPresent(value -> raw.add("fog_color", value));
        water.map(TextColor::color).map(TextColor::asHexString).ifPresent(value -> raw.add("water_color", value));
        waterFog.map(TextColor::color).map(TextColor::asHexString).ifPresent(value -> raw.add("water_fog_color", value));
        sky.map(TextColor::color).map(TextColor::asHexString).ifPresent(value -> raw.add("sky_color", value));
        foliage.map(TextColor::color).map(TextColor::asHexString).ifPresent(value -> raw.add("foliage_color", value));
        grass.map(TextColor::color).map(TextColor::asHexString).ifPresent(value -> raw.add("grass_color", value));
        grassModifier.ifPresent(value -> raw.add("grass_color_modifier", value.getName()));
        snow.ifPresent(value -> raw.add("snow", value));

        return raw;
    }

    public static BiomeData empty() {
        return new BiomeData();
    }
}
