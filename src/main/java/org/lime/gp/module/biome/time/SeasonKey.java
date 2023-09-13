package org.lime.gp.module.biome.time;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.biome.BiomeFog;

import java.util.Optional;

public enum SeasonKey {
    Sunny(1),
    Frosty(2, -0.5f),
    Rainy(3);

    public final Float temperature;

    public final int index;
    public final char prefix;
    public final String prefixString;
    public final String key;

    SeasonKey(int index) {
        this(index, null);
    }
    SeasonKey(int index, Float temperature) {
        this.index = index;
        this.prefix = Character.toUpperCase(name().charAt(0));
        this.prefixString = String.valueOf(this.prefix);
        this.key = name().toLowerCase();
        this.temperature = temperature;
    }

    public void modify(NBTTagCompound element) {
        if (temperature == null || !element.contains("has_precipitation") || element.getByte("has_precipitation") == 0) return;
        setTemperature(element, temperature);
    }

    public static void setTemperature(NBTTagCompound element, float temperature) {
        element.putFloat("temperature", temperature);
    }

    public static SeasonKey byIndex(int index) {
        for (SeasonKey season : SeasonKey.values())
            if (season.index == index)
                return season;
        throw new IllegalArgumentException("Season index '" + index + "' not supported!");
    }
    public static SeasonKey byPrefix(String prefix) {
        for (SeasonKey season : SeasonKey.values())
            if (season.prefixString.equals(prefix))
                return season;
        throw new IllegalArgumentException("Season prefix '" + prefix + "' not supported!");
    }
    public static SeasonKey byKey(String key) {
        for (SeasonKey season : SeasonKey.values())
            if (season.key.equals(key))
                return season;
        throw new IllegalArgumentException("Season key '" + key + "' not supported!");
    }
}
