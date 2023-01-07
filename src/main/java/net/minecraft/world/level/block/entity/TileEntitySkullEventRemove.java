package net.minecraft.world.level.block.entity;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TileEntitySkullEventRemove extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final TileEntityLimeSkull skull;

    protected TileEntitySkullEventRemove(TileEntityLimeSkull skull) {
        this.skull = skull;
    }
    public static void execute(TileEntityLimeSkull skull) {
        TileEntitySkullEventRemove event = new TileEntitySkullEventRemove(skull);
        Bukkit.getPluginManager().callEvent(event);
    }

    public TileEntityLimeSkull getSkull() { return skull; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
