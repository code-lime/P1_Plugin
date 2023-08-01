package net.minecraft.world.level.biome;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.item.ItemMaxDamageEvent;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BiomeTemperatureEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private float temperature;
    private final BiomeBase biome;
    private final BlockPosition position;

    private BiomeTemperatureEvent(float temperature, BiomeBase biome, BlockPosition position, boolean isAsync) {
        super(isAsync);
        this.temperature = temperature;
        this.biome = biome;
        this.position = position;
    }

    public static float execute(float temperature, BiomeBase biome, BlockPosition position) {
        Server server = Bukkit.getServer();
        if (server == null) return temperature;
        BiomeTemperatureEvent event = new BiomeTemperatureEvent(temperature, biome, position, !server.isPrimaryThread());
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
