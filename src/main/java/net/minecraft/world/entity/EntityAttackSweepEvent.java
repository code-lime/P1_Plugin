package net.minecraft.world.entity;

import net.minecraft.world.entity.player.EntityHuman;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EntityAttackSweepEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final EntityHuman human;
    private boolean sweep;

    protected EntityAttackSweepEvent(EntityHuman human, boolean sweep) {
        this.human = human;
        this.sweep = sweep;
    }
    public static boolean execute(EntityHuman human, boolean sweep) {
        EntityAttackSweepEvent event = new EntityAttackSweepEvent(human, sweep);
        Bukkit.getPluginManager().callEvent(event);
        return event.getSweep();
    }

    public EntityHuman getHuman() { return human; }
    public boolean getSweep() { return sweep; }
    public void setSweep(boolean sweep) { this.sweep = sweep; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
