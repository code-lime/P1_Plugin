package net.minecraft.world.level.biome;

import net.minecraft.core.BlockPosition;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BiomeTemperatureEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private float temperature;
    private final BiomeBase biome;
    private final BlockPosition position;

    public BiomeTemperatureEvent(float temperature, BiomeBase biome, BlockPosition position) {
        this.temperature = temperature;
        this.biome = biome;
        this.position = position;
    }

    public static float execute(float temperature, BiomeBase biome, BlockPosition position) {
        BiomeTemperatureEvent event = new BiomeTemperatureEvent(temperature, biome, position);
        Bukkit.getPluginManager().callEvent(event);
        return event.temperature;
    }

    public float getTemperature() { return temperature; }
    public BiomeBase getBiome() { return biome; }
    public BlockPosition getPosition() { return position; }

    public void setTemperature(float temperature) { this.temperature = temperature; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
