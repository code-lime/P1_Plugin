package net.minecraft.world.entity.player;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerAttackStrengthResetEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final EntityHuman human;

    protected PlayerAttackStrengthResetEvent(EntityHuman human) { this.human = human; }
    public static void execute(EntityHuman human) { Bukkit.getPluginManager().callEvent(new PlayerAttackStrengthResetEvent(human)); }

    public EntityHuman getHuman() { return human; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
