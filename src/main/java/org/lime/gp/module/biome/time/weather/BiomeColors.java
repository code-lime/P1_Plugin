package org.lime.gp.module.biome.time.weather;

import net.kyori.adventure.text.format.TextColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.biome.BiomeFog;
import org.lime.gp.extension.JsonNBT;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.Optional;

public record BiomeColors(
        Optional<Integer> fog,
        Optional<Integer> water,

        Optional<Integer> waterFog,
        Optional<Integer> sky,

        Optional<Integer> foliage,
        Optional<Integer> grass,

        Optional<BiomeFog.GrassColor> grassModifier
) {
    private BiomeColors() {
        this(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static BiomeFog.GrassColor getGrassColor(String name) {
        for (BiomeFog.GrassColor color : BiomeFog.GrassColor.values())
            if (color.getName().equals(name))
                return color;
        throw new IllegalArgumentException("No grass modifier type " + name);
    }

    public static BiomeColors parseElement(NBTTagCompound element) {
        if (!element.contains("effects")) return new BiomeColors();
        JsonObjectOptional json = JsonObjectOptional.of(JsonNBT.toJson(element.get("effects")).getAsJsonObject());
        return new BiomeColors(
                json.getAsInt("fog_color"),
                json.getAsInt("water_color"),

                json.getAsInt("water_fog_color"),
                json.getAsInt("sky_color"),

                json.getAsInt("foliage_color"),
                json.getAsInt("grass_color"),

                json.getAsString("grass_color_modifier").map(BiomeColors::getGrassColor)
        );
    }

    public static BiomeColors parseJson(JsonObjectOptional json) {
        return new BiomeColors(
                json.getAsString("fog_color").map(TextColor::fromHexString).map(TextColor::value),
                json.getAsString("water_color").map(TextColor::fromHexString).map(TextColor::value),

                json.getAsString("water_fog_color").map(TextColor::fromHexString).map(TextColor::value),
                json.getAsString("sky_color").map(TextColor::fromHexString).map(TextColor::value),

                json.getAsString("foliage_color").map(TextColor::fromHexString).map(TextColor::value),
                json.getAsString("grass_color").map(TextColor::fromHexString).map(TextColor::value),

                json.getAsString("grass_color_modifier").map(BiomeColors::getGrassColor)
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
    }

    public system.json.builder.object saveJson() {
        system.json.builder.object raw = system.json.object();

        fog.map(TextColor::color).map(TextColor::asHexString).ifPresent(value -> raw.add("fog_color", value));
        water.map(TextColor::color).map(TextColor::asHexString).ifPresent(value -> raw.add("water_color", value));
        waterFog.map(TextColor::color).map(TextColor::asHexString).ifPresent(value -> raw.add("water_fog_color", value));
        sky.map(TextColor::color).map(TextColor::asHexString).ifPresent(value -> raw.add("sky_color", value));
        foliage.map(TextColor::color).map(TextColor::asHexString).ifPresent(value -> raw.add("foliage_color", value));
        grass.map(TextColor::color).map(TextColor::asHexString).ifPresent(value -> raw.add("grass_color", value));
        grassModifier.ifPresent(value -> raw.add("grass_color_modifier", value.getName()));

        return raw;
    }

    public static BiomeColors empty() {
        return new BiomeColors();
    }
}
