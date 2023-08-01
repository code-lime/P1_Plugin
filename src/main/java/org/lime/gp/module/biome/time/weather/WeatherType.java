package org.lime.gp.module.biome.time.weather;

import net.minecraft.world.level.World;

public enum WeatherType {
    CLEAR,
    RAIN,
    THUNDER;

    public static WeatherType getBy(World world) {
        return world.isThundering() ? THUNDER : world.isRaining() ? RAIN : CLEAR;
    }
}
